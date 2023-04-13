package dev.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

@JsonAdapter(Entrypoint.Adapter.class)
public class Entrypoint {

    @SerializedName("main")
    private String eMain;
    @SerializedName("dynamic")
    private String eDynamic;
    @SerializedName("preLaunch")
    private String ePreLaunch;

    public Entrypoint( String eStatic, String eDynamic, String eMixin) {
        this.eMain = eStatic;
        this.eDynamic = eDynamic;
        this.ePreLaunch = eMixin;
    }

    public boolean valid() {
        return hasMain() || hasDynamic();
    }

    public boolean hasMain() {
        return eMain != null;
    }

    public boolean hasDynamic() {
        return eDynamic != null;
    }

    public boolean hasPreLaunch() {
        return ePreLaunch != null;
    }

    public String getMain() {
        return eMain;
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
            String eStatic = json.has("main") ? json.getAsJsonPrimitive("main").getAsString() : null;
            String eDynamic = json.has("dynamic") ? json.getAsJsonPrimitive("dynamic").getAsString() : null;
            String eMixin = json.has("preLaunch") ? json.getAsJsonPrimitive("preLaunch").getAsString() : null;

            return new Entrypoint(eStatic, eDynamic, eMixin);
        }
    }
}
