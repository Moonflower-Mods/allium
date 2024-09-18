package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.CoerceToBound;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static dev.hugeblank.bouquet.api.lib.AlliumLib.COMMANDS;

@LuaWrapped(name = "command")
public class CommandLib implements WrappedLuaLibrary {
    private final Script script;

    public CommandLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public void register(LiteralArgumentBuilder<ServerCommandSource> builder, @OptionalArg CommandManager.RegistrationEnvironment environment) {
        COMMANDS.add(new CommandRegisterEntry(
                script,
                builder,
                environment == null ? CommandManager.RegistrationEnvironment.ALL : environment
        ));
    }

    @LuaWrapped(name = "arguments")
    public static final @CoerceToBound ArgumentTypeLib ARGUMENTS = new ArgumentTypeLib();
}
