package me.hugeblank.allium.mixin.recipe;

import com.google.gson.JsonElement;
import me.hugeblank.allium.lua.api.recipe.RecipeLib;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Shadow private Map<Identifier, Recipe<?>> recipesById;

    @Shadow private Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/recipe/RecipeManager;recipesById:Ljava/util/Map;", shift = At.Shift.AFTER))
    private void makeByIdMapMutable(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        recipesById = new HashMap<>(recipesById);
    }

    @Redirect(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;toImmutableMap(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/stream/Collector;"))
    private <T, K, V> Collector<T, ?, Map<K, V>> makeMapMutable(Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return Collectors.toMap(keyFunction, valueFunction);
    }

    @Inject(method = "method_20703", at = @At("RETURN"), cancellable = true)
    private static <K, V> void makeSubMapMutable(Map.Entry<K, V> entry, CallbackInfoReturnable<Map<K, V>> cir) {
        cir.setReturnValue(new HashMap<>(cir.getReturnValue()));
    }

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("RETURN"))
    private void invokeRecipeModifiers(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        RecipeLib.runRecipeEvents(recipes, recipesById);
    }
}
