package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.util.Identifier;

@LuaWrapped
public class ClientEvents implements Events {
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_HEAD;
    @LuaWrapped public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_TAIL;

    static {
        // The end of the client render cycle (renders below everything in the gui)
        GUI_RENDER_HEAD = new SimpleEventType<>(Identifier.of("allium:client/render_head"));
        // The end of the client render cycle (renders above everything in the gui)
        GUI_RENDER_TAIL = new SimpleEventType<>(Identifier.of("allium:client/render_tail"));
    }
}
