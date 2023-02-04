package dev.hugeblank.allium.lua.api.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record CommandRegisterEntry(
        Script script,
        LiteralArgumentBuilder<ServerCommandSource> builder,
        CommandManager.RegistrationEnvironment environment
        ) {}
