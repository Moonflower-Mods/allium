package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.lua.api.*;
import dev.hugeblank.allium.lua.api.PackageLib;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.lua.api.commands.CommandLib;
import dev.hugeblank.allium.lua.api.commands.CommandsLib;
import dev.hugeblank.allium.lua.api.http.HttpLib;
import dev.hugeblank.allium.lua.api.recipe.RecipeLib;
import dev.hugeblank.allium.lua.type.TypeCoercions;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import javax.annotation.Nullable;
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
        state = LuaState.builder().build();

        globals = new LuaTable();

        // Base globals
        globals.load( state, new BaseLib() );
        globals.load( state, new TableLib() );
        globals.load( state, new StringLib() );
        globals.load( state, new MathLib() );
        // globals.load( state, new CoroutineLib() ); // Not providing this for reasons(tm)
        globals.load( state, new Bit32Lib() );
        globals.load( state, new Utf8Lib() );
        globals.load( state, new DebugLib() );

        // Custom globals
        globals.load( state, new AlliumLib() );
        globals.load( state, new GameLib() );
        globals.load( state, new JavaLib() );
        globals.load( state, new TextLib() );
        globals.load( state, new NbtLib() );
        globals.load( state, new CommandLib(script) );
        globals.load( state, new CommandsLib(script) );
        globals.rawset( "script", TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class)) );
        globals.load( state, new DefaultEventsLib() );
        globals.load( state, new FabricLib() );
        globals.load( state, new ConfigLib(script) );
        globals.load( state, new FsLib(script) );
        globals.load( state, new HttpLib() );
        globals.load( state, new JsonLib() );
        globals.load( state, new RecipeLib() );

        // Package library, kinda quirky.
        PackageLib pkg = new PackageLib(script, state);
        globals.rawset( "package" , pkg.create() );
        globals.rawset( "require", new PackageLib.Require(pkg) );
        globals.rawset( "module", Constants.NIL ); // TODO: module call

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );


        globals.rawset( "print", new PrintMethod(script) );

        globals.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + Allium.VERSION) );
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize(@Nullable InputStream sMain, @Nullable InputStream dMain) throws Throwable {
        Entrypoint entrypoints = script.getManifest().entrypoints();
        LuaFunction staticFunction;
        LuaFunction dynamicFunction;
        state.setupThread(new LuaTable());
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
