package me.hugeblank.allium.util;

import com.mojang.datafixers.util.Pair;
import me.hugeblank.allium.Allium;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
        c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        storedFields.put(fieldName, new Pair<>(o, fieldType));
        return fieldName;
    }

    public <T> String storeComplex(Function<Class<?>, T> supplier, Class<T> fieldType) {
        String fieldName = "allium$field" + fieldIndex++;
        c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        complexFields.put(fieldName, supplier);
        return fieldName;
    }

    public <T> void storeAndGet(MethodVisitor m, T o, Class<T> type) {
        m.visitFieldInsn(GETSTATIC, className, store(o, type), Type.getDescriptor(type));
    }

    public <T> void storeAndGetComplex(MethodVisitor m, Function<Class<?>, T> supplier, Class<T> type) {
        m.visitFieldInsn(GETSTATIC, className, storeComplex(supplier, type), Type.getDescriptor(type));
    }

    public void apply(Class<?> builtClass) {
        Map<String, Object> computedFields = new HashMap<>();

        for (var entry : storedFields.entrySet()) {
            computedFields.put(entry.getKey(), entry.getValue().getFirst());
        }

        for (var entry : complexFields.entrySet()) {
            computedFields.put(entry.getKey(), entry.getValue().apply(builtClass));
        }

        if (Allium.DEVELOPMENT) {
            Path dumpPath = Path.of("allium-dump", className + "-fields.txt");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpPath))) {
                writer.println("Fields of " + className + ":");

                for (var entry : computedFields.entrySet()) {
                    writer.printf("%s = %s%n", entry.getKey(), entry.getValue());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to dump fields for class " + className, e);
            }
        }

        try {
            for (var entry : computedFields.entrySet()) {
                builtClass.getField(entry.getKey()).set(null, entry.getValue());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply fields to class", e);
        }
    }
}
