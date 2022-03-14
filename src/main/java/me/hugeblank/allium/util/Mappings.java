package me.hugeblank.allium.util;

import me.hugeblank.allium.Allium;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public record Mappings(Map<String, List<String>> yarn2intermediary, Map<String, String> intermediary2yarn) {
    public static Mappings of(Map<String, String> intermediary2yarn) {
        var yarn2intermediary = new HashMap<String, List<String>>();

        for (var entry : intermediary2yarn.entrySet()) {
            yarn2intermediary.computeIfAbsent(entry.getValue(), (s) -> new ArrayList<>()).add(entry.getKey());
        }

        return new Mappings(yarn2intermediary, intermediary2yarn);
    }

    public List<String> getIntermediary(String value) {
        var val = this.yarn2intermediary.get(value);
        return val != null && !Allium.DEVELOPMENT ? val : List.of(value);
    }

    public String getYarn(String value) {
        return this.intermediary2yarn.getOrDefault(value, value);
    }

    public static String asMethod(String className, String method) {
        return (className + "#" + method).replace('/', '.');
    }

    public static String asMethod(Class<?> clazz, Method method) {
        return asMethod(clazz.getName(), method.getName());
    }

    public static String asMethod(Class<?> clazz, Field field) {
        return asMethod(clazz.getName(), field.getName());
    }

    public static String asClass(String className) {
        return className.replace('/', '.');
    }

    public static String asClass(Class<?> clazz) {
        return asClass(clazz.getName());
    }
}
