package me.hugeblank.allium.lua.type;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

public class BlockPosType {
    private final LuaTable tbl = new LuaTable();

    public BlockPosType(BlockPos pos) {
        LibFunction.bind(
                tbl,
                () -> new BlockPosFunctions(pos),
                new String[]{
                        "add",
                        "multiply",
                        "up",
                        "down",
                        "north",
                        "east",
                        "south",
                        "west",
                        "crossProduct",
                        "withY",
                        "getX",
                        "getY",
                        "getZ"
                });
    }

    public LuaTable build() {
        return tbl;
    }

    public static BlockPos checkBlockPos(LuaState state, LuaValue value) throws LuaError, UnwindThrowable {
        LuaTable tbl = value.checkTable();
        int x = tbl.rawget("getX").checkFunction().call(state).checkInteger();
        int y = tbl.rawget("getY").checkFunction().call(state).checkInteger();
        int z = tbl.rawget("getZ").checkFunction().call(state).checkInteger();
        return new BlockPos(x, y, z);
    }

    private static final class BlockPosFunctions extends VarArgFunction {
        private final BlockPos pos;

        public BlockPosFunctions(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            return switch (opcode) {
                case 0 -> // add(double, double, double)
                        new BlockPosType(pos.add(
                                args.arg(1).checkDouble(),
                                args.arg(2).checkDouble(),
                                args.arg(3).checkDouble()
                        )).build();
                case 1 -> // multiply(int)
                        new BlockPosType(pos.multiply(args.arg(1).checkInteger())).build();
                case 2 -> // up(opt-int)
                        new BlockPosType(pos.up(args.arg(1).optInteger(1))).build();
                case 3 -> // down(opt-int)
                        new BlockPosType(pos.down(args.arg(1).optInteger(1))).build();
                case 4 -> // north(opt-int)
                        new BlockPosType(pos.north(args.arg(1).optInteger(1))).build();
                case 5 -> // south(opt-int)
                        new BlockPosType(pos.south(args.arg(1).optInteger(1))).build();
                case 6 -> // east(opt-int)
                        new BlockPosType(pos.east(args.arg(1).optInteger(1))).build();
                case 7 -> // west(opt-int)
                        new BlockPosType(pos.west(args.arg(1).optInteger(1))).build();
                case 8 -> // crossProduct(int, int, int)
                        new BlockPosType(pos.crossProduct(new Vec3i(
                                args.arg(1).checkInteger(),
                                args.arg(2).checkInteger(),
                                args.arg(3).checkInteger()
                        ))).build();
                case 9 -> // withY(int)
                        new BlockPosType(pos.withY(args.arg(1).checkInteger())).build();
                case 10 -> // getX()
                        ValueFactory.valueOf(pos.getX());
                case 11 -> // getY()
                        ValueFactory.valueOf(pos.getY());
                case 12 -> // getZ()
                        ValueFactory.valueOf(pos.getZ());
                default -> Constants.NIL;
            };
        }
    }
}
