package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.*;

import java.io.IOException;
import java.io.InputStream;

public class ScriptExecutor {
    protected final Script script;
    protected final LuaTable globals;
    protected final LuaState state;

    public ScriptExecutor(Script script) {
        this.script = script;

        // Derived from CobaltMachine.java
        // https://github.com/cc-tweaked/cc-restitched/blob/79366bf2f5389b45c0db1ad0d37fbddc6d1151b3/src/main/java/dan200/computercraft/core/lua/CobaltLuaMachine.java
        state = LuaState.builder().interruptHandler(() -> InterruptAction.SUSPEND).build();

        globals = new LuaTable();

        // Base globals
        new BaseLib().add(globals);
        TableLib.add(state, globals);
        StringLib.add(state, globals);
        new MathLib().add(state, globals);
        CoroutineLib.add(state, globals);
        Bit32Lib.add(state, globals);
        new Utf8Lib().add(state, globals);
        DebugLib.add(state, globals);

        // Custom globals
//        globals.load( state, new AlliumLib() );
//        globals.load( state, new GameLib() );
//        globals.load( state, new JavaLib() );
//        globals.load( state, new TextLib() );
//        globals.load( state, new NbtLib() );
//        globals.load( state, new CommandLib(script) );
//        globals.load( state, new CommandsLib(script) );
//        globals.load( state, new DefaultEventsLib() );
//        globals.load( state, new FabricLib() );
//        globals.load( state, new ConfigLib(script) );
//        globals.load( state, new FsLib(script) );
//        globals.load( state, new HttpLib() );
//        globals.load( state, new JsonLib() );
//        globals.load( state, new RecipeLib() );
        globals.rawset( "script", TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class)) );

        // Package library, kinda quirky.
        PackageLib pkg = new PackageLib(script, state);
        globals.rawset( "package" , pkg.getPackage() );
        globals.rawset( "require", pkg.getRequire() );

        // TODO: these calls, or mark them as unsupported
//        globals.rawset( "module", Constants.NIL );
//        globals.rawset( "collectgarbage", Constants.NIL );
//        globals.rawset( "dofile", Constants.NIL );
//        globals.rawset( "loadfile", Constants.NIL );


        globals.rawset( "print", new PrintMethod(script) );

        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + Allium.VERSION) );
    }

    public LuaState getState() {
        return state;
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

    public Varargs reload(InputStream dynamic) throws LuaError, InterruptedException, CompileException, IOException {
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
