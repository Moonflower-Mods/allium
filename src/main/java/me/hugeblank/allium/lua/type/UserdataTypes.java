package me.hugeblank.allium.lua.type;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class UserdataTypes {
    public static final Map<Class<?>, UserdataFactory<?>> TYPES = new HashMap<>();
    public static final UserdataFactory<ServerPlayerEntity> PLAYER;
    public static final UserdataFactory<BlockPos> BLOCK_POS;
    public static final UserdataFactory<Block> BLOCK;
    public static final UserdataFactory<Item> ITEM;
    public static final UserdataFactory<World> WORLD;
    public static final UserdataFactory<BlockState> BLOCK_STATE;

    static {
        PLAYER = register(ServerPlayerEntity.class);
        BLOCK_POS = register(BlockPos.class);
        BLOCK = register(Block.class);
        ITEM = register(Item.class);
        WORLD = register(World.class);
        BLOCK_STATE = register(BlockState.class);
    }

    public static <T> UserdataFactory<T> register(Class<T> clazz) {
        UserdataFactory<T> factory = new UserdataFactory<>(clazz);
        TYPES.put(clazz, factory);
        return factory;
    }
}
