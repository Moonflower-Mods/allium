package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.*;
import me.hugeblank.allium.util.FileHelper;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class  PluginExecutor {
    protected final Plugin plugin;
    protected final LuaTable globals;
    protected final LuaState state;

    public PluginExecutor(Plugin plugin) {
        this.plugin = plugin;
        // Derived from CobaltMachine.java
        // https://github.com/cc-tweaked/cc-restitched/blob/79366bf2f5389b45c0db1ad0d37fbddc6d1151b3/src/main/java/dan200/computercraft/core/lua/CobaltLuaMachine.java
        state = LuaState.builder().build();

        globals = new LuaTable();

        // Base globals
        globals.load( state, new BaseLib() );
        globals.load( state, new TableLib() );
        globals.load( state, new StringLib() );
        globals.load( state, new MathLib() );
        globals.load( state, new CoroutineLib() );
        globals.load( state, new Bit32Lib() );
        globals.load( state, new Utf8Lib() );
        globals.load( state, new DebugLib() );

        // Custom globals
        globals.load( state, AlliumLib.create(plugin) );
        globals.load( state, new GameLib() );
        globals.load( state, JavaLib.create() );
        globals.load( state, TextLib.create() );
        globals.load( state, CommandLib.create(plugin) );
        globals.load( state, ScriptLib.create(plugin) );

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );
        globals.rawset( "print", new PrintMethod(plugin) );


        globals.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", ValueFactory.valueOf("Allium 0.1.0" ) );
    }

    public LuaState getState() {
        return state;
    }

    public void initialize(File main) throws LuaError, InterruptedException, IOException, CompileException {
        LuaFunction loadValue = LoadState.load(
                state,
                new FileInputStream(main),
                plugin.getId(),
                this.globals
        );
        state.setupThread(new LuaTable());
        // TODO: Does this allow for multiple plugins to run?
        LuaThread.runMain(state, loadValue);
    }

    private static final class PrintMethod extends VarArgFunction {
        private final Plugin plugin;

        PrintMethod(Plugin plugin) {
            this.plugin = plugin;
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
            plugin.getLogger().info(out.toString());
            return Constants.NIL;
        }
    }
}
