package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.LuaWrapped;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.squiddev.cobalt.LuaError;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@LuaWrapped
public class GameLib implements WrappedLuaLibrary {
    public ServerPlayerEntity getPlayer(String username) throws LuaError {
        ServerPlayerEntity player = Allium.SERVER.getPlayerManager().getPlayer(username);
        if (player == null) throw new LuaError("Player '" + username + "' does not exist");
        return player;
    }

    @LuaWrapped
    public Block getBlock(String id) {
        return Objects.requireNonNull(Registry.BLOCK.get(new Identifier(id)));
    }

    @LuaWrapped
    public Item getItem(String id) {
        return Objects.requireNonNull(Registry.ITEM.get(new Identifier(id)));
    }

    @LuaWrapped
    public ServerWorld getWorld(String id) {
        return Objects.requireNonNull(Allium.SERVER.getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(id))));
    }

    @LuaWrapped
    public Map<Identifier, Block> listBlocks() {
        return Registry.BLOCK.stream().collect(Collectors.toMap(Registry.BLOCK::getId, x -> x));
    }

    @LuaWrapped
    public Map<Identifier, Item> listItems() {
        return Registry.ITEM.stream().collect(Collectors.toMap(Registry.ITEM::getId, x -> x));
    }

    @LuaWrapped
    public List<ServerPlayerEntity> listPlayers() {
        return Allium.SERVER.getPlayerManager().getPlayerList();
    }

    @LuaWrapped
    public Map<Identifier, ServerWorld> listWorlds() {
        return StreamSupport.stream(Allium.SERVER.getWorlds().spliterator(), false).collect(Collectors.toMap(x -> x.getRegistryKey().getValue(), x -> x));
    }

    @Override
    public String getLibraryName() {
        return "game";
    }
}
