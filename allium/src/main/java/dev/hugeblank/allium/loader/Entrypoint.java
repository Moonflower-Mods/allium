package dev.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

@JsonAdapter(Entrypoint.Adapter.class)
public class Entrypoint {

    @SerializedName("static")
    private final String eStatic;
    @SerializedName("dynamic")
    private final String eDynamic;
    @SerializedName("preLaunch")
    private final String ePreLaunch;

    public Entrypoint( String eStatic, String eDynamic, String eMixin) {
        this.eStatic = eStatic;
        this.eDynamic = eDynamic;
        this.ePreLaunch = eMixin;
    }

    public boolean valid() {
        return containsStatic() || containsDynamic();
    }

    public boolean containsStatic() {
        return eStatic != null;
    }

    public boolean containsDynamic() {
        return eDynamic != null;
    }

    public boolean containsPreLaunch() {
        return ePreLaunch != null;
    }

    public String getStatic() {
        return eStatic;
    }

    public String getDynamic() {
        return eDynamic;
    }

    public String getPreLaunch() {
        return ePreLaunch;
    }

    public static class Adapter implements JsonDeserializer<Entrypoint> {

        @Override
        public Entrypoint deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            String eStatic = json.has("static") ? json.getAsJsonPrimitive("static").getAsString() : null;
            String eDynamic = json.has("dynamic") ? json.getAsJsonPrimitive("dynamic").getAsString() : null;
            String eMixin = json.has("preLaunch") ? json.getAsJsonPrimitive("preLaunch").getAsString() : null;

            return new Entrypoint(eStatic, eDynamic, eMixin);
        }
    }
}
