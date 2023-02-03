package dev.hugeblank.allium.lua.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;

// For all events that use classes that the server doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

public class ClientEventHandlers {
    public interface PlayerTick {
        @Environment(EnvType.CLIENT)
        void onPlayerTick(ClientPlayerEntity player);

        @Environment(EnvType.SERVER)
        void onPlayerTick(); // TODO: Throw an exception here and in ServerEventHandlers
    }

    public interface GuiRender {
        @Environment(EnvType.CLIENT)
        void onGuiRender(MinecraftClient client, MatrixStack matrices, float tickDelta, int scaledWidth, int scaledHeight, TextRenderer textRenderer);

        @Environment(EnvType.SERVER)
        void onGuiRender();
    }
}
