package dev.hugeblank.allium.util.asm;

public record VisitedField(VisitedClass owner, int access, String name, String descriptor, String signature, Object value) implements VisitedElement { }
