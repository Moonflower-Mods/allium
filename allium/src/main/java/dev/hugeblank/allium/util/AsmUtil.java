package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class AsmUtil {
    private static final AtomicInteger NEXT_CLASS_ID = new AtomicInteger();
    public static final Handle LAMBDA_METAFACTORY = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);

    public static String getUniqueClassName() {
        return "allium/GeneratedClass_" + NEXT_CLASS_ID.incrementAndGet();
    }

    public static Class<?> defineClass(String name, byte[] bytes) {
        if (Allium.DEVELOPMENT) {
            Path classPath = Allium.DUMP_DIRECTORY.resolve(name + ".class");

            try {
                Files.createDirectories(classPath.getParent());
                Files.write(classPath, bytes);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't dump class", e);
            }

            ClassReader cr = new ClassReader(bytes);
            cr.accept(new CheckClassAdapter(new ClassVisitor(Opcodes.ASM9) { }), 0);
        }

        return DefiningClassLoader.INSTANCE.defineClass(name.replace('/', '.'), bytes);
    }

    private static class DefiningClassLoader extends ClassLoader {
        public static final DefiningClassLoader INSTANCE = new DefiningClassLoader(DefiningClassLoader.class.getClassLoader());

        public DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    public static void wrapPrimitive(MethodVisitor m, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Boolean.class), "valueOf", "(Z)Ljava/lang/Boolean;", false);
            case Type.CHAR
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", "(C)Ljava/lang/Character;", false);
            case Type.BYTE
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Byte.class), "valueOf", "(B)Ljava/lang/Byte;", false);
            case Type.SHORT
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Short.class), "valueOf", "(S)Ljava/lang/Short;", false);
            case Type.INT
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", "(I)Ljava/lang/Integer;", false);
            case Type.FLOAT
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Float.class), "valueOf", "(F)Ljava/lang/Float;", false);
            case Type.LONG
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Long.class), "valueOf", "(J)Ljava/lang/Long;", false);
            case Type.DOUBLE
                -> m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Double.class), "valueOf", "(D)Ljava/lang/Double;", false);
        }
    }

    public static void unwrapPrimitive(MethodVisitor m, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
            case Type.CHAR
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C", false);
            case Type.BYTE
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "()B", false);
            case Type.SHORT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "()S", false);
            case Type.INT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "()I", false);
            case Type.FLOAT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Float.class), "floatValue", "()F", false);
            case Type.LONG
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "()J", false);
            case Type.DOUBLE
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "()D", false);
        }
    }
}
