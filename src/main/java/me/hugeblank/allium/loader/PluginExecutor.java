package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.*;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class  PluginExecutor {
    protected final LuaTable globals;
    protected final LuaState state;

    public PluginExecutor(Plugin plugin) {
        // Derived from CobaltMachine.java
        // https://github.com/cc-tweaked/cc-restitched/blob/79366bf2f5389b45c0db1ad0d37fbddc6d1151b3/src/main/java/dan200/computercraft/core/lua/CobaltLuaMachine.java
        state = LuaState.builder().build();

        globals = new LuaTable();

        // Base globals
        globals.load( state, new BaseLib());
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
        globals.load( state, JavaLib.create());
        globals.load( state, TextLib.create());

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );
        globals.rawset( "print", new PrintMethod(plugin) );


        globals.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", ValueFactory.valueOf("Allium 0.0.0" ) );
    }

    public LuaState getState() {
        return state;
    }

    public boolean initialize(Plugin plugin, File main) {
        try {
            LuaFunction loadValue = LoadState.load(state, new FileInputStream(main), "main.lua", this.globals);
            state.setupThread(new LuaTable());
            // TODO: Does this allow for multiple plugins to run?
            LuaTable info = LuaThread.runMain(state, loadValue).checkValue(1).checkTable();
            try {
                String id = info.rawget("id").checkLuaString().toString();
                String version = info.rawget("version").checkLuaString().toString();
                String name = info.rawget("name").optString(id);
                return plugin.register(id, version, name, this);
            } catch (LuaError e) {
                plugin.cleanup();
                Allium.LOGGER.error("Plugin initialize error", e);
            }
        } catch (Exception e) {
            Allium.LOGGER.error("Error loading main.lua for plugin", e);
        }
        return false;
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
