package me.hugeblank.allium.lua.api;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.LuaLibrary;

public class PlayerLib implements LuaLibrary {

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        env.rawset("player", lib);
        state.loadedPackages.rawset("player", lib);
        return lib;
    }
}
