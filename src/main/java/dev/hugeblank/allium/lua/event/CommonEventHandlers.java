package dev.hugeblank.allium.lua.event;

import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class CommonEventHandlers {
    public interface ChatMessage {
        void onChatMessage(ServerPlayerEntity player, String message);
    }

    public interface PlayerTick {
        void onPlayerTick(ServerPlayerEntity player);
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

    public interface PlayerDeath {
        void onPlayerDeath(ServerPlayerEntity player, DamageSource damageSource);
    }

    public interface PlayerBlockInteract {
        void onPlayerBlockInteraction(BlockState state, ServerWorld world, BlockPos pos, ServerPlayerEntity player, Hand hand, BlockHitResult hitResult);
    }

    public interface Tick {
        void onServerTick();
    }

    public interface CommandRegistration {
        void onCommandRegistration(String scriptId, String commandName, boolean successful);
    }
}
