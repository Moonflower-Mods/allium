package me.hugeblank.allium.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import me.hugeblank.allium.lua.api.AlliumLib;
import me.hugeblank.allium.lua.api.commands.CommandRegisterEntry;
import me.hugeblank.allium.lua.event.Events;
import me.hugeblank.allium.util.DebugLoggerWrapper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Mutable
    @Final
    @Shadow
    private static Logger LOGGER;

    @Mutable
    @Final
    @Shadow
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CommandManager.RegistrationEnvironment environment, CallbackInfo ci) {
        AlliumLib.COMMANDS.forEach((entry) -> {
            if (
                    (
                            environment.equals(entry.environment()) ||
                            entry.environment().equals(CommandManager.RegistrationEnvironment.ALL)
                    ) && this.dispatcher.getRoot().getChild(entry.builder().getLiteral()) == null
            ) {
                this.dispatcher.register(entry.builder());
                allium$queueEvent(entry, true);
                return;
            }
            allium$queueEvent(entry, false);
        });
    }

    @Inject(at = @At("TAIL"), method = "<clinit>")
    private static void clinit(CallbackInfo ci) {
        LOGGER = new DebugLoggerWrapper(LOGGER);
    }

    private static void allium$queueEvent(CommandRegisterEntry entry, boolean result) {
        Events.COMMAND_REGISTER.queueEvent(
                entry.script().getManifest().id(),
                entry.builder().getLiteral(),
                result
        );
    }
}
