package dev.moongarden.bouquet.mixin.client.gui.hud;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import dev.moongarden.bouquet.api.event.ClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Definition(id = "GuiGraphicsExtractor", type = GuiGraphicsExtractor.class)
    @Expression("? = new GuiGraphicsExtractor(?,?,?,?)")
    @Inject(at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER), method = "extractRenderState")
    private void renderHead(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
        ClientEvents.GUI_RENDER_HEAD.invoker().onGuiRender((Gui) (Object) this, minecraft, graphics, deltaTracker, shouldRenderLevel, resourcesLoaded);
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState")
    private void renderTail(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
        ClientEvents.GUI_RENDER_TAIL.invoker().onGuiRender((Gui) (Object) this, minecraft, graphics, deltaTracker, shouldRenderLevel, resourcesLoaded);
    }
}
