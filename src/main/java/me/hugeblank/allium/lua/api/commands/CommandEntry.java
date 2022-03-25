package me.hugeblank.allium.lua.api.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.hugeblank.allium.loader.Script;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public record CommandEntry(
        Script script,
        LiteralArgumentBuilder<ServerCommandSource> builder,
        CommandManager.RegistrationEnvironment environment
        ) {}
