package dev.hugeblank.allium.mixin.command;

import com.mojang.brigadier.context.CommandContext;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.server.command.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {
    @Inject(at = @At("HEAD"), method = "method_13530(Lcom/mojang/brigadier/context/CommandContext;)I")
    private static void executes(CommandContext<?> context, CallbackInfoReturnable<Integer> cir) {
        Allium.CANDIDATES.forEach(Script::reload);
    }
}
