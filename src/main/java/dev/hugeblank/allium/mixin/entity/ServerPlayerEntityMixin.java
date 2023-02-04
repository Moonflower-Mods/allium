package dev.hugeblank.allium.mixin.entity;

import dev.hugeblank.allium.util.EntityDataHolder;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void allium_private$copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ((EntityDataHolder) this).allium_private$copyFromData(oldPlayer);
    }
}
