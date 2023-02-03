package dev.hugeblank.allium.mixin.recipe.fabric;

import net.minecraft.recipe.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "method_20703", at = @At("RETURN"), cancellable = true)
    private static <K, V> void makeSubMapMutable(Map.Entry<K, V> entry, CallbackInfoReturnable<Map<K, V>> cir) {
        cir.setReturnValue(new HashMap<>(cir.getReturnValue()));
    }
}
