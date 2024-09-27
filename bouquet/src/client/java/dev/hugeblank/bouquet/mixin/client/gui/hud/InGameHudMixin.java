package dev.hugeblank.bouquet.mixin.client.gui.hud;

import dev.hugeblank.bouquet.api.event.ClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private final InGameHud thiz = (InGameHud) (Object) this;

    @Inject(at = @At("HEAD"), method = "render")
    private void renderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_HEAD.invoker().onGuiRender(client, context, thiz);
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void renderTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_TAIL.invoker().onGuiRender(client, context, thiz);
    }
}
