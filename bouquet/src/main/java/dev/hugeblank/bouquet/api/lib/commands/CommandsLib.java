package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collections;

// Functionally similar to ComputerCraft's commands API
// See: https://github.com/cc-tweaked/CC-Tweaked/blob/mc-1.16.x/src/main/java/dan200/computercraft/shared/computer/apis/CommandAPI.java
@LuaWrapped(name = "commands")
public class CommandsLib implements WrappedLuaLibrary {

    @LuaWrapped
    public void exec(MinecraftServer server, String... args) {

        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        manager.executeWithPrefix(source, String.join(" ", args));
    }

    @LuaIndex
    public BoundCommand index(MinecraftServer server, String command) {

        CommandManager manager = server.getCommandManager();
        ServerCommandSource source = server.getCommandSource();
        CommandDispatcher<ServerCommandSource> dispatcher = manager.getDispatcher();
        CommandNode<?> node = dispatcher.findNode(Collections.singleton(command));

        if (node == null) return null;
        else return (args) -> manager.executeWithPrefix(source, (command + " " + String.join(" ", args).trim()));
    }

    @FunctionalInterface
    public interface BoundCommand {
        void exec(String... args);
    }
}
