package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

// The goofiest class to exist in all of Allium.
public class AsmUtil {
    private static final AtomicInteger NEXT_CLASS_ID = new AtomicInteger();
    private static final AtomicInteger NEXT_MIXIN_ID = new AtomicInteger();
    public static final Handle LAMBDA_METAFACTORY = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);

    public static String getUniqueClassName() {
        return "allium/GeneratedClass_" + NEXT_CLASS_ID.incrementAndGet();
    }
    public static String getUniqueMixinClassName() {
        return "allium/mixin/GeneratedClass_" + NEXT_MIXIN_ID.incrementAndGet();
    }

    public static void dumpClass(String name, byte[] bytes) {
        if (Allium.DEVELOPMENT) {
            Path classPath = Allium.DUMP_DIRECTORY.resolve(name + ".class");

            try {
                Files.createDirectories(classPath.getParent());
                Files.write(classPath, bytes);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't dump class", e);
            }
        }
    }

    public static Class<?> loadClass(String name, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        cr.accept(new CheckClassAdapter(new ClassVisitor(Opcodes.ASM9) { }), 0);

        return DefiningClassLoader.INSTANCE.defineClass(name.replace('/', '.'), bytes);
    }

    public static Class<?> defineClass(String name, byte[] bytes) {
        dumpClass(name, bytes);
        return loadClass(name, bytes);
    }

    public static void createArray(MethodVisitor mv, int varIndex, List<Type> args, Class<?> type, ArrayVisitor arrayVisitor) {
        mv.visitLdcInsn(args.size()); // <- 0
        mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(type)); // <- 0 | -> 0;
        mv.visitVarInsn(ASTORE, varIndex); // -> 0

        int argIndex = 0;
        for (int i = 0; i < args.size(); i++) {
            Type arg = args.get(i);
            mv.visitVarInsn(ALOAD, varIndex); // <- 0
            mv.visitLdcInsn(i); // <- 1
            arrayVisitor.visit(mv, argIndex, arg); // pray.
            mv.visitInsn(AASTORE); // -> 0, 1, 2
            argIndex += arg.getSize();
        }

        mv.visitVarInsn(ALOAD, varIndex);
    }

    public interface ArrayVisitor {
        void visit(MethodVisitor visitor, int index, Type arg);
    }

    public static Runnable visitObjectDefinition(MethodVisitor visitor, String internalName, String descriptor) {
        visitor.visitTypeInsn(NEW, internalName);
        visitor.visitInsn(DUP);
        return () -> visitor.visitMethodInsn(
                INVOKESPECIAL,
                internalName,
                "<init>",
                descriptor,
                false
        );
    }

    public static String mapType(Type type) {
        StringBuilder builder = new StringBuilder();
        while (type.getSort() == Type.ARRAY) {
            builder.append("[");
            type = type.getElementType();
        }
        if (type.getSort() == Type.OBJECT) {
            builder.append("L").append(Allium.MAPPINGS.getYarn(type.getInternalName())).append(";");
        } else {
            builder.append(type.getDescriptor());
        }
        return builder.toString();
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

    public static String getWrappedTypeName(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN
                    -> Type.getType(Boolean.class).getClassName();
            case Type.CHAR
                    -> Type.getType(Character.class).getClassName();
            case Type.BYTE
                    -> Type.getType(Byte.class).getClassName();
            case Type.SHORT
                    -> Type.getType(Short.class).getClassName();
            case Type.INT
                    -> Type.getType(Integer.class).getClassName();
            case Type.FLOAT
                    -> Type.getType(Float.class).getClassName();
            case Type.LONG
                    -> Type.getType(Long.class).getClassName();
            case Type.DOUBLE
                    -> Type.getType(Double.class).getClassName();
            default
                    -> type.getClassName();
        };
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
