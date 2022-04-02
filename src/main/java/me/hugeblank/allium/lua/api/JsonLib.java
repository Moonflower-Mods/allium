package me.hugeblank.allium.lua.api;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.hugeblank.allium.lua.type.LuaWrapped;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;

import java.util.Set;

@LuaWrapped(name = "json")
public class JsonLib implements WrappedLuaLibrary {
    @LuaWrapped
    public static LuaValue fromJson(String json) {
        return fromJson(JsonParser.parseString(json));
    }

    @LuaWrapped
    public static LuaValue fromJson(JsonElement element) {
        if (element.isJsonNull()) {
            return Constants.NIL;
        } else if (element instanceof JsonPrimitive prim) {
            if (prim.isString())
                return ValueFactory.valueOf(prim.getAsString());
            else if (prim.isNumber())
                return ValueFactory.valueOf(prim.getAsDouble());
            else if (prim.isBoolean())
                return ValueFactory.valueOf(prim.getAsBoolean());
            else
                throw new UnsupportedOperationException("Unknown JsonPrimitive type");
        } else if (element instanceof JsonArray arr) {
            var table = new LuaTable(arr.size(), 0);

            for (int i = 0; i < arr.size(); i++) {
                table.rawset(i + 1, fromJson(arr.get(i)));
            }

            return table;
        } else if (element instanceof JsonObject obj) {
            var table = new LuaTable(0, obj.size());

            for (var entry : obj.entrySet()) {
                table.rawset(entry.getKey(), fromJson(entry.getValue()));
            }

            return table;
        } else {
            throw new UnsupportedOperationException("Unknown JsonElement type");
        }
    }

    @LuaWrapped
    public static String toJson(LuaValue value) {
        return toJsonElement(value).toString();
    }

    @LuaWrapped
    public static JsonElement toJsonElement(LuaValue value) {
        return toJsonElementInternal(value, new ReferenceOpenHashSet<>());
    }

    private static JsonElement toJsonElementInternal(LuaValue value, Set<LuaValue> seenValues) {
        if (value.isUserdata()) {
            var val = value.toUserdata();
            if (val instanceof JsonElement) {
                return (JsonElement) val;
            }
        }

        if (seenValues.contains(value)) return JsonNull.INSTANCE;

        try {
            return switch (value.type()) {
                case (Constants.TINT) -> new JsonPrimitive(value.checkInteger());
                case (Constants.TBOOLEAN) -> new JsonPrimitive(value.checkBoolean());
                case (Constants.TNUMBER) -> {
                    if (value.isInteger()) yield new JsonPrimitive(value.checkInteger());
                    else if (value.isLong()) yield new JsonPrimitive(value.checkLong());
                    else yield new JsonPrimitive(value.checkDouble());
                }
                case (Constants.TSTRING) -> new JsonPrimitive(value.checkString());
                case (Constants.TTABLE) -> {
                    var table = value.checkTable();
                    seenValues.add(value);

                    if (table.getArrayLength() > 0) {
                        var list = new JsonArray(table.getArrayLength());

                        for (int i = 0; i < table.getArrayLength(); i++) {
                            list.set(i, toJsonElementInternal(table.rawget(i + 1), seenValues));
                        }

                        seenValues.remove(value);

                        yield list;
                    } else {
                        var obj = new JsonObject();

                        for (var key : table.keys()) {
                            var val = table.rawget(key);
                            if (val != Constants.NIL) {
                                obj.add(key.toString(), toJsonElementInternal(val, seenValues));
                            }
                        }

                        seenValues.remove(value);

                        yield obj;
                    }
                }
                case (Constants.TNIL) -> JsonNull.INSTANCE;
                default -> throw ErrorFactory.argError(value, "nil, boolean, number, string or table");
            };
        } catch (Exception e) {
            return null;
        }
    }
}
