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
    private static final Set<String> AUTO_COMPLETE = Set.of("", "java.util", "java.lang");

    private static final Map<String, String> CACHED_AUTO_COMPLETE = new HashMap<>();

    public static void addAutoComplete(String prefix) {
        //noinspection DataFlowIssue
        AUTO_COMPLETE.add(prefix);
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