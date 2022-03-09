package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.PlayerType;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

public class PlayerLib implements LuaLibrary {

    public PlayerLib() {}

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        env.rawset("players", lib);
        LibFunction.bind(lib, PlayerLibFunctions::new, new String[]{
                "getPlayer"
        });
        state.loadedPackages.rawset("players", lib);
        return lib;
    }

    private static final class PlayerLibFunctions extends VarArgFunction {

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            switch (opcode) {
                case 0:
                    return new PlayerType(PlayerType.checkPlayer(state, args.arg(1), Allium.SERVER)).build();
            }
            return Constants.NIL;
        }
    }
}
