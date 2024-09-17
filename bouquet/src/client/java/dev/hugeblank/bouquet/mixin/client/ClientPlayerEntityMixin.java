package dev.hugeblank.bouquet.mixin.client;

import dev.hugeblank.bouquet.api.lib.ClientEventsLib;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "tick()V")
    private void tick(CallbackInfo ci) {
        ClientEventsLib.PLAYER_TICK.invoker().onPlayerTick((ClientPlayerEntity)(Object) this);
    }
}
