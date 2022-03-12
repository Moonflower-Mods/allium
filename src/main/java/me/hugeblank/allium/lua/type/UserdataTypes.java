package me.hugeblank.allium.lua.type;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class UserdataTypes {
    // Unlocked types! These are available in one way or another on the Lua side.
    public static final Map<Class<?>, UserdataFactory<?>> TYPES = new HashMap<>();
    public static final UserdataFactory<ServerPlayerEntity> PLAYER;
    public static final UserdataFactory<BlockPos> BLOCKPOS;
    public static final UserdataFactory<Block> BLOCK;
    public static final UserdataFactory<Item> ITEM;
    public static final UserdataFactory<ItemStack> ITEMSTACK;
    public static final UserdataFactory<World> WORLD;
    public static final UserdataFactory<BlockState> BLOCKSTATE;
    public static final UserdataFactory<DamageSource> DAMAGESOURCE;
    public static final UserdataFactory<Identifier> IDENTIFIER;
    public static final UserdataFactory<Text> TEXT;

    static {
        PLAYER = register(ServerPlayerEntity.class);
        BLOCKPOS = register(BlockPos.class);
        BLOCK = register(Block.class);
        ITEM = register(Item.class);
        ITEMSTACK = register(ItemStack.class);
        WORLD = register(World.class);
        BLOCKSTATE = register(BlockState.class);
        DAMAGESOURCE = register(DamageSource.class);
        IDENTIFIER = register(Identifier.class);
        TEXT = register(Text.class);
    }

    public static <T> UserdataFactory<T> register(Class<T> clazz) {
        UserdataFactory<T> factory = new UserdataFactory<>(clazz);
        TYPES.put(clazz, factory);
        return factory;
    }
}
