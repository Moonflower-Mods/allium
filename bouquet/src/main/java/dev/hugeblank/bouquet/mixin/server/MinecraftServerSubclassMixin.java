package dev.hugeblank.bouquet.mixin.server;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.test.TestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {MinecraftDedicatedServer.class, TestServer.class})
public class MinecraftServerSubclassMixin {
    @Inject(at = @At("TAIL"), method = "setupServer")
    private void init(CallbackInfoReturnable<Boolean> cir) {
        ServerEvents.SERVER_START.invoker().onServerStart((MinecraftServer) (Object) this);
    }
}
