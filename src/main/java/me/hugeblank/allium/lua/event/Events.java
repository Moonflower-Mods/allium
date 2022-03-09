package me.hugeblank.allium.lua.event;

import net.minecraft.server.network.ServerPlayerEntity;
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
                ServerPlayerEntity player = (ServerPlayerEntity)objects[0];
                String message = (String)objects[1];
                String name = player.getEntityName();
                String uuid = player.getUuidAsString();
                return ValueFactory.varargsOf(LuaString.valueOf(name), LuaString.valueOf(uuid), LuaString.valueOf(message));
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_TICK = new Event("player_tick", (objects) -> {
            try {
                ServerPlayerEntity player = (ServerPlayerEntity)objects[0];
                String name = player.getEntityName();
                String uuid = player.getUuidAsString();
                return ValueFactory.varargsOf(LuaString.valueOf(name), LuaString.valueOf(uuid));
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
    }

    public static void init() {}
}
