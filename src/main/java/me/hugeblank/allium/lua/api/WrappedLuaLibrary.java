package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.LuaLibrary;

public interface WrappedLuaLibrary extends LuaLibrary {
    @Override
    default LuaValue add(LuaState state, LuaTable environment) {
        LuaValue lib = UserdataFactory.of(EClass.fromJava(getClass())).createBound(this);

        LuaWrapped wrapped = getClass().getAnnotation(LuaWrapped.class);

        if (wrapped == null || wrapped.name().length == 0)
            throw new IllegalStateException("WrappedLuaLibrary must have a @LuaWrapped annotation with a name!");

        for (String name : wrapped.name()) {
            environment.rawset(name, lib);
            state.loadedPackages.rawset(name, lib);
        }

        return lib;
    }
}
