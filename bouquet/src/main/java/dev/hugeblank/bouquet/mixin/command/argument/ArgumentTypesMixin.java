package dev.hugeblank.bouquet.mixin.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.hugeblank.bouquet.api.lib.commands.ArgumentTypeLib;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArgumentTypes.class)
public class ArgumentTypesMixin {

    @Inject(at = @At("HEAD"), method = "register(Lnet/minecraft/registry/Registry;Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/command/argument/serialize/ArgumentSerializer;)Lnet/minecraft/command/argument/serialize/ArgumentSerializer;")
    private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(Registry<ArgumentSerializer<?, ?>> registry, String id, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer, CallbackInfoReturnable<ArgumentSerializer<A, T>> cir) {
        ArgumentTypeLib.addType(id, clazz);
    }

}
