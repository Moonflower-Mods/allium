package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.loader.ScriptCandidate;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.OneArgFunction;

import java.io.File;
import java.util.Set;

public class PackageLib {
    private final LuaTable loaders = new LuaTable();
    private final LuaTable loaded = new LuaTable();
    private final LuaTable preload = new LuaTable();
    private final String pathString = "./?.lua;./?/init.lua";

    public LuaTable create(Script script) {
        // When writing a loader in Java, anywhere where a module value can't be determined `null` should be returned.
        loaders.rawset(1, new PreloadLoader()); // Loader to check if module has a loader provided by preload table
        loaders.rawset(2, new PathLoader(script)); // Loader to check the path internal to the script
        loaders.rawset(3, new ExternScriptLoader(script)); // Loader to check the path assuming the first value in the path is a script ID

        LuaTable out = new LuaTable();
        out.rawset("preload", preload);
        out.rawset("loaded", loaded);
        out.rawset("loaders", loaders);
        out.rawset("path", LuaString.valueOf(pathString));
        return out;
    }

    private LuaValue loadFromPaths(LuaState state, Script script, String modStr) throws UnwindThrowable, LuaError {
        String[] paths = pathString.split(";");
        File entrypoint = new File(script.getRootPath().toFile(), script.getManifest().entrypoint());
        for (String pathStr : paths) {
            File module = new File(pathStr.replace("?", modStr.replace(".", "/")).replace("./", script.getRootPath().toString() + '/'));
            if (entrypoint.compareTo(module) == 0) {
                Allium.LOGGER.warn(
                        "Attempted to require entrypoint of script '" + script.getManifest().id() +
                        "'. Use require(\"" + script.getManifest().id() + "\") if you'd like to get" +
                        " the value loaded by the entrypoint script."
                ); // Slap on the wrist. Allium has already handled loading of the script.
                return null;
            }
            return script.loadLibrary(state, module);
        }
        return null;
    }

    public static class Require extends OneArgFunction {
        private final PackageLib pkg;

        public Require(PackageLib pkg) {
            this.pkg = pkg;
        }

        @Override
        public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
            LuaString mod = arg.checkLuaString();
            if (!pkg.loaded.rawget(mod).isNil()) return pkg.loaded.rawget(mod);
            for (int i = 1; i <= pkg.loaders.length(); i++) {
                LuaValue loader = pkg.loaders.rawget(i);
                if (loader.isFunction()) {
                    LuaValue contents = loader.checkFunction().call(state, mod);
                    if (contents != null) {
                        pkg.loaded.rawset(mod, contents);
                        return contents;
                    }
                }
            }
            throw new LuaError("Could not find module " + mod.toString());
        }
    }

    private class ExternScriptLoader extends OneArgFunction {
        private final Script script;

        public ExternScriptLoader(Script script) {
            this.script = script;
        }


        @Override
        public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
            String[] path = arg.checkString().split("\\.");
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
            // return getModuleCandidate(state, Allium.CANDIDATES, path[0], toPath(path));
            return null;
        }

        private LuaValue getModuleCandidate(LuaState state, Set<ScriptCandidate<?>> entrySet, String id, String modStr) throws UnwindThrowable, LuaError {
            Allium.LOGGER.info(id + " | " + modStr);
            for (ScriptCandidate<?> entry : entrySet) {
                if (entry.manifest().id().equals(id)) {
                    Script candidate = (Script) entry.load();
                    candidate.initialize();
                    if (candidate.isInitialized()) {
                        if (modStr.isBlank()) return candidate.getModule();
                        return loadFromPaths(state, candidate, modStr);
                    } else {
                        throw new LuaError(
                                "Cyclic dependency found between '" +
                                script.getManifest().id() + "' and '" +
                                entry.manifest().id() + "'."
                        );
                    }
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

    private class PathLoader extends OneArgFunction {
        private final Script script;

        public PathLoader(Script script) {
            this.script = script;
        }

        @Override
        public LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable {
            String modStr = arg.checkString();
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
