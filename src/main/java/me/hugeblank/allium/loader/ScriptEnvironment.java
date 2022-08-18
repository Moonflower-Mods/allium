package me.hugeblank.allium.loader;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.*;
import me.hugeblank.allium.lua.api.PackageLib;
import me.hugeblank.allium.lua.api.commands.CommandLib;
import me.hugeblank.allium.lua.api.commands.CommandsLib;
import me.hugeblank.allium.lua.api.http.HttpLib;
import me.hugeblank.allium.lua.api.recipe.RecipeLib;
import me.hugeblank.allium.lua.type.TypeCoercions;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.IOException;
import java.io.InputStream;

public class ScriptEnvironment {
    protected final Script script;
    protected final LuaTable globalEnv;
    protected final LuaState state;

    public ScriptEnvironment(Script script) {
        this.script = script;

        // Derived from CobaltMachine.java
        // https://github.com/cc-tweaked/cc-restitched/blob/79366bf2f5389b45c0db1ad0d37fbddc6d1151b3/src/main/java/dan200/computercraft/core/lua/CobaltLuaMachine.java
        state = LuaState.builder().build();

        globalEnv = new LuaTable();

        // Base globals
        globalEnv.load( state, new BaseLib() );
        globalEnv.load( state, new TableLib() );
        globalEnv.load( state, new StringLib() );
        globalEnv.load( state, new MathLib() );
        // globals.load( state, new CoroutineLib() ); // Not providing this for reasons(tm)
        globalEnv.load( state, new Bit32Lib() );
        globalEnv.load( state, new Utf8Lib() );
        globalEnv.load( state, new DebugLib() );

        // Custom globals
        globalEnv.load( state, new AlliumLib() );
        globalEnv.load( state, new JavaLib() );
        globalEnv.rawset( "script", TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class)) );
        globalEnv.load( state, new FabricLib() );
        globalEnv.load( state, new ConfigLib(script) );
        globalEnv.load( state, new FsLib(script) );
        globalEnv.load( state, new HttpLib() );
        globalEnv.load( state, new JsonLib() );

        // Package library, kinda quirky.
        me.hugeblank.allium.lua.api.PackageLib pkg = new me.hugeblank.allium.lua.api.PackageLib(script, state);
        globalEnv.rawset( "package" , pkg.create() );
        globalEnv.rawset( "require", new PackageLib.Require(pkg) );
        // TODO: This is wrong but I don't care to fix it right now
        globalEnv.rawset( "module", new ZeroArgFunction() {
            @Override
            public LuaValue call(LuaState state) {
                return script.getModule();
            }
        } );

        // Remove globals we don't want to expose
        globalEnv.rawset( "collectgarbage", Constants.NIL );
        globalEnv.rawset( "dofile", Constants.NIL );
        globalEnv.rawset( "loadfile", Constants.NIL );

        globalEnv.rawset( "print", new PrintMethod(script) );

        globalEnv.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globalEnv.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + Allium.VERSION) );
    }

    public void onInitialize() {
        // Custom globals that involve the game
        globalEnv.load( state, new GameLib() );
        globalEnv.load( state, new TextLib() );
        globalEnv.load( state, new NbtLib() );
        globalEnv.load( state, new CommandLib(script) );
        globalEnv.load( state, new CommandsLib(script) );
        globalEnv.load( state, new DefaultEventsLib() );
        globalEnv.load( state, new RecipeLib() );
    }

    public LuaState getState() {
        return state;
    }

    public Varargs reload(InputStream dynamic) throws LuaError, InterruptedException, CompileException, IOException {
        Entrypoint entrypoint = script.getManifest().entrypoints();
        if (entrypoint.hasDynamic()) {
            return run(dynamic, script.getId() + ":dynamic");
        }
        return null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException {
        return LoadState.load(
                state,
                stream,
                name,
                this.globalEnv
        );
    }

    public Varargs run(InputStream stream, String name) throws CompileException, IOException, LuaError, InterruptedException {
        return LuaThread.runMain(state, load(stream, name));
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
