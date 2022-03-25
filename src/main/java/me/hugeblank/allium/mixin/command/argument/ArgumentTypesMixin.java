package me.hugeblank.allium.mixin.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import me.hugeblank.allium.lua.api.commands.ArgumentTypeLib;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArgumentTypes.class)
public class ArgumentTypesMixin {

    @Inject(at = @At("HEAD"), method = "register(Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/command/argument/serialize/ArgumentSerializer;)V")
    private static <T extends ArgumentType<?>> void register(String id, Class<T> argClass, ArgumentSerializer<T> serializer, CallbackInfo ci) {
        ArgumentTypeLib.addType(id, argClass);
    }

}
