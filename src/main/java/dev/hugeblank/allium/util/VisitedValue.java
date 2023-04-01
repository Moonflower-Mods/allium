package dev.hugeblank.allium.util;

public interface VisitedValue {
    int access();
    String name();
    String descriptor();
    String signature();
}
