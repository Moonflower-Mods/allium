package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.lang.reflect.InvocationTargetException;

public record PropertyMethodData<I>(EMethod getter,
                                    @Nullable EMethod setter) implements PropertyData<I> {
    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        var params = getter.parameters();
        try {
            var jargs = ArgumentUtils.toJavaArguments(state, Constants.NONE, 1, params);

            EClassUse<?> ret = getter.returnTypeUse().upperBound();
            Object out = getter.invoke(instance, jargs);
            return TypeCoercions.toLuaValue(out, ret);
        } catch (InvalidArgumentException e) {
            throw new IllegalStateException("Getter for '" + name + "' needs arguments");
        } catch (ReflectiveOperationException roe) {
            throw new LuaError(roe);
        }
    }

    @Override
    public void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        if (setter == null) {
            PropertyData.super.set(name, state, instance, value);
            return;
        }

        var params = setter.parameters();
        try {
            var jargs = ArgumentUtils.toJavaArguments(state, value, 1, params);

            setter.invoke(instance, jargs);
        } catch (InvalidArgumentException e) {
            throw new IllegalStateException("Setter for '" + name + "' needs more than one argument");
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof LuaError luaError)
                throw luaError;

            throw new LuaError(e.getTargetException());
        } catch (ReflectiveOperationException e) {
            throw new LuaError(e);
        }
    }
}
