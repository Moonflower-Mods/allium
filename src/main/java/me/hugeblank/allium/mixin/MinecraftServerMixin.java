package me.hugeblank.allium.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import me.hugeblank.allium.Allium;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.UserCache;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
        Allium.SERVER = (MinecraftServer) (Object) this;

        IndexedIterable<RegistryEntry<Block>> blocks = Allium.SERVER.getRegistryManager().get(Registry.BLOCK_KEY).getIndexedEntries();
        IndexedIterable<RegistryEntry<Item>> items = Allium.SERVER.getRegistryManager().get(Registry.ITEM_KEY).getIndexedEntries();
        blocks.forEach((entry) -> {
            if (entry.getKey().isPresent()) {
                Allium.BLOCKS.put(entry.getKey().get().getValue(), entry.value());
            }
        });
        items.forEach((entry) -> {
            if (entry.getKey().isPresent()) {
                Allium.ITEMS.put(entry.getKey().get().getValue(), entry.value());
            }
        });
    }

    @Inject(at = @At("TAIL"), method = "exit")
    private void exit(CallbackInfo ci) {
        Allium.SERVER = null;
    }
}
