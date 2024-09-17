package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.bouquet.api.event.CommonEventHandlers;
import dev.hugeblank.bouquet.api.event.SimpleEventType;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

@LuaWrapped(name = "events")
public class DefaultEventsLib implements WrappedLuaLibrary {
    // TODO: Consider a better naming scheme for events.
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.ChatMessage> CHAT_MESSAGE; // player sends a chat message
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerTick> PLAYER_TICK; // player gets ticked on the server
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerJoin> PLAYER_JOIN; // player joins the game
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerQuit> PLAYER_QUIT; // player leaves the game
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockCollision> PLAYER_BLOCK_COLLISION; // player collides with a block
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerDeath> PLAYER_DEATH; // player dies
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockInteract> BLOCK_INTERACT; // player interacts (right clicks) with a block
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.Tick> TICK; // server gets ticked
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.CommandRegistration> COMMAND_REGISTER; // the result of a registered command

    static {
        CHAT_MESSAGE = new SimpleEventType<>(Identifier.of("allium:chat_message"));
        PLAYER_TICK = new SimpleEventType<>(Identifier.of("allium:server_player_tick"));
        PLAYER_JOIN = new SimpleEventType<>(Identifier.of("allium:player_join"));
        PLAYER_QUIT = new SimpleEventType<>(Identifier.of("allium:player_quit"));
        PLAYER_BLOCK_COLLISION = new SimpleEventType<>(Identifier.of("allium:player_block_collision"));
        PLAYER_DEATH = new SimpleEventType<>(Identifier.of("allium:player_death"));
        BLOCK_INTERACT = new SimpleEventType<>(Identifier.of("allium:block_interact"));
        TICK = new SimpleEventType<>(Identifier.of("allium:server_tick"));
        COMMAND_REGISTER = new SimpleEventType<>(Identifier.of("allium:command_register"));
    }

}
