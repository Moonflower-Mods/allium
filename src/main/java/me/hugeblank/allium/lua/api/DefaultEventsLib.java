package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.event.ClientEventHandlers;
import me.hugeblank.allium.lua.event.CommonEventHandlers;
import me.hugeblank.allium.lua.event.SimpleEventType;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

@LuaWrapped(name = "events")
public class DefaultEventsLib implements WrappedLuaLibrary {
    // TODO: Consider a better naming scheme for events.
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.ChatMessage> CHAT_MESSAGE; // player sends a chat message
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerTick> SERVER_PLAYER_TICK; // player gets ticked on the server
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.PlayerTick> CLIENT_PLAYER_TICK; // player gets ticked on the client
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerJoin> PLAYER_JOIN; // player joins the game
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerQuit> PLAYER_QUIT; // player leaves the game
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockCollision> PLAYER_BLOCK_COLLISION; // player collides with a block
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerDeath> PLAYER_DEATH; // player dies
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockInteract> BLOCK_INTERACT; // player interacts (right clicks) with a block
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.Tick> SERVER_TICK; // server gets ticked
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.CommandRegistration> COMMAND_REGISTER; // the result of a registered command
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> CLIENT_RENDER_HEAD; // The end of the client render cycle (renders below everything in the gui)
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> CLIENT_RENDER_TAIL; // The end of the client render cycle (renders above everything in the gui)

    static {
        CHAT_MESSAGE = new SimpleEventType<>(new Identifier("allium:chat_message"));
        SERVER_PLAYER_TICK = new SimpleEventType<>(new Identifier("allium:server_player_tick"));
        CLIENT_PLAYER_TICK = new SimpleEventType<>(new Identifier("allium:client_player_tick"));
        PLAYER_JOIN = new SimpleEventType<>(new Identifier("allium:player_join"));
        PLAYER_QUIT = new SimpleEventType<>(new Identifier("allium:player_quit"));
        PLAYER_BLOCK_COLLISION = new SimpleEventType<>(new Identifier("allium:player_block_collision"));
        PLAYER_DEATH = new SimpleEventType<>(new Identifier("allium:player_death"));
        BLOCK_INTERACT = new SimpleEventType<>(new Identifier("allium:block_interact"));
        SERVER_TICK = new SimpleEventType<>(new Identifier("allium:server_tick"));
        COMMAND_REGISTER = new SimpleEventType<>(new Identifier("allium:command_register"));
        CLIENT_RENDER_HEAD = new SimpleEventType<>(new Identifier("allium:client_render_head"));
        CLIENT_RENDER_TAIL = new SimpleEventType<>(new Identifier("allium:client_render_tail"));
    }

}
