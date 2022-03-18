package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.UserdataFactory;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.LuaLibrary;

public class NbtLib {

    public static LuaLibrary create() {
        return LibBuilder.create("nbt")
                .set("toNbt", (state, args) -> UserdataFactory.toLuaValue(toNbt(args.arg(1))))
                .set("fromNbt", (state, args) -> fromNbt(args.arg(1).checkUserdata(NbtElement.class)))
                .build();
    }


    public static LuaValue fromNbt(NbtElement element) {
        return switch (element.getType()) {
            case NbtElement.BYTE_TYPE, NbtElement.SHORT_TYPE, NbtElement.INT_TYPE -> ValueFactory.valueOf(((AbstractNbtNumber) element).intValue());
            case NbtElement.LONG_TYPE -> ValueFactory.valueOf(((NbtLong) element).longValue());
            case NbtElement.FLOAT_TYPE, NbtElement.DOUBLE_TYPE -> ValueFactory.valueOf(((AbstractNbtNumber) element).doubleValue());
            case NbtElement.BYTE_ARRAY_TYPE -> ValueFactory.valueOf(((NbtByteArray) element).getByteArray());
            case NbtElement.STRING_TYPE -> ValueFactory.valueOf(element.asString());
            case NbtElement.LIST_TYPE -> {
                var list = (NbtList) element;
                var table = new LuaTable();

                for (int i = 0; i < list.size(); i++) {
                    table.rawset(i + 1, fromNbt(list.get(i)));
                }

                yield table;
            }
            case NbtElement.COMPOUND_TYPE -> {
                var list = (NbtCompound) element;
                var table = new LuaTable();

                for (var key : list.getKeys()) {
                    table.rawset(key, fromNbt(list.get(key)));
                }

                yield table;
            }
            case NbtElement.INT_ARRAY_TYPE -> UserdataFactory.toLuaValue(((NbtIntArray) element).getIntArray());
            case NbtElement.LONG_ARRAY_TYPE -> UserdataFactory.toLuaValue(((NbtLongArray) element).getLongArray());
            default -> Constants.NIL;
        };
    }

    @Nullable
    public static NbtElement toNbt(LuaValue value) {
        return toNbtInternal(value, new LuaValue[0]);
    }

    private static NbtElement toNbtInternal(LuaValue value, LuaValue[] obj) {
        if (value.isUserdata()) {
            var val = value.toUserdata();
            if (val instanceof NbtElement) {
                return (NbtElement) val;
            }
        }

        for (int i = 0; i < obj.length; i++) {
            if (obj[i] == value) {
                return null;
            }
        }

        try {
            return switch (value.type()) {
                case (Constants.TINT) -> NbtInt.of(value.checkInteger());
                case (Constants.TBOOLEAN) -> NbtByte.of(value.checkBoolean());
                case (Constants.TNUMBER) -> {
                    if (value.isInteger()) yield NbtInt.of(value.toInteger());
                    else if (value.isLong()) yield NbtLong.of(value.toLong());
                    else yield NbtDouble.of(value.toDouble());
                }
                case (Constants.TSTRING) -> NbtString.of(value.checkString());
                case (Constants.TTABLE) -> {
                    var nbt = new NbtCompound();
                    var table = value.checkTable();
                    var objNew = new LuaValue[obj.length + 1];
                    objNew[0] = value;
                    for (int i = obj.length; i > 0; i--) {
                        objNew[i] = obj[i - 1];
                    }

                    for (var key : table.keys()) {
                        var val = table.rawget(key);
                        if (val != Constants.NIL) {
                            nbt.put(key.toString(), toNbtInternal(val, objNew));
                        }
                    }

                    yield nbt;
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}
