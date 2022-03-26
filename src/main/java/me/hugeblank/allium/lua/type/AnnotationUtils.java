package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.EAnnotated;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;

public final class AnnotationUtils {
    private AnnotationUtils() {

    }

    public static boolean isHiddenFromLua(EMember element) {
        return element.declaringClass().hasAnnotation(LuaWrapped.class) && !element.hasAnnotation(LuaWrapped.class);
    }

    public static String[] findNames(EAnnotated element) {
        LuaWrapped luaWrapped = element.annotation(LuaWrapped.class);
        if (luaWrapped != null && luaWrapped.name().length > 0) {
            return luaWrapped.name();
        }

        return null;
    }
}
