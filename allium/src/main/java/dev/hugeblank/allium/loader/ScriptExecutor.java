package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.TypeCoercions;
import dev.hugeblank.allium.loader.type.WrappedLuaLibrary;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ScriptExecutor {
    private static final Set<LibraryInitializer> LIBRARIES = new HashSet<>();
    protected final Script script;
    protected final LuaTable globals;
    protected final LuaState state;

    public ScriptExecutor(Script script) {
        this.script = script;
        this.state = new LuaState();

        // Base globals
        this.globals = CoreLibraries.debugGlobals(state);
        Bit32Lib.add(state, globals);

        // TODO: Can Script implement WrappedLuaLibrary?
        LibFunction.setGlobalLibrary(state, globals, "script", TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class)));

        // External libraries
        LIBRARIES.forEach((library) -> library.init(script).add(state, globals));

        // Package library, kinda quirky.

        // TODO: these calls, or mark them as unsupported
//        globals.rawset( "module", Constants.NIL );
//        globals.rawset( "collectgarbage", Constants.NIL );
//        globals.rawset( "dofile", Constants.NIL );
//        globals.rawset( "loadfile", Constants.NIL );


        globals.rawset( "print", new PrintMethod(script) );

        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );
    }

    static {
        registerLibrary(PackageLib::new);

    }

    public LuaState getState() {
        return state;
    }

    public LuaTable getGlobals() {
        return globals;
    }

    public Varargs initialize(@Nullable InputStream sMain, @Nullable InputStream dMain) throws Throwable {
        Entrypoint entrypoints = script.getManifest().entrypoints();
        LuaFunction staticFunction;
        LuaFunction dynamicFunction;
        switch (entrypoints.getType()) {
            case STATIC -> {
                staticFunction = this.load(sMain, script.getId());
                return LuaThread.runMain(state, staticFunction);
            }
            case DYNAMIC -> {
                dynamicFunction = this.load(dMain, script.getId());
                return LuaThread.runMain(state, dynamicFunction);
            }
            case BOTH -> {
                staticFunction = this.load(sMain, script.getId() + ":static");
                dynamicFunction = this.load(dMain, script.getId() + ":dynamic");
                Varargs out = LuaThread.runMain(state, staticFunction);
                LuaThread.runMain(state, dynamicFunction);
                return out;
            }
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public Varargs reload(InputStream dynamic) throws LuaError, CompileException, IOException {
        Entrypoint entrypoint = script.getManifest().entrypoints();
        if (entrypoint.hasType(Entrypoint.Type.DYNAMIC)) {
            LuaFunction dynamicFunction = this.load(dynamic, script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        return null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException {
        return LoadState.load(
                state,
                stream,
                name,
                this.globals
        );
    }

    public static void registerLibrary(LibraryInitializer initializer) {
        LIBRARIES.add(initializer);
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
    
    public interface LibraryInitializer {
        WrappedLuaLibrary init(Script script);
    }

}
