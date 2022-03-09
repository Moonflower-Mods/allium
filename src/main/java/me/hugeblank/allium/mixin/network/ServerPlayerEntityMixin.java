package me.hugeblank.allium.mixin.network;

import me.hugeblank.allium.lua.event.Events;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin{
    @Final
    @Shadow
    public ServerPlayerInteractionManager interactionManager;

    @Inject(at = @At("HEAD"), method = "playerTick()V")
    private void tick(CallbackInfo ci) {
        Events.PLAYER_TICK.queueEvent(((ServerPlayerInteractionManagerAccessor)interactionManager).getPlayer());
    }
}
