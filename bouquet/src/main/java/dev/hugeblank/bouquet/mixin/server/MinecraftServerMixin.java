package dev.hugeblank.bouquet.mixin.server;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    
    @Inject(at = @At("TAIL"), method = "tick")
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerEvents.SERVER_TICK.invoker().onServerTick((MinecraftServer) (Object) this);
    }
}
