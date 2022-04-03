package me.hugeblank.allium.lua.type.property;

import me.basiqueevangelist.enhancedreflection.api.EField;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public record FieldData<I>(EField field) implements PropertyData<I> {
    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        try {
            return UserdataFactory.toLuaValue(field.get(instance), field.fieldTypeUse().upperBound());
        } catch (IllegalAccessException e) {
            throw new LuaError(e);
        }
    }
}
