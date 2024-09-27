package dev.hugeblank.bouquet.api.lib;

import com.google.gson.*;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.AlliumUserdata;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.bouquet.util.TableHelpers;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
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

        if (value instanceof AlliumUserdata<?> userdata && userdata.instanceOf(JsonElement.class)) {
            return userdata.toUserdata(JsonElement.class);
        } else if (value instanceof LuaTable table) {
            if (TableHelpers.isArray(table)) {
                JsonArray out = new JsonArray();
                seenValues.add(table);
                TableHelpers.forEachI(table, (i, v) -> out.add(toJsonElementInternal(v, seenValues)));
                seenValues.remove(table);
                return out;
            } else {
                JsonObject out = new JsonObject();
                seenValues.add(table);
                TableHelpers.forEach(table, (k, v) -> {
                    if (!k.isString()) {
                        throw new LuaError("Expected json object key of type 'string', got " + k.typeName());
                    }
                    out.add(k.toString(), toJsonElementInternal(v, seenValues));
                });
                seenValues.remove(table);
                return out;
            }
        } else if (value instanceof LuaBoolean) {
            return new JsonPrimitive(value.toBoolean());
        } else if (value instanceof LuaInteger) {
            return new JsonPrimitive(value.toInteger());
        } else if (value instanceof LuaNumber) {
            return new JsonPrimitive(value.toDouble());
        } else if (value instanceof LuaString) {
            return new JsonPrimitive(value.toString());
        } else if (value instanceof LuaNil) {
            return JsonNull.INSTANCE;
        }
        throw new LuaError("Could not parse value " + value);
    }
}
