package dev.hugeblank.allium.util;

import com.mojang.datafixers.util.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class ClassFieldBuilder {
    private final String className;
    private final ClassVisitor c;
    private int fieldIndex = 0;
    private final HashMap<String, Pair<Object, Class<?>>> storedFields = new HashMap<>();
    private final HashMap<String, Function<Class<?>, ?>> complexFields = new HashMap<>();

    public ClassFieldBuilder(String className, ClassVisitor c) {
        this.className = className;
        this.c = c;
    }

    public <T> String store(T o, Class<T> fieldType) {
        for (var entry : storedFields.entrySet()) {
            if (o == entry.getValue().getFirst() && fieldType.isAssignableFrom(entry.getValue().getSecond())) {
                return entry.getKey();
            }
        }

        String fieldName = "allium$field" + fieldIndex++;

        var f = c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("value", o.toString());
        a.visitEnd();

        storedFields.put(fieldName, new Pair<>(o, fieldType));
        return fieldName;
    }

    public <T> String storeComplex(Function<Class<?>, T> supplier, Class<T> fieldType, String description) {
        String fieldName = "allium$field" + fieldIndex++;

        var f = c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("description", description);
        a.visitEnd();

        complexFields.put(fieldName, supplier);
        return fieldName;
    }

    public <T> void storeAndGet(MethodVisitor m, T o, Class<T> type) {
        m.visitFieldInsn(GETSTATIC, className, store(o, type), Type.getDescriptor(type));
    }

    public <T> void storeAndGetComplex(MethodVisitor m, Function<Class<?>, T> supplier, Class<T> type, String description) {
        m.visitFieldInsn(GETSTATIC, className, storeComplex(supplier, type, description), Type.getDescriptor(type));
    }

    public void apply(Class<?> builtClass) {
        try {
            for (var entry : storedFields.entrySet()) {
                builtClass.getField(entry.getKey()).set(null, entry.getValue().getFirst());
            }

            for (var entry : complexFields.entrySet()) {
                builtClass.getField(entry.getKey()).set(null, entry.getValue().apply(builtClass));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply fields to class", e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface GeneratedFieldValue {
        String DESCRIPTOR = "Lme/hugeblank/allium/util/ClassFieldBuilder$GeneratedFieldValue;";

        String value() default "";
        String description() default "";
    }
}
