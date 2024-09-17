package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JavaHelpers {
    private static final Map<String, String> CACHED_AUTO_COMPLETE = new HashMap<>();

    public static EClass<?> getRawClass(String className) throws LuaError {
        var cachedClassName = CACHED_AUTO_COMPLETE.get(className);

        if (cachedClassName != null) { // Cached
            try {
                return EClass.fromJava(Class.forName(cachedClassName));
            } catch (ClassNotFoundException ignored) {}
        }
        try { // Intermediary
            cachedClassName = Allium.MAPPINGS.getIntermediary(className).get(0);
            var clazz = EClass.fromJava(Class.forName(cachedClassName));
            CACHED_AUTO_COMPLETE.put(className, cachedClassName);
            return clazz;
        } catch (ClassNotFoundException ignored) {}

        try { // Named
            var clazz = EClass.fromJava(Class.forName(className));
            CACHED_AUTO_COMPLETE.put(className, className);
            return clazz;
        } catch (ClassNotFoundException ignored) {}

        throw new LuaError("Couldn't find class \"" + className + "\"");

    }

    public static String getRawClassName(String className) throws LuaError {
        var cachedClassName = CACHED_AUTO_COMPLETE.get(className);

        if (cachedClassName != null) {
            return cachedClassName;
        }

        cachedClassName = Allium.MAPPINGS.getIntermediary(className).get(0);
        if (cachedClassName != null) {
            CACHED_AUTO_COMPLETE.put(className, cachedClassName);
            return cachedClassName;
        }
        
        throw new LuaError("Couldn't find class \"" + className + "\"");

    }

    @SuppressWarnings("unchecked")
    public static EClass<?> asClass(LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(value.checkString());
        } else if (value.isNil()) {
            return null;
        } else if (value.type() == Constants.TTABLE && value.checkTable().rawget("allium_java_class") != Constants.NIL) {
            return value.checkTable().rawget("allium_java_class").checkUserdata(EClass.class);
        } else {
            try {
                return value.checkUserdata(EClass.class);
            } catch (LuaError ignored) {}
            try {
                return EClass.fromJava(value.checkUserdata(Class.class));
            } catch (LuaError ignored) {}
            try {
                return EClass.fromJava(value.checkUserdata().getClass());
            } catch (LuaError ignored) {}
        }


        throw new LuaError(new ClassNotFoundException());
    }


}