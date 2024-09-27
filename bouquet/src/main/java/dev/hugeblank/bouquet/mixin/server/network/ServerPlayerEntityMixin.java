package dev.hugeblank.bouquet.mixin.server.network;

import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.util.EntityDataHolder;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin{

    @Inject(at = @At("HEAD"), method = "onSpawn")
    private void onPlayerConnect(CallbackInfo ci) {
        ServerEvents.PLAYER_JOIN.invoker().onPlayerJoin((ServerPlayerEntity)(Object) this);
    }

    @Inject(at = @At("HEAD"), method = "onDisconnect")
    private void onDisconnect(CallbackInfo ci) {
        ServerEvents.PLAYER_QUIT.invoker().onPlayerQuit((ServerPlayerEntity)(Object) this);
    }

    @Inject(at = @At("HEAD"), method = "onBlockCollision")
    private void onBlockCollision(BlockState state, CallbackInfo ci) {
        ServerEvents.PLAYER_BLOCK_COLLISION.invoker().onPlayerBlockCollision((ServerPlayerEntity)(Object) this, state);
    }

    @Inject(at = @At("RETURN"), method = "copyFrom")
    private void allium_private$copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ((EntityDataHolder) this).allium_private$copyFromData(oldPlayer);
    }
}
