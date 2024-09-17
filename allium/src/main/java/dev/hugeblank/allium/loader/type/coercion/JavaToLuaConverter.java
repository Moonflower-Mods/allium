package dev.hugeblank.allium.loader.type.coercion;

import org.squiddev.cobalt.LuaValue;

public interface JavaToLuaConverter<T> {
    LuaValue toLua(T value);
}
