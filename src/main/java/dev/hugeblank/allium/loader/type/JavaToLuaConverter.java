package dev.hugeblank.allium.loader.type;

import org.squiddev.cobalt.LuaValue;

public interface JavaToLuaConverter<T> {
    LuaValue toLua(T value);
}
