package dev.hugeblank.bouquet.mixin.server.integrated;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(at = @At("TAIL"), method = "setupServer")
    private void init(CallbackInfoReturnable<Boolean> cir) {
        ServerEvents.SERVER_START.invoker().onServerStart((MinecraftServer) (Object) this);
    }
}
