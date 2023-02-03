package dev.hugeblank.allium.lua.type.property;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public class EmptyData implements PropertyData<Object> {
    public static EmptyData INSTANCE = new EmptyData();

    @Override
    public LuaValue get(String name, LuaState state, Object instance, boolean noThisArg) throws LuaError {
        return Constants.NIL;
    }

    @Override
    public void set(String name, LuaState state, Object instance, LuaValue value) throws LuaError {
        throw new LuaError("'" + name + "' isn't a valid property name");
    }
}
