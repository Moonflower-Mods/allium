package dev.hugeblank.allium.loader.type.property;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public interface PropertyData<I> {
    LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError;

    default void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        throw new LuaError("property '" + name + "' doesn't support set");
    }
}
