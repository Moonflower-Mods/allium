package me.hugeblank.allium.lua.event;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaBoolean;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.ValueFactory;

public class Events {
    // TODO: Make the Event class smarter
    public static final Event CHAT_MESSAGE; // player sends a chat message
    public static final Event PLAYER_TICK; // player gets ticked
    public static final Event PLAYER_JOIN; // player joins the game
    public static final Event PLAYER_QUIT; // player leaves the game
    public static final Event PLAYER_BLOCK_COLLISION; // player collides with a block
    public static final Event PLAYER_DEATH; // player dies
    public static final Event BLOCK_INTERACT; // player interacts (right clicks) with a block
    public static final Event SERVER_TICK; // server gets ticked
    public static final Event COMMAND_REGISTER; // the result of a registered command

    static {
        CHAT_MESSAGE = new Event("chat_message", (objects) -> {
            // Expects: [ServerPlayerEntity player, String message]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0]),
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
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_JOIN = new Event("player_join", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_QUIT = new Event("player_quit", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_BLOCK_COLLISION = new Event("player_block_collision", (objects) -> {
            // Expects: [ServerPlayerEntity player, BlockState state]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0]),
                        UserdataFactory.of(EClass.fromJava(BlockState.class)).create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_DEATH = new Event("player_death", (objects) -> {
            // Expects: [ServerPlayerEntity player, DamageSource source]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[0]),
                        UserdataFactory.of(EClass.fromJava(DamageSource.class)).create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        BLOCK_INTERACT = new Event("block_interact", (objects) -> {
            // Expects: [BlockState state, World world, BlockPos pos, ServerPlayerEntity player, Hand hand, BlockHitResult hitResult]
            try {
                return ValueFactory.varargsOf(
                        UserdataFactory.of(EClass.fromJava(BlockState.class)).create(objects[0]),
                        UserdataFactory.of(EClass.fromJava(World.class)).create(objects[1]),
                        UserdataFactory.of(EClass.fromJava(BlockPos.class)).create(objects[2]),
                        UserdataFactory.of(EClass.fromJava(ServerPlayerEntity.class)).create(objects[3]),
                        UserdataFactory.of(EClass.fromJava(Hand.class)).create(objects[4]),
                        UserdataFactory.of(EClass.fromJava(BlockHitResult.class)).create(objects[5])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        SERVER_TICK = new Event("server_tick", (objects) -> {
            // Expects: nothing
            return Constants.NIL;
        });
        COMMAND_REGISTER = new Event("command_register", (objects) -> {
            // Expects: [String script_id, String command_name, Boolean successful]
            try {
                LuaBoolean result = (Boolean) objects[2] ? Constants.TRUE : Constants.FALSE;
                return ValueFactory.varargsOf(
                        LuaString.valueOf((String) objects[0]),
                        LuaString.valueOf((String) objects[1]),
                        result
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
    }

    public static void init() {}
}
