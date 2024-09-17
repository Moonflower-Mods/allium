package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Debug;

import java.util.*;

@Debug.Renderer(text = "\"Mappings { ... }\"", hasChildren = "false")
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

    public static String asMethod(EClass<?> clazz, EMethod method) {
        return asMethod(clazz.name(), method.name());
    }

    public static String asMethod(EClass<?> clazz, EField field) {
        return asMethod(clazz.name(), field.name());
    }

    public static String asClass(String className) {
        return className.replace('/', '.');
    }

    public static String asClass(EClass<?> clazz) {
        return asClass(clazz.name());
    }
}
