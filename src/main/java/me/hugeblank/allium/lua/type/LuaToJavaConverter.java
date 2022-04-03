package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public interface LuaToJavaConverter<T> {
    T fromLua(LuaState state, LuaValue value) throws LuaError, InvalidArgumentException;
}
