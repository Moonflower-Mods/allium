package dev.hugeblank.allium.loader.type.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoerceToNative {
}
