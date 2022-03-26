package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public interface LuaDeserializer<T> {
    T fromLua(LuaState state, LuaValue value) throws LuaError, UserdataFactory.InvalidArgumentException;
}
