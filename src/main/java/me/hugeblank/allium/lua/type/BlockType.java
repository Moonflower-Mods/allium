package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.LuaLibrary;

public class BlockType implements LuaLibrary {

    public BlockType() {

    }

    @Override
    public LuaValue add(LuaState state, LuaTable environment) {
        LuaTable tbl = new LuaTable();

        return tbl;
    }
}
