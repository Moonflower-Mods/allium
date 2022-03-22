package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.event.Event;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.LuaLibrary;
import me.hugeblank.allium.lua.type.LuaWrapped;

@LuaWrapped
public class AlliumLib implements WrappedLuaLibrary {
    private final Script script;

    public AlliumLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public void onEvent(String eName, LuaFunction handler) throws LuaError {
        Event event = Event.get(eName);
        if (event != null) {
            event.addListener(script, handler);
        } else {
            throw new LuaError("No event of type '" + eName + "' exists");
        }
    }

    @LuaWrapped
    private boolean removeListener(LuaFunction listener) {
        final boolean[] removed = {false};
        Event.getEvents().values().forEach((event) -> {
            if (event.removeListener(script, listener)) removed[0] = true;

        });
        return removed[0];
    }

    @Override
    public String getLibraryName() {
        return "allium";
    }
}
