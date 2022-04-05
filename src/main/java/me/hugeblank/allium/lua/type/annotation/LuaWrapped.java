package me.hugeblank.allium.lua.type.annotation;

import java.lang.annotation.*;

/**
 * When used on a type, @LuaWrapped makes wrapping methods and fields opt-in: they will only be wrapped if they also have
 * a @LuaWrapped annotation.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LuaWrapped {
    String[] name() default {};
}
