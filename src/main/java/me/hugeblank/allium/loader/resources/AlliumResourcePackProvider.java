package me.hugeblank.allium.loader.resources;

import me.hugeblank.allium.Allium;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;

import java.util.function.Consumer;

public class AlliumResourcePackProvider implements ResourcePackProvider {
    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder, ResourcePackProfile.Factory factory) {
        if (Allium.PACK != null) {
            profileAdder.accept(ResourcePackProfile.of(
                    Allium.PACK.getName(),
                    true,
                    () -> Allium.PACK, factory,
                    ResourcePackProfile.InsertionPosition.TOP,
                    ResourcePackSource.PACK_SOURCE_BUILTIN
            ));
        } else {
            Allium.LOGGER.error("Could not load Allium Generated resource pack. " +
                    "Items added by scripts will not display correctly!");
        }
    }
}