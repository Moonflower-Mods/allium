package dev.hugeblank.allium.lua.api;

import dev.hugeblank.allium.lua.type.annotation.CoerceToNative;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.List;

@LuaWrapped(name = "fabric")
public class FabricLib implements WrappedLuaLibrary {
    @LuaWrapped
    public @CoerceToNative List<ModContainer> getAllMods() {
        return FabricLoader.getInstance().getAllMods().stream().toList();
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
