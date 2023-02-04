package dev.hugeblank.allium.mixin.network;

import dev.hugeblank.allium.lua.api.DefaultEventsLib;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/server/filter/FilteredMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/registry/RegistryKey;)V"), method = "handleDecoratedMessage")
    private void onChatMessage(FilteredMessage<SignedMessage> message, CallbackInfo ci) {
        var msg = message.raw().getContent();
        DefaultEventsLib.CHAT_MESSAGE.invoker().onChatMessage(player, msg.getString());
    }
}
