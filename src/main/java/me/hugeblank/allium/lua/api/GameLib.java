package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.BlockPosType;
import me.hugeblank.allium.lua.type.PlayerType;
import net.minecraft.util.math.BlockPos;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

public class GameLib implements LuaLibrary {

    public GameLib() {}

    @Override
    public LuaValue add(LuaState state, LuaTable env) {
        LuaTable lib = new LuaTable();
        // Stuff should be here
        env.rawset("game", lib);
        LibFunction.bind(lib, GameLibFunctions::new, new String[]{
                "getPlayer",
                "getBlockPos",
                "getBlock",
                "getItem"
        });
        state.loadedPackages.rawset("game", lib);
        return lib;
    }

    private static final class GameLibFunctions extends VarArgFunction {

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
            switch (opcode) {
                case 0:
                    return PlayerType.create(args.arg(1).checkString());
                case 1:
                    return new BlockPosType(new BlockPos(args.arg(1).checkDouble(), args.arg(2).checkDouble(), args.arg(3).checkDouble())).build();
                case 2:
                    // TODO Block API
                case 3:
                    // TODO Item API
            }
            return Constants.NIL;
        }
    }
}
