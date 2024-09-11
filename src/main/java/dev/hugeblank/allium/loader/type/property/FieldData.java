package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EField;
import dev.hugeblank.allium.loader.type.TypeCoercions;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public record FieldData<I>(EField field) implements PropertyData<I> {
    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(field.get(instance), field.fieldTypeUse().upperBound());
        } catch (IllegalAccessException e) {
            throw new LuaError(e);
        }
    }

    @Override
    public void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        if (field.isFinal()) {
            PropertyData.super.set(name, state, instance, value);
            return;
        }

        try {
            field.set(instance, TypeCoercions.toJava(state, value, field.fieldType().upperBound()));
        } catch (InvalidArgumentException | IllegalAccessException e) {
            throw new LuaError(e);
        }
    }
}
