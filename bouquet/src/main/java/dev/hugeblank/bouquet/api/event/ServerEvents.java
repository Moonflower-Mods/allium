package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

public class ServerEvents implements Events {

    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.ChatMessage> CHAT_MESSAGE;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.PlayerJoin> PLAYER_JOIN;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.PlayerQuit> PLAYER_QUIT;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.PlayerBlockCollision> PLAYER_BLOCK_COLLISION;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.ServerTick> SERVER_TICK;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.CommandRegistration> COMMAND_REGISTER;
    @LuaWrapped public static final SimpleEventType<ServerEventHandlers.ServerStart> SERVER_START;

    static {
        // server gets a chat message
        CHAT_MESSAGE = new SimpleEventType<>(Identifier.of("allium:server/chat_message"));
        // player joins the game
        PLAYER_JOIN = new SimpleEventType<>(Identifier.of("allium:server/player_join"));
        // player leaves the game
        PLAYER_QUIT = new SimpleEventType<>(Identifier.of("allium:server/player_quit"));
        // player collides with a block
        PLAYER_BLOCK_COLLISION = new SimpleEventType<>(Identifier.of("allium:server/player_block_collision"));
        // server gets ticked
        SERVER_TICK = new SimpleEventType<>(Identifier.of("allium:server/tick"));
        // the result of a registered command
        COMMAND_REGISTER = new SimpleEventType<>(Identifier.of("allium:server/command_register"));
        // server finishes loading
        SERVER_START = new SimpleEventType<>(Identifier.of("allium:server/start"));
    }
}
