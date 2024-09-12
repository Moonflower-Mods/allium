package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.util.JavaHelpers;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;
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
    private final Require require;
    private final LuaTable packageApi;

    public PackageLib(Script script, LuaState state) {
        this.loaded = new LuaTable();
        this.require = new Require(this);

        // When writing a loader in Java, anywhere where a module value can't be determined `null` should be returned.
        loaders.rawset(1, new PreloadLoader()); // Loader to check if module has a loader provided by preload table
        loaders.rawset(2, new PathLoader(script)); // Loader to check the path internal to the script
        loaders.rawset(3, new ExternScriptLoader()); // Loader to check the path assuming the first value in the path is a script ID
        loaders.rawset(4, new JavaLoader());

        packageApi = new LuaTable();
        packageApi.rawset("preload", preload);
        packageApi.rawset("loaded", loaded);
        packageApi.rawset("loaders", loaders);
        packageApi.rawset("path", LuaString.valueOf(pathString));
    }

    public Require getRequire() {
        return require;
    }

    public LuaTable getPackage() {
        return packageApi;
    }

    private Varargs loadFromPaths(LuaState state, Script script, String modStr) throws UnwindThrowable, LuaError {
        List<Path> paths = getPathsFromModule(script, modStr);
        for (Path path : paths) {
            if (!Files.exists(path)) return Constants.NIL;
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
                            "Attempted to require an entrypoint of script '" + script.getId() +
                                    "'. Use require(\"" + script.getId() + "\") if you'd like to get" +
                                    " the value loaded by the entrypoint script."
                    ); // Slap on the wrist. Allium has already handled loading of the script.
                    return Constants.NIL;
                }
            } catch (IOException ignored) {}
            // Sometimes the loader returns the module *as well* as the path they were loaded from.
            return ValueFactory.varargsOf(script.loadLibrary(state, path), ValueFactory.valueOf(path.toString()));
        }
        return Constants.NIL;
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

        private Require(PackageLib pkg) {
            this.pkg = pkg;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            LuaString mod = args.arg(1).checkLuaString();
            if (!pkg.loaded.rawget(mod).isNil()) return pkg.loaded.rawget(mod);
            for (int i = 1; i <= pkg.loaders.length(); i++) {
                LuaValue loader = pkg.loaders.rawget(i);
                if (loader.isFunction()) {
                    LuaFunction f = loader.checkFunction();
                    Varargs contents = f.call(state, mod);
                    if (contents != Constants.NIL) {
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
            return Constants.NIL;
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
            return Constants.NIL;
        }
    }

    private static class JavaLoader extends OneArgFunction {

        @Override
        public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
            return StaticBinder.bindClass(JavaHelpers.asClass(arg));
        }
    }
}