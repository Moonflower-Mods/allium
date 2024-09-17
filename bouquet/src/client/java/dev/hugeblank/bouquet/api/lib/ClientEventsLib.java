package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.bouquet.api.event.ClientEventHandlers;
import dev.hugeblank.bouquet.api.event.SimpleEventType;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

@LuaWrapped(name = "events")
public class ClientEventsLib {

    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.PlayerTick> PLAYER_TICK; // player gets ticked on the client
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_HEAD; // The end of the client render cycle (renders below everything in the gui)
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_TAIL; // The end of the client render cycle (renders above everything in the gui)

    static {
        PLAYER_TICK = new SimpleEventType<>(Identifier.of("allium:client_player_tick"));
        GUI_RENDER_HEAD = new SimpleEventType<>(Identifier.of("allium:client_render_head"));
        GUI_RENDER_TAIL = new SimpleEventType<>(Identifier.of("allium:client_render_tail"));
    }
}
