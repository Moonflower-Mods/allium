package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.Entrypoint;
import me.hugeblank.allium.loader.Script;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PackageLib {
    private final LuaTable loaders = new LuaTable();
    private final LuaTable preload = new LuaTable();
    private static final String pathString = "./?.lua;./?/init.lua";
    private final LuaTable loaded;
    private final Script loadedFor;

    public PackageLib(Script script, LuaState state) {
        this.loadedFor = script;
        this.loaded = state.loadedPackages;
    }

    public LuaTable create() {
        // When writing a loader in Java, anywhere where a module value can't be determined `null` should be returned.
        loaders.rawset(1, new PreloadLoader()); // Loader to check if module has a loader provided by preload table
        loaders.rawset(2, new PathLoader(loadedFor)); // Loader to check the path internal to the script
        loaders.rawset(3, new ExternScriptLoader()); // Loader to check the path assuming the first value in the path is a script ID

        LuaTable out = new LuaTable();
        out.rawset("preload", preload);
        out.rawset("loaded", loaded);
        out.rawset("loaders", loaders);
        out.rawset("path", LuaString.valueOf(pathString));
        return out;
    }

    private Varargs loadFromPaths(LuaState state, Script script, String modStr) throws UnwindThrowable, LuaError {
        List<Path> paths = getPathsFromModule(script, modStr);
        for (Path path : paths) {
            if (!Files.exists(path)) return null;
            try {
                // Do not allow entrypoints to get loaded from the path.
                Entrypoint entrypoint = script.getManifest().entrypoints();
                boolean loadingEntrypoint = (
                        entrypoint.containsDynamic() &&
                        Files.isSameFile(path, script.getPath().resolve(entrypoint.getDynamic()))
                ) || (
                        entrypoint.containsStatic() &&
                        Files.isSameFile(path, script.getPath().resolve(entrypoint.getStatic()))
                );

                if (loadingEntrypoint) {
                    Allium.LOGGER.warn(
                        "Attempted to require an entrypoint of script '" + script.getManifest().id() +
                        "'. Use require(\"" + script.getManifest().id() + "\") if you'd like to get" +
                        " the value loaded by the entrypoint script."
                    ); // Slap on the wrist. Allium has already handled loading of the script.
                    return null;
                }
            } catch (IOException ignored) {}
            // Sometimes the loader returns the module *as well* as the path they were loaded from.
            return ValueFactory.varargsOf(script.loadLibrary(state, path), ValueFactory.valueOf(path.toString()));
        }
        return null;
    }

    private static List<Path> getPathsFromModule(Script script, String modStr) {
        List<Path> pathList = new ArrayList<>();
        String[] paths = pathString.split(";");
        for (String pathStr : paths) {
            Path path = script.getPath().resolve(
                    pathStr.replace("?", modStr.replace(".", "/"))
            );
            pathList.add(path);
        }
        return pathList;
    }

    public static class Require extends VarArgFunction {
        private final PackageLib pkg;

        public Require(PackageLib pkg) {
            this.pkg = pkg;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            LuaString mod = args.arg(1).checkLuaString();
            if (!pkg.loaded.rawget(mod).isNil()) return pkg.loaded.rawget(mod);
            for (int i = 1; i <= pkg.loaders.length(); i++) {
                LuaValue loader = pkg.loaders.rawget(i);
                if (loader.isFunction()) {
                    Varargs contents = loader.checkFunction().call(state, mod);
                    if (contents != null) {
                        pkg.loaded.rawset(mod, contents.arg(1));
                        return contents;
                    }
                }
            }
            throw new LuaError("Could not find module " + mod.toString());
        }
    }

    private class ExternScriptLoader extends VarArgFunction {

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            String[] path = args.arg(1).checkString().split("\\.");
            Script candidate = Script.getFromID(path[0]);
            if (candidate != null) {
                if (!candidate.isInitialized()) {
                    candidate.initialize();
                }
                if (path.length == 1) {
                    return candidate.getModule();
                } else {
                    return loadFromPaths(state, candidate, toPath(path));
                }
            }
            return null;
        }

        private static String toPath(String[] arr) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < arr.length; i++) {
                builder.append(arr[i]);
                if (i < arr.length-1) {
                    builder.append("/");
                }
            }
            return builder.toString();
        }
    }

    private class PathLoader extends VarArgFunction {
        private final Script script;

        public PathLoader(Script script) {
            this.script = script;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            String modStr = args.arg(1).checkString();
            Entrypoint entrypoint = script.getManifest().entrypoints();
            for (Path path : getPathsFromModule(script, modStr)) {
                try {
                    if ( // If the script is requiring its own static entrypoint from the dynamic one, give the value.
                            entrypoint.hasType(Entrypoint.Type.STATIC) &&
                                    Files.isSameFile( path, script.getPath().resolve(entrypoint.getStatic()))
                    ) return ValueFactory.varargsOf(script.getModule(), ValueFactory.valueOf(path.toString()));
                } catch (IOException ignored) {}
            }
            return loadFromPaths(state, script, modStr);
        }
    }

    private class PreloadLoader extends OneArgFunction {
        @Override
        public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
            if (preload.rawget(arg).isFunction()){
                return preload.rawget(arg).checkFunction().call(state, arg);
            }
            return null;
        }
    }
}
