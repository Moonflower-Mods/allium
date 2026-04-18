package dev.moongarden.bouquet.api.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// For all events that use classes that the server doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

public class ClientEventHandlers {

    public interface GuiRender {
        void onGuiRender(Gui hud, Minecraft client, GuiGraphicsExtractor context, DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded);
    }
}
