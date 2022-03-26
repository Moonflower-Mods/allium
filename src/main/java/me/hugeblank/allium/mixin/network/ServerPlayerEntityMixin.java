package me.hugeblank.allium.mixin.network;

import me.hugeblank.allium.lua.api.DefaultEventsLib;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin{

    @Inject(at = @At("HEAD"), method = "playerTick()V")
    private void tick(CallbackInfo ci) {
        DefaultEventsLib.PLAYER_TICK.invoker().onPlayerTick((ServerPlayerEntity)(Object) this);
    }

    @Inject(at = @At("HEAD"), method = "onDisconnect")
    private void onDisconnect(CallbackInfo ci) {
        DefaultEventsLib.PLAYER_QUIT.invoker().onPlayerQuit((ServerPlayerEntity)(Object) this);
    }

    @Inject(at = @At("HEAD"), method = "onBlockCollision")
    private void onBlockCollision(BlockState state, CallbackInfo ci) {
        DefaultEventsLib.PLAYER_BLOCK_COLLISION.invoker().onPlayerBlockCollision((ServerPlayerEntity)(Object) this, state);
    }

    @Inject(at = @At("TAIL"), method = "onDeath")
    private void onDeath(DamageSource source, CallbackInfo ci) {
        DefaultEventsLib.PLAYER_DEATH.invoker().onPlayerDeath((ServerPlayerEntity)(Object) this, source);
    }
}
