package me.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

@JsonAdapter(Entrypoint.Adapter.class)
public class Entrypoint {

    @SerializedName("static")
    private String eStatic;
    @SerializedName("dynamic")
    private String eDynamic;
    @SerializedName("mixin")
    private String eMixin;

    public Entrypoint( String eStatic, String eDynamic, String eMixin) {
        this.eStatic = eStatic;
        this.eDynamic = eDynamic;
        this.eMixin = eMixin;
    }

    public boolean valid() {
        return hasStatic() || hasDynamic();
    }

    public boolean hasStatic() {
        return eStatic != null;
    }

    public boolean hasDynamic() {
        return eDynamic != null;
    }

    public boolean hasMixin() {
        return eMixin != null;
    }

    public String getStatic() {
        return eStatic;
    }

    public String getDynamic() {
        return eDynamic;
    }

    public String getMixin() {
        return eMixin;
    }

    public static class Adapter implements JsonDeserializer<Entrypoint> {

        @Override
        public Entrypoint deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            String eStatic = json.has("static") ? json.getAsJsonPrimitive("static").getAsString() : null;
            String eDynamic = json.has("dynamic") ? json.getAsJsonPrimitive("dynamic").getAsString() : null;
            String eMixin = json.has("mixin") ? json.getAsJsonPrimitive("mixin").getAsString() : null;

            return new Entrypoint(eStatic, eDynamic, eMixin);
        }
    }
}
