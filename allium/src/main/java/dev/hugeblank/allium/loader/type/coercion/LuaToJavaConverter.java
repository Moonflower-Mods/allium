package dev.hugeblank.allium.loader.type.coercion;

import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public interface LuaToJavaConverter<T> {
    T fromLua(LuaState state, LuaValue value) throws LuaError, InvalidArgumentException;
}
