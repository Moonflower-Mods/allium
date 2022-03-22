package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.LuaLibrary;

public interface WrappedLuaLibrary extends LuaLibrary {
    String getLibraryName();

    @Override
    default LuaValue add(LuaState state, LuaTable environment) {
        LuaValue lib = UserdataFactory.of(EClass.fromJava(getClass())).createBound(this);

        environment.rawset(getLibraryName(), lib);
        state.loadedPackages.rawset(getLibraryName(), lib);

        return lib;
    }
}
