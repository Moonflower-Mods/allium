package dev.hugeblank.allium.util;

public record VisitedField(int access, String name, String descriptor, String signature, Object value) implements VisitedValue {
}
