package dev.hugeblank.allium.lua.api.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import dev.hugeblank.allium.lua.api.JavaLib;
import dev.hugeblank.allium.lua.type.annotation.LuaIndex;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;

@LuaWrapped
public class ArgumentTypeLib {
    private static final Map<String, Class<? extends ArgumentType<?>>> types = new HashMap<>();

    public static void addType(String id, Class<? extends ArgumentType<?>> type) {
        types.put(id, type);
    }

    @LuaIndex
    public LuaValue index(String type) throws LuaError {
        // type ID from ArgumentTypes.class
        String toLoad;
        if (types.containsKey(type)) {
            toLoad = type;
        } else if (types.containsKey("brigadier:" + type)) {
            toLoad = "brigadier:" + type;
        } else {
            return null;
        }
        Class<? extends ArgumentType<?>> clazz = types.get(toLoad);
        try {
            return JavaLib.importClass(EClass.fromJava(clazz));
        } catch (Exception e) {
            throw new LuaError(e);
        }
    }
}
