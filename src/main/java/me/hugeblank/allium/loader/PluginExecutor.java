package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.PlayerLib;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.*;

import java.io.File;
import java.io.FileInputStream;

public class PluginExecutor {
    protected final LuaTable globals;
    protected final LuaState state;
    protected boolean initialized;

    public PluginExecutor() {
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

        // Custo globals
        globals.load( state, new PlayerLib() );

        // Remove globals we don't want to expose
        globals.rawset( "collectgarbage", Constants.NIL );
        globals.rawset( "dofile", Constants.NIL );
        globals.rawset( "loadfile", Constants.NIL );
        globals.rawset( "print", Constants.NIL );


        globals.rawset( "_VERSION", ValueFactory.valueOf( "Lua 5.1" ) );
        globals.rawset( "_HOST", ValueFactory.valueOf("Allium 0.0.0" ) );
    }

    public Plugin initialize(File main) {
        try {
            LuaFunction loadValue = LoadState.load(state, new FileInputStream(main), "main.lua", this.globals);
            state.setupThread(new LuaTable());
            LuaTable info = LuaThread.runMain(state, loadValue).checkValue(1).checkTable();
            try {
                String id = info.rawget("id").checkLuaString().toString();
                String version = info.rawget("version").checkLuaString().toString();
                String name = info.rawget("name").optString(id);
                return new Plugin(id, version, name, this);
            } catch (LuaError e) {
                Allium.LOGGER.error("Plugin initialize error", e);
            }
        } catch (Exception e) {
            Allium.LOGGER.error("Error loading main.lua for plugin", e);
        }
        return null;
    }
}
