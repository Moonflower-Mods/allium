package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.UserdataFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.squiddev.cobalt.LuaTable;

@LuaWrapped(name = "fabric")
public class FabricLib implements WrappedLuaLibrary {
    @LuaWrapped
    public LuaTable getAllMods() {
        return UserdataFactory.listToTable(FabricLoader.getInstance().getAllMods().stream().toList(), EClass.fromJava(ModContainer.class));
    }

    @LuaWrapped
    public ModContainer getMod(String id) {
        return FabricLoader.getInstance().getModContainer(id).orElse(null);
    }

    @LuaWrapped
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
