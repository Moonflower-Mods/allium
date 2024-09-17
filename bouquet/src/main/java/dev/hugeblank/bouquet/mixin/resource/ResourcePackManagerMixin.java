package dev.hugeblank.bouquet.mixin.resource;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
    @Shadow
    @Final
    @Mutable
    private Set<ResourcePackProvider> providers;

    @Inject(at = @At("RETURN"), method = "<init>([Lnet/minecraft/resource/ResourcePackProvider;)V")
    private void init(ResourcePackProvider[] providers, CallbackInfo ci) {
        // TODO: Reimplement Resource Pack Library
        //this.providers = new HashSet<>(this.providers);
        //this.providers.add(new dev.hugeblank.allium.api.lib.AlliumResourcePackProvider());
    }
}
