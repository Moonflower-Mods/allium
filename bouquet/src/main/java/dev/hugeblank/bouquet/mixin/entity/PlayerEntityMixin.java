package dev.hugeblank.bouquet.mixin.entity;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(at = @At("TAIL"), method = "tick")
    private void tick(CallbackInfo ci) {
        CommonEvents.PLAYER_TICK.invoker().onPlayerTick((PlayerEntity)(Object) this);
    }

    @Inject(at = @At("TAIL"), method = "onDeath")
    private void onDeath(DamageSource source, CallbackInfo ci) {
        CommonEvents.PLAYER_DEATH.invoker().onPlayerDeath((PlayerEntity) (Object) this, source);
    }
}
