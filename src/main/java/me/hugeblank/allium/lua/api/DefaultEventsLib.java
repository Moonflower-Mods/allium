package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.event.SimpleEventType;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

@LuaWrapped(name = "events")
public class DefaultEventsLib implements WrappedLuaLibrary {
    @LuaWrapped public static final SimpleEventType<ChatMessageHandler> CHAT_MESSAGE; // player sends a chat message
    @LuaWrapped public static final SimpleEventType<PlayerTickHandler> PLAYER_TICK; // player gets ticked
    @LuaWrapped public static final SimpleEventType<PlayerJoinHandler> PLAYER_JOIN; // player joins the game
    @LuaWrapped public static final SimpleEventType<PlayerQuitHandler> PLAYER_QUIT; // player leaves the game
    @LuaWrapped public static final SimpleEventType<PlayerBlockCollisionHandler> PLAYER_BLOCK_COLLISION; // player collides with a block
    @LuaWrapped public static final SimpleEventType<PlayerDeathHandler> PLAYER_DEATH; // player dies
    @LuaWrapped public static final SimpleEventType<PlayerBlockInteractHandler> BLOCK_INTERACT; // player interacts (right clicks) with a block
    @LuaWrapped public static final SimpleEventType<ServerTickHandler> SERVER_TICK; // server gets ticked
    @LuaWrapped public static final SimpleEventType<CommandRegistrationHandler> COMMAND_REGISTER; // the result of a registered command

    static {
        CHAT_MESSAGE = new SimpleEventType<>(new Identifier("allium:chat_message"));
        PLAYER_TICK = new SimpleEventType<>(new Identifier("allium:player_tick"));
        PLAYER_JOIN = new SimpleEventType<>(new Identifier("allium:player_join"));
        PLAYER_QUIT = new SimpleEventType<>(new Identifier("allium:player_quit"));
        PLAYER_BLOCK_COLLISION = new SimpleEventType<>(new Identifier("allium:player_block_collision"));
        PLAYER_DEATH = new SimpleEventType<>(new Identifier("allium:player_death"));
        BLOCK_INTERACT = new SimpleEventType<>(new Identifier("allium:block_interact"));
        SERVER_TICK = new SimpleEventType<>(new Identifier("allium:server_tick"));
        COMMAND_REGISTER = new SimpleEventType<>(new Identifier("allium:command_register"));
    }

    public interface ChatMessageHandler {
        void onChatMessage(ServerPlayerEntity player, String message);
    }

    public interface PlayerTickHandler {
        void onPlayerTick(ServerPlayerEntity player);
    }

    public interface PlayerJoinHandler {
        void onPlayerJoin(ServerPlayerEntity player);
    }

    public interface PlayerQuitHandler {
        void onPlayerQuit(ServerPlayerEntity player);
    }

    public interface PlayerBlockCollisionHandler {
        void onPlayerBlockCollision(ServerPlayerEntity player, BlockState state);
    }

    public interface PlayerDeathHandler {
        void onPlayerDeath(ServerPlayerEntity player, DamageSource damageSource);
    }

    public interface PlayerBlockInteractHandler {
        void onPlayerBlockInteraction(BlockState state, ServerWorld world, BlockPos pos, ServerPlayerEntity player, Hand hand, BlockHitResult hitResult);
    }

    public interface ServerTickHandler {
        void onServerTick();
    }

    public interface CommandRegistrationHandler {
        void onCommandRegistration(String scriptId, String commandName, boolean successful);
    }
}
