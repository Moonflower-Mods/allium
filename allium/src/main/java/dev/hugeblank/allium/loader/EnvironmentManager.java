package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.LibraryInitializer;
import dev.hugeblank.allium.loader.mixin.MixinLib;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.util.HashSet;
import java.util.Set;

public class EnvironmentManager {
    protected final LuaState state;

    private static final Set<LibraryInitializer> INITIALIZERS = new HashSet<>();
    private static final Set<WrappedLuaLibrary> LIBRARIES = new HashSet<>();
    private static final Set<LibraryInitializer> PRELAUNCH_INITIALIZERS = new HashSet<>();
    private static final Set<WrappedLuaLibrary> PRELAUNCH_LIBRARIES = new HashSet<>();

    EnvironmentManager() {
        this.state = new LuaState();
    }

    protected LuaTable createPreLaunchEnvironment(Script script) {
        LuaTable globals = CoreLibraries.debugGlobals(state);
        Bit32Lib.add(state, globals);

        LibFunction.setGlobalLibrary(state, globals, "script",
                TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class))
        );
        globals.rawset( "print", new PrintMethod(script) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );

        PRELAUNCH_INITIALIZERS.forEach(initializer -> initializer.init(script).add(state, globals));
        PRELAUNCH_LIBRARIES.forEach(library -> library.add(state, globals));

        return globals;
    }

    protected void bindExtendedLibraries(Script script, LuaTable globals) {
        INITIALIZERS.forEach(initializer -> initializer.init(script).add(state, globals));
        LIBRARIES.forEach(library -> library.add(state, globals));
    }

    /**
     * register a library to be used by static and dynamic entrypoints.
     * @param initializer Library constructor that accepts a script.
     */
    public static void registerLibrary(LibraryInitializer initializer) {
        INITIALIZERS.add(initializer);
    }

    /**
     * register a library to be used by static and dynamic entrypoints.
     * @param library LuaWrapped annotated class object.
     */
    public static void registerLibrary(WrappedLuaLibrary library) {
        LIBRARIES.add(library);
    }

    /**
     * register a library to be used by preLaunch entrypoint.
     * @param initializer Library constructor that accepts a script.
     */
    public static void registerPreLaunchLibrary(LibraryInitializer initializer) {
        PRELAUNCH_INITIALIZERS.add(initializer);
    }

    /**
     * register a library to be used by preLaunch entrypoints.
     * @param library LuaWrapped annotated class object.
     */
    public static void registerPreLaunchLibrary(WrappedLuaLibrary library) {
        PRELAUNCH_LIBRARIES.add(library);
    }

    static {
        registerPreLaunchLibrary(PackageLib::new);
        registerPreLaunchLibrary(MixinLib::new);
    }

    private static final class PrintMethod extends VarArgFunction {
        private final Script script;

        PrintMethod(Script script) {
            this.script = script;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) {
            StringBuilder out = new StringBuilder();
            for (int i = 1; i <= args.count(); i++) {
                out.append(args.arg(i).toString());
                if (i != args.count()) {
                    out.append(" ");
                }
            }
            script.getLogger().info(out.toString());
            return Constants.NIL;
        }
    }
}
