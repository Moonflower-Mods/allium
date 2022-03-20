package me.hugeblank.allium.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class AsmUtil {
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
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "(Ljava/lang/Boolean;)Z", false);
            case Type.CHAR
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "(Ljava/lang/Character;)C", false);
            case Type.BYTE
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "(Ljava/lang/Byte;)B", false);
            case Type.SHORT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "(Ljava/lang/Short;)S", false);
            case Type.INT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "(Ljava/lang/Integer;)I", false);
            case Type.FLOAT
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Float.class), "floatValue", "(Ljava/lang/Float;)F", false);
            case Type.LONG
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "(Ljava/lang/Long;)J", false);
            case Type.DOUBLE
                -> m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "(Ljava/lang/Double;)D", false);
        }
    }
}
