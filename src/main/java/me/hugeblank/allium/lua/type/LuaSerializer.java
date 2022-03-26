package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaValue;

public interface LuaSerializer<T> {
    LuaValue toLua(T value);
}
