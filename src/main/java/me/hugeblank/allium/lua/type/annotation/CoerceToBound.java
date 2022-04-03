package me.hugeblank.allium.lua.type.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoerceToBound {
}
