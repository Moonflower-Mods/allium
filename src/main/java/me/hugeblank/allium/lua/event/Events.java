package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.lua.type.UserdataTypes;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.ValueFactory;

public class Events {
    public static final Event CHAT_MESSAGE;
    public static final Event PLAYER_TICK;
    static {
        CHAT_MESSAGE = new Event("chat_message", (objects) -> {
            // Expects: [ServerPlayerEntity player, String message]
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0]),
                        LuaString.valueOf((String)objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_TICK = new Event("player_tick", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
    }

    public static void init() {}
}
