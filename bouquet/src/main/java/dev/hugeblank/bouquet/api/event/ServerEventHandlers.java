package dev.hugeblank.bouquet.api.event;

// For all events that use classes that the client doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerEventHandlers {

    public interface ChatMessage {
        void onChatMessage(ServerPlayerEntity player, String message);
    }

    public interface PlayerJoin {
        void onPlayerJoin(ServerPlayerEntity player);
    }

    public interface PlayerQuit {
        void onPlayerQuit(ServerPlayerEntity player);
    }

    public interface PlayerBlockCollision {
        void onPlayerBlockCollision(ServerPlayerEntity player, BlockState state);
    }

    public interface ServerTick {
        void onServerTick(MinecraftServer server);
    }

    public interface ServerStart {
        void onServerStart(MinecraftServer server);
    }

    public interface CommandRegistration {
        void onCommandRegistration(String scriptId, String commandName, boolean successful);
    }
}
