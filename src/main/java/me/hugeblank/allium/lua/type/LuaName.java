package me.hugeblank.allium.lua.type;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface LuaName {
    String[] value();
}
