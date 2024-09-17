package dev.hugeblank.allium.api;


import dev.hugeblank.allium.loader.type.UserdataFactory;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.LibFunction;

/**
 * Interface for lua libraries that take advantage of the LuaWrapped annotation
 *
 * @see dev.hugeblank.allium.loader.PackageLib
 */
public interface WrappedLuaLibrary {

    default LuaValue add(LuaState state, LuaTable globals) {
        LuaValue lib = UserdataFactory.of(EClass.fromJava(getClass())).createBound(this);

        LuaWrapped wrapped = getClass().getAnnotation(LuaWrapped.class);

        if (wrapped == null || wrapped.name().length == 0)
            throw new IllegalStateException("WrappedLuaLibrary must have a @LuaWrapped annotation with a name!");

        for (String name : wrapped.name()) {
            LibFunction.setGlobalLibrary(state, globals, name, lib);
        }

        return lib;
    }
}
