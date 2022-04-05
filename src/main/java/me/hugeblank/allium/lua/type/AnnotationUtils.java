package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.*;
import me.hugeblank.allium.lua.type.annotation.LuaStateArg;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;

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

    public static String[] findNames(EAnnotated element) {
        LuaWrapped luaWrapped = element.annotation(LuaWrapped.class);
        if (luaWrapped != null && luaWrapped.name().length > 0) {
            return luaWrapped.name();
        }

        return null;
    }
}
