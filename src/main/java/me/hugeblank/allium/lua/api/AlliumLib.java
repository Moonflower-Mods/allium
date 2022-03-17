package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Plugin;
import me.hugeblank.allium.lua.event.Event;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.LuaLibrary;

public record AlliumLib(Plugin plugin) {

    public static LuaLibrary create(Plugin plugin) {
        return LibBuilder.create("allium")
                .set("onEvent", (state, args) -> onEvent(args, plugin))
                .build();
    }

    private static LuaValue onEvent(Varargs args, Plugin plugin) throws LuaError {
        String eName = args.arg(1).checkString();
        Event event = Event.get(eName);
        if (event != null) {
            event.addListener(plugin, args.arg(2).checkFunction());
        } else {
            throw new LuaError("No event of type '" + eName + "' exists");
        }
        return Constants.NIL;
    }
}
