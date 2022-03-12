package me.hugeblank.allium.lua.event;

import me.hugeblank.allium.lua.type.UserdataTypes;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.ValueFactory;

public class Events {
    public static final Event CHAT_MESSAGE;
    public static final Event PLAYER_TICK;
    public static final Event PLAYER_JOIN;
    public static final Event PLAYER_QUIT;
    public static final Event PLAYER_BLOCK_COLLISION;
    public static final Event PLAYER_DEATH;
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
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_JOIN = new Event("player_join", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_QUIT = new Event("player_quit", (objects) -> {
            // Expects: [ServerPlayerEntity player]
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_BLOCK_COLLISION = new Event("player_block_collision", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0]),
                        UserdataTypes.BLOCKSTATE.create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
        PLAYER_DEATH = new Event("player_death", (objects) -> {
            try {
                return ValueFactory.varargsOf(
                        UserdataTypes.PLAYER.create(objects[0]),
                        UserdataTypes.DAMAGESOURCE.create(objects[1])
                );
            } catch(ClassCastException e) {
                return Constants.NIL;
            }
        });
    }

    public static void init() {}
}
