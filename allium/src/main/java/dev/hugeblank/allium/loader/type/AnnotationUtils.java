package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.*;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;

public final class AnnotationUtils {
    private AnnotationUtils() {

    }

    public static boolean isHiddenFromLua(EMember element) {
        return element.declaringClass().hasAnnotation(LuaWrapped.class) && !element.hasAnnotation(LuaWrapped.class);
    }

    public static int countLuaArguments(EExecutable executable) {
        int count = 0;

        for (EParameter param : executable.parameters()) {
            if (param.hasAnnotation(LuaStateArg.class))
                continue;

            count++;
        }

        return count;
    }

    public static int getPriority(EMember element) {
        LuaWrapped luaWrapped = element.annotation(LuaWrapped.class);

        if (luaWrapped == null)
            return 1000;

        return luaWrapped.priority();
    }

    public static String[] findNames(EAnnotated element) {
        LuaWrapped luaWrapped = element.annotation(LuaWrapped.class);
        if (luaWrapped != null && luaWrapped.name().length > 0) {
            return luaWrapped.name();
        }

        return null;
    }
}
