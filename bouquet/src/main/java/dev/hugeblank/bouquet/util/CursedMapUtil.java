package dev.hugeblank.bouquet.util;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

public final class CursedMapUtil {
    private CursedMapUtil() {}

    @SuppressWarnings("RedundantUnmodifiable") private static final Class<?> UNMODIFIABLE_MAP = Collections.unmodifiableMap(Collections.emptyMap()).getClass();

    public static boolean isUnmodifiableMap(Map<?, ?> map) {
        return map instanceof ImmutableMap<?,?> || map.getClass() == UNMODIFIABLE_MAP;
    }
}
