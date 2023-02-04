package dev.hugeblank.allium.lua.api;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptResource;
import dev.hugeblank.allium.lua.type.annotation.OptionalArg;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.FileHelper;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.io.IOException;

@LuaWrapped(name = "config")
public class ConfigLib implements WrappedLuaLibrary, ScriptResource {
    private final Script script;
    private LuaValue config = null;



    public ConfigLib(Script script) {
        this.script = script;
    }

    // Compares the default config, and the config obtained from the json.
    // If there's an error, the default value gets used
    private LuaValue check(String key, LuaValue defConf, LuaValue newConf) throws LuaError {
        if (checkType(defConf, newConf)) {
            if (defConf.isTable()) {
                LuaTable defTable = defConf.checkTable();
                LuaTable newTable = newConf.checkTable();
                if (defTable.length() > 0) {
                    for (int i = 1; i <= defTable.length(); i++) {
                        newTable.rawset(i, check(key + "." + i, defTable.rawget(i), newTable.rawget(i)));
                    }
                } else if (defTable.keys().length > 0) {
                    for (LuaValue kValue : defTable.keys()) {
                        if (!kValue.isString()) { // I refuse to error correct until all keys are strings.
                            throw new LuaError(Strings.lenientFormat(
                                    "All keys in config must be of type 'string'. " +
                                            "Got key of type '%s', at element '%s'",
                                    kValue.typeName(),
                                    key
                            ));
                        }
                        String k = kValue.toString();
                        newTable.rawset(kValue, check(key + "." + k, defTable.rawget(k), newTable.rawget(k)));
                    }
                } else {
                    throw new LuaError(Strings.lenientFormat(
                            "Config table at element '%s' cannot have both named and unnamed keys.",
                            key
                    ));
                }
                return newTable;
            } else {
                return newConf;
            }
        } else {
            script.getLogger().warn(Strings.lenientFormat(
                    "Using default value to correct error in config key '%s'. Expected type '%s', got '%s'",
                    key,
                    defConf.typeName(),
                    newConf.typeName()
            ));
            return defConf;
        }
    }

    private static boolean checkType(LuaValue defConf, LuaValue newConf) {
        return defConf.typeName().matches(newConf.typeName());
    }

    @LuaWrapped
    public LuaValue open(@OptionalArg LuaValue defaultConfig) throws LuaError {
        // Opens the config json, parsing compared to the default table. If any keys don't match type to the provided,
        // or the key is outright missing, it gets replaced with the default passed in.
        LuaValue cached = null;
        try {
            cached = JsonLib.fromJson(FileHelper.getConfig(script));
        } catch (IOException ignore) {}

        if (cached == null || cached.isNil()) {
            if (defaultConfig.isNil()) return Constants.NIL;
            config = defaultConfig;
            script.getLogger().info("Creating default config file, since none was found");
            flush();
        } else {
            // If there's no default, assume developer doesn't want to use error checking/correcting
            if (defaultConfig.isNil()) return cached;
            config = check("root", defaultConfig, cached);
        }
        return config;
    }

    @LuaWrapped
    public void flush() throws LuaError {
        if (config == null) {
            throw new LuaError("Attempt to flush config file before opening");
        }
        JsonElement json = JsonLib.toJsonElement(config);
        try {
            FileHelper.saveConfig(script, json);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    public void close() {
        // Save config to json file
        try {
            flush();
        } catch (LuaError e) {
            script.getLogger().error("Failed to save config", e);
        }
    }
}
