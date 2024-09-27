package dev.hugeblank.bouquet.api.event;

import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommonEventHandlers {
    public interface PlayerTick {
        void onPlayerTick(PlayerEntity player);
    }

    public interface PlayerBlockInteract {
        void onPlayerBlockInteraction(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult);
    }

    public interface PlayerDeath {
        void onPlayerDeath(PlayerEntity player, DamageSource damageSource);
    }

}
