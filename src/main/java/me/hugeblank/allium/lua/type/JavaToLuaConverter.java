package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaValue;

public interface JavaToLuaConverter<T> {
    LuaValue toLua(T value);
}
