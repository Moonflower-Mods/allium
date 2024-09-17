package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.type.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@LuaWrapped(name = "game")
public class GameLib implements WrappedLuaLibrary {

    @LuaWrapped
    public Block getBlock(String id) {
        return Objects.requireNonNull(Registries.BLOCK.get(Identifier.of(id)));
    }

    @LuaWrapped
    public Item getItem(String id) {
        return Objects.requireNonNull(Registries.ITEM.get(Identifier.of(id)));
    }

    @LuaWrapped
    public ServerWorld getWorld(MinecraftServer server, String id) {
        return Objects.requireNonNull(server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(id))));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, Block> listBlocks() {
        return Registries.BLOCK.stream().collect(Collectors.toMap(x -> Registries.BLOCK.getId(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, Item> listItems() {
        return Registries.ITEM.stream().collect(Collectors.toMap(x -> Registries.ITEM.getId(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<Identifier, ServerWorld> listWorlds(MinecraftServer server) {
        return StreamSupport.stream(server.getWorlds().spliterator(), false)
                .collect(Collectors.toMap(x -> x.getRegistryKey().getValue(), x -> x));
    }
}
