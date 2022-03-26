package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.api.commands.CommandRegisterEntry;
import me.hugeblank.allium.lua.event.Event;
import me.hugeblank.allium.lua.type.LuaWrapped;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@LuaWrapped
public class AlliumLib implements WrappedLuaLibrary {
    private final Script script;
    public static final List<CommandRegisterEntry>
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

    @Override
    public String getLibraryName() {
        return "allium";
    }
}
