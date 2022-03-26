package me.hugeblank.allium.lua.api.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.api.WrappedLuaLibrary;
import me.hugeblank.allium.lua.type.LuaIndex;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.OptionalArg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static me.hugeblank.allium.lua.api.AlliumLib.COMMANDS;

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
    public static final ArgumentTypeLib ARGUMENTS = new ArgumentTypeLib();
}
