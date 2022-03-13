package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.ValueFactory;

public class Events {
    // TODO: Make the Event class smarter
    public static final Event CHAT_MESSAGE;
    public static final Event PLAYER_TICK;
    public static final Event PLAYER_JOIN;
    public static final Event PLAYER_QUIT;
    public static final Event PLAYER_BLOCK_COLLISION;
    public static final Event PLAYER_DEATH;
    public static final Event BLOCK_INTERACT;
    public static final Event SERVER_TICK;
    static {
        CHAT_MESSAGE = new Event("chat_message", (objects) -> {
            // Expects: [ServerPlayerEntity player, String message]
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0]),
                        LuaString.valueOf((String)objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_TICK = new Event("player_tick", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_JOIN = new Event("player_join", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_QUIT = new Event("player_quit", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_BLOCK_COLLISION = new Event("player_block_collision", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0]),
                        new UserdataFactory<>(BlockState.class).create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_DEATH = new Event("player_death", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[0]),
                        new UserdataFactory<>(DamageSource.class).create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        BLOCK_INTERACT = new Event("block_interact", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        new UserdataFactory<>(BlockState.class).create(objects[0]),
                        new UserdataFactory<>(World.class).create(objects[1]),
                        new UserdataFactory<>(BlockPos.class).create(objects[2]),
                        new UserdataFactory<>(ServerPlayerEntity.class).create(objects[3]),
                        new UserdataFactory<>(Hand.class).create(objects[4]),
                        new UserdataFactory<>(BlockHitResult.class).create(objects[5])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        SERVER_TICK = new Event("server_tick", (objects) -> {
            try {
                return ValueFactory.varargsOf(

                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
    }

    public static void init() {}
}
