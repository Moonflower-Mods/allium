package me.hugeblank.allium.lua.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.api.commands.CommandEntry;
import me.hugeblank.allium.lua.event.Event;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.OptionalArg;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@LuaWrapped
public class AlliumLib implements WrappedLuaLibrary {
    private final Script script;
    public static final List<CommandEntry>
            COMMANDS = new ArrayList<>();


    public AlliumLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public Supplier<Boolean> onEvent(String eName, LuaFunction handler) throws LuaError {
        Event event = Event.get(eName);
        if (event != null) {
            event.addListener(script, handler);
        } else {
            throw new LuaError("No event of type '" + eName + "' exists");
        }
        return () -> event.removeListener(script, handler); // return an unsubscribe function
    }

    @LuaWrapped
    public void command(LiteralArgumentBuilder<ServerCommandSource> builder,
                        @OptionalArg CommandManager.RegistrationEnvironment environment) {
        COMMANDS.add(new CommandEntry(
                script,
                builder,
                environment == null ? CommandManager.RegistrationEnvironment.ALL : environment
        ));
    }

    @Override
    public String getLibraryName() {
        return "allium";
    }
}
