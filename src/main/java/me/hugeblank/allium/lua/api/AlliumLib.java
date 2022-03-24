package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.event.Event;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

public record AlliumLib(Script script) {

    public static LuaLibrary create(Script script) {
        return LibBuilder.create("allium")
                .set("onEvent", (state, args) -> onEvent(args, script))
                .set("removeListener", (state, args) -> removeListener(args, script))
                .build();
    }

    private static LuaValue onEvent(Varargs args, Script script) throws LuaError {
        String eName = args.arg(1).checkString();
        Event event = Event.get(eName);
        if (event != null) {
            event.addListener(script, args.arg(2).checkFunction());
        } else {
            throw new LuaError("No event of type '" + eName + "' exists");
        }
        return args.arg(2);
    }

    private static LuaValue removeListener(Varargs args, Script script) throws LuaError {
        LuaFunction func = args.arg(1).checkFunction();
        final boolean[] removed = {false};
        Event.getEvents().values().forEach((event) -> {
            if (event.removeListener(script, func)) removed[0] = true;

        });
        return removed[0] ? Constants.TRUE : Constants.FALSE;
    }
}
