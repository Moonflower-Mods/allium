package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.CommonTypes;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;

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
    public LuaTable listBlocks() {
        return UserdataFactory.mapToTable(
            Registry.BLOCK.stream().collect(Collectors.toMap(x -> Registry.BLOCK.getId(x).toString(), x -> x)),
            CommonTypes.STRING, EClass.fromJava(Block.class));
    }

    @LuaWrapped
    public LuaTable listItems() {
        return UserdataFactory.mapToTable(
            Registry.ITEM.stream().collect(Collectors.toMap(x -> Registry.ITEM.getId(x).toString(), x -> x)),
            CommonTypes.STRING, EClass.fromJava(Item.class));
    }

    @LuaWrapped
    public LuaTable listPlayers() {
        return UserdataFactory.listToTable(Allium.SERVER.getPlayerManager().getPlayerList(), EClass.fromJava(ServerPlayerEntity.class));
    }

    @LuaWrapped
    public LuaTable listWorlds() {
        return UserdataFactory.mapToTable(
            StreamSupport.stream(Allium.SERVER.getWorlds().spliterator(), false).collect(Collectors.toMap(x -> x.getRegistryKey().getValue().toString(), x -> x)),
            CommonTypes.STRING, EClass.fromJava(ServerWorld.class));
    }

    @Override
    public String getLibraryName() {
        return "game";
    }
}
