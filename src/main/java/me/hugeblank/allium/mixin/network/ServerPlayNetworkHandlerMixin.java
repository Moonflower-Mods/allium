package me.hugeblank.allium.mixin.network;

import me.hugeblank.allium.lua.api.DefaultEventsLib;
import net.minecraft.server.filter.TextStream;
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

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"), method = "handleMessage(Lnet/minecraft/server/filter/TextStream$Message;)V")
    private void onChatMessage(TextStream.Message message, CallbackInfo ci) {
        String msg = message.getFiltered();
        DefaultEventsLib.CHAT_MESSAGE.invoker().onChatMessage(player, msg);
    }
}
