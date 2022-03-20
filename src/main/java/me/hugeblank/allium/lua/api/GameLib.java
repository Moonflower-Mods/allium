package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.HideFromLua;
import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GameLib {
    @HideFromLua
    public static LuaLibrary create() {
        return (state, env) -> {
            LuaValue lib = JavaLib.importClass(EClass.fromJava(GameLib.class));

            env.rawset("game", lib);
            state.loadedPackages.rawset("game", lib);

            return lib;
        };
    }

    public static ServerPlayerEntity getPlayer(String username) throws LuaError {
        ServerPlayerEntity player = Allium.SERVER.getPlayerManager().getPlayer(username);
        if (player == null) throw new LuaError("Player '" + username + "' does not exist");
        return player;
    }

    public static Block getBlock(String id) {
        return Objects.requireNonNull(Registry.BLOCK.get(new Identifier(id)));
    }

    public static Item getItem(String id) {
        return Objects.requireNonNull(Registry.ITEM.get(new Identifier(id)));
    }

    public static ServerWorld getWorld(String id) {
        return Objects.requireNonNull(Allium.SERVER.getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(id))));
    }

    public static Map<Identifier, Block> listBlocks() {
        return Registry.BLOCK.stream().collect(Collectors.toMap(Registry.BLOCK::getId, x -> x));
    }

    public static Map<Identifier, Item> listItems() {
        return Registry.ITEM.stream().collect(Collectors.toMap(Registry.ITEM::getId, x -> x));
    }

    public static List<ServerPlayerEntity> listPlayers() {
        return Allium.SERVER.getPlayerManager().getPlayerList();
    }

    public static Map<Identifier, ServerWorld> listWorlds() {
        return StreamSupport.stream(Allium.SERVER.getWorlds().spliterator(), false).collect(Collectors.toMap(x -> x.getRegistryKey().getValue(), x -> x));
    }
}
