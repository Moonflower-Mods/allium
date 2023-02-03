package dev.hugeblank.allium.mixin.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.hugeblank.allium.lua.api.commands.ArgumentTypeLib;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArgumentTypes.class)
public class ArgumentTypesMixin {

    @Inject(at = @At("HEAD"), method = "register(Lnet/minecraft/util/registry/Registry;Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/command/argument/serialize/ArgumentSerializer;)Lnet/minecraft/command/argument/serialize/ArgumentSerializer;")
    private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(Registry<ArgumentSerializer<?, ?>> registry, String id, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer, CallbackInfoReturnable<ArgumentSerializer<A, T>> cir) {
        ArgumentTypeLib.addType(id, clazz);
    }

}
