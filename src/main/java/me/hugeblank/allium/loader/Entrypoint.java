package me.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

@JsonAdapter(Entrypoint.Adapter.class)
public class Entrypoint {

    @SerializedName("static")
    private String eStatic;
    @SerializedName("dynamic")
    private String eDynamic;
    @Expose(deserialize = false)
    private final Type type;

    public Entrypoint( String eStatic, String eDynamic) {
        this.eStatic = eStatic;
        this.eDynamic = eDynamic;
        if (eStatic != null) {
            this.type = Type.STATIC;
        } else if (eDynamic != null) {
            this.type = Type.DYNAMIC;
        } else {
            this.type = Type.BOTH;
        }
    }

    public boolean valid() {
        return containsStatic() || containsDynamic();
    }

    public boolean hasType(Type t) {
        if (this.type == Type.BOTH) return true;
        return this.type == t;
    }

    public Type getType() {
        return type;
    }

    public boolean containsStatic() {
        return eStatic != null;
    }

    public boolean containsDynamic() {
        return eDynamic != null;
    }

    public String getStatic() {
        return eStatic;
    }

    public String getDynamic() {
        return eDynamic;
    }

    public enum Type {
        STATIC,
        DYNAMIC,
        BOTH
    }

    public static class Adapter implements JsonDeserializer<Entrypoint> {

        @Override
        public Entrypoint deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            String eStatic = json.has("static") ? json.getAsJsonPrimitive("static").getAsString() : null;
            String eDynamic = json.has("dynamic") ? json.getAsJsonPrimitive("dynamic").getAsString() : null;

            return new Entrypoint(eStatic, eDynamic);
        }
    }
}
