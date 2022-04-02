package me.hugeblank.allium.util;

import com.google.gson.*;
import org.squiddev.cobalt.*;

public final class TableJsonParser {
    // Converts a LuaValue to a JsonElement, effectively lua -> json translation
    // Tables that have at least one un-named key are treated as arrays, rather than objects
    // Tables that are meant to be parsed as a json object MUST have string keys.
    public static JsonElement toJson(LuaValue value) throws LuaError {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value.isTable()) {
            LuaTable table = value.checkTable();
            if (table.length() > 0) {
                JsonArray out = new JsonArray();
                for (int i = 1; i <= table.length(); i++) {
                    out.add(toJson(table.rawget(i)));
                }
                return out;
            } else {
                JsonObject out = new JsonObject();
                for (LuaValue key : table.keys()) {
                    if (!key.isString()) {
                        throw new LuaError("Expected json object key of type 'string', got " + key.typeName());
                    }
                    String k = key.toString();
                    out.add(k, toJson(table.rawget(k)));
                }
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
        } else {
            throw new LuaError("Could not parse value " + value);
        }
    }

    // Converts a JsonElement to a LuaValue, effectively json -> lua translation
    public static LuaValue toLua(JsonElement object) {
        if (object == null) return Constants.NIL;
        if (object.isJsonObject()) {
            LuaTable out = new LuaTable();
            JsonObject json = object.getAsJsonObject();
            json.entrySet().forEach((entry) -> out.rawset(entry.getKey(), toLua(entry.getValue())));
            return out;
        } else if (object.isJsonArray()) {
            LuaTable out = new LuaTable();
            JsonArray json = object.getAsJsonArray();
            for (int i = 0; i < json.size(); i++) {
                out.rawset(i+1, toLua(json.get(i)));
            }
            return out;
        } else if (object.isJsonPrimitive()) {
            JsonPrimitive primitive = object.getAsJsonPrimitive();
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
}
