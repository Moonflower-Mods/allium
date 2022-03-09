package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Plugin;
import me.hugeblank.allium.lua.event.Event;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

public record AlliumLib(Plugin plugin) implements LuaLibrary {

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        LibFunction.bind(lib, () -> new AlliumLibTwoArgFunction(this.plugin), new String[]{"onEvent"});
        env.rawset("allium", lib);
        state.loadedPackages.rawset("allium", lib);
        return lib;
    }

    private static final class AlliumLibTwoArgFunction extends TwoArgFunction {
        private final Plugin plugin;

        AlliumLibTwoArgFunction(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable {
            switch (opcode) {
                case 0: // onEvent
                    String eName = arg1.checkString();
                    Event event = Event.get(eName);
                    if (event != null) {
                        event.addListener(this.plugin, arg2.checkFunction());
                        this.plugin.getLogger().info("registered listener");
                    } else {
                        throw new LuaError("No event of type '" + eName + "' exists");
                    }
                    break;
                case 1:
                    // nothing yet
            }
            return Constants.NIL;
        }
    }
}
