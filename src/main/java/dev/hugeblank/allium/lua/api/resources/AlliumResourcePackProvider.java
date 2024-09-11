package dev.hugeblank.allium.lua.api.resources;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;

import java.util.function.Consumer;

public class AlliumResourcePackProvider implements ResourcePackProvider {
    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder, ResourcePackProfile.Factory factory) {
        AlliumResourcePack pack = AlliumResourcePack.create("Allium Generated");
        profileAdder.accept(ResourcePackProfile.of(
                "allium_generated",
                true,
                () -> pack, factory,
                ResourcePackProfile.InsertionPosition.TOP,
                ResourcePackSource.PACK_SOURCE_BUILTIN
        ));
    }
}