package me.hugeblank.allium.lua.api;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.OptionalArg;
import org.squiddev.cobalt.*;

import java.util.Set;

@LuaWrapped(name = "json")
public class JsonLib implements WrappedLuaLibrary {
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    @LuaWrapped
    public static LuaValue fromJson(String json) {
        return fromJson(JsonParser.parseString(json));
    }

    @LuaWrapped
    public static LuaValue fromJson(JsonElement element) {
        if (element == null) return Constants.NIL;
        if (element.isJsonObject()) {
            LuaTable out = new LuaTable();
            JsonObject json = element.getAsJsonObject();
            json.entrySet().forEach((entry) -> out.rawset(entry.getKey(), fromJson(entry.getValue())));
            return out;
        } else if (element.isJsonArray()) {
            LuaTable out = new LuaTable();
            JsonArray json = element.getAsJsonArray();
            for (int i = 0; i < json.size(); i++) {
                out.rawset(i+1, fromJson(json.get(i)));
            }
            return out;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? Constants.TRUE : Constants.FALSE;
            } else if (primitive.isNumber()) {
                return ValueFactory.valueOf(primitive.getAsDouble());
            } else if (primitive.isString()) {
                return ValueFactory.valueOf(primitive.getAsString());
            } else {
                return Constants.NIL;
            }
        } else {
            return Constants.NIL;
        }
    }

    @LuaWrapped
    public static String toJson(LuaValue value, @OptionalArg Boolean compact) throws LuaError {
        JsonElement element = toJsonElement(value);
        if (compact != null && compact) return COMPACT.toJson(element);
        return  PRETTY.toJson(element);
    }

    @LuaWrapped
    public static JsonElement toJsonElement(LuaValue value) throws LuaError {
        return toJsonElementInternal(value, new ReferenceOpenHashSet<>());
    }

    private static JsonElement toJsonElementInternal(LuaValue value, Set<LuaValue> seenValues) throws LuaError {
        if (seenValues.contains(value)) return JsonNull.INSTANCE;

        if (value.isUserdata(JsonElement.class))
            return value.toUserdata(JsonElement.class);

        if (value.isUserdata()) {
            var val = value.toUserdata();
            if (val instanceof JsonElement) {
                return (JsonElement) val;
            }
        } else if (value.isTable()) {
            LuaTable table = value.checkTable();
            if (table.length() > 0) {
                JsonArray out = new JsonArray();
                seenValues.add(value);
                for (int i = 1; i <= table.length(); i++) {
                    out.add(toJsonElementInternal(table.rawget(i), seenValues));
                }
                seenValues.remove(value);
                return out;
            } else {
                JsonObject out = new JsonObject();
                seenValues.add(value);
                for (LuaValue key : table.keys()) {
                    if (!key.isString()) {
                        throw new LuaError("Expected json object key of type 'string', got " + key.typeName());
                    }
                    String k = key.toString();
                    out.add(k, toJsonElementInternal(table.rawget(k), seenValues));
                }
                seenValues.remove(value);
                return out;
            }
        } else if (value.isBoolean()) {
            return new JsonPrimitive(value.toBoolean());
        } else if (value.isInteger()) {
            return new JsonPrimitive(value.toInteger());
        } else if (value.isNumber()) {
            return new JsonPrimitive(value.toDouble());
        } else if (value.isString()) {
            return new JsonPrimitive(value.toString());
        } else if (value.isNil()) {
            return JsonNull.INSTANCE;
        }
        throw new LuaError("Could not parse value " + value);
    }
}
