package dev.hugeblank.allium.util.asm;

public record VisitedMethod(VisitedClass owner, int access, String name, String descriptor, String signature, String[] exceptions) implements VisitedElement {}
