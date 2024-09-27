package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.AlliumUserdata;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JavaHelpers {
    private static final Set<String> AUTO_COMPLETE = Set.of("", "java.util", "java.lang");

    private static final Map<String, String> CACHED_AUTO_COMPLETE = new HashMap<>();

    public static void addAutoComplete(String prefix) {
        //noinspection DataFlowIssue
        AUTO_COMPLETE.add(prefix);
    }


    public static <T> T checkUserdata(LuaValue value, Class<T> clazz) throws LuaError {
        if (value instanceof AlliumUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clazz);
            } catch (Exception e) {
                throw new LuaError(e);
            }
        }
        throw new LuaError("value " + value + " is not an instance of AlliumUserData");
    }

    public static EClass<?> getRawClass(String className) throws LuaError {
        var cachedClassName = CACHED_AUTO_COMPLETE.get(className);

        if (cachedClassName != null) {
            try {
                return EClass.fromJava(Class.forName(cachedClassName));
            } catch (ClassNotFoundException ignored) {

            }
        }

        for (var auto : AUTO_COMPLETE) {
            try {
                cachedClassName = Allium.MAPPINGS.getIntermediary(auto + className).get(0);
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException ignored) {

            }

            try {
                cachedClassName = auto + className;
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException ignored) {

            }
        }

        throw new LuaError("Couldn't find class \"" + className + "\"");
    }

    public static EClass<?> asClass(LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(value.checkString());
        } else if (value.isNil()) {
            return null;
        } else if (value instanceof LuaTable table && table.rawget("allium_java_class") instanceof AlliumUserdata<?> userdata) {
            return userdata.toUserdata(EClass.class);
        } else if (value instanceof AlliumUserdata<?> userdata) {
            if (userdata.instanceOf(EClass.class)) {
                return userdata.toUserdata(EClass.class);
            } else if (userdata.instanceOf(Class.class)) {
                //noinspection unchecked
                return EClass.fromJava(userdata.toUserdata(Class.class));
            }
            return EClass.fromJava(userdata.toUserdata().getClass());
        }

        throw new LuaError(new ClassNotFoundException());
    }

}