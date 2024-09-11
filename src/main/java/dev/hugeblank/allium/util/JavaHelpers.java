package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;

public class JavaHelpers {
    private static final String[] AUTO_COMPLETE = new String[]{
            "",
            "java.util.",
            "java.lang.",
            "net.minecraft.",
            "net.minecraft.item.",
            "net.minecraft.block.",
            "net.minecraft.entity.",
            "net.minecraft.entity.player.",
            "net.minecraft.inventory.",
            "net.minecraft.nbt.",
            "net.minecraft.potion.",
            "net.minecraft.sound.",
            "net.minecraft.text.",
            "net.minecraft.tag.",
            "net.minecraft.village.",
            "net.minecraft.world.",
            "net.minecraft.util.",
            "net.minecraft.util.registry.",
            "net.minecraft.server.",
            "net.minecraft.server.command.",
            "net.minecraft.server.world.",
            "net.minecraft.server.network.",
            "com.mojang."
    };

    private static final Map<String, String> CACHED_AUTO_COMPLETE = new HashMap<>();

    public static EClass<?> getRawClass(String className) throws LuaError {
        var cachedClassName = CACHED_AUTO_COMPLETE.get(className);

        if (cachedClassName != null) {
            try {
                return EClass.fromJava(Class.forName(cachedClassName));
            } catch (ClassNotFoundException e1) {

            }
        }

        for (var auto : AUTO_COMPLETE) {
            try {
                cachedClassName = Allium.MAPPINGS.getIntermediary(auto + className).get(0);
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException e1) {

            }

            try {
                cachedClassName = auto + className;
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException e) {

            }
        }

        throw new LuaError("Couldn't find class \"" + className + "\"");

    }

    @SuppressWarnings("unchecked")
    public static EClass<?> asClass(LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(value.checkString());
        } else if (value.isUserdata(EClass.class)) {
            return value.checkUserdata(EClass.class);
        } else if (value.isUserdata(Class.class)) {
            return EClass.fromJava(value.checkUserdata(Class.class));
        } else if (value.isUserdata()) {
            return EClass.fromJava(value.checkUserdata().getClass());
        } else if (value.isTable() && value.checkTable().rawget("allium_java_class") != Constants.NIL) {
            return value.checkTable().rawget("allium_java_class").checkUserdata(EClass.class);
        } else if (value.isNil()) {
            return null;
        }

        throw new LuaError(new ClassNotFoundException());
    }


}