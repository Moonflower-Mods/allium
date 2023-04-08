package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// A masterpiece
public class AnnotationHelper {

    private static void annotate(LuaState state, LuaValue value, AnnotationVisitor visitor, EClass<?> clazz) throws LuaError, InvalidArgumentException {
        LuaTable table = value.checkTable();
        for (EMethod method : clazz.methods()) {
            String name = method.name();
            LuaValue nextValue = table.rawget(name);
            EClass<?> returnType = method.rawReturnType();
            if (!nextValue.isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(state, visitor, name, nextValue, returnType);
                } else {
                    visitValue(state, nextValue, visitor, returnType, name);
                }
            } else if (name.equals("value") && !table.rawget(1).isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(state, visitor, name, table.rawget(1), returnType);
                } else {
                    visitValue(state, table.rawget(1), visitor, returnType, "value");
                }
            }
        }
    }

    private static void annotateArray(LuaState state, AnnotationVisitor visitor, String name, LuaValue nextValue, EClass<?> returnType) throws LuaError, InvalidArgumentException {
        AnnotationVisitor array = visitor.visitArray(name);
        if (nextValue.isString() || (nextValue.isTable() && nextValue.checkTable().length() == 0)) {
            visitValue(state, nextValue, array, returnType.arrayComponent(), null);
        } else {
            LuaTable nextTable = nextValue.checkTable();
            for (int i = 0; i < nextTable.length(); i++) {
                visitValue(state, nextTable.rawget(i + 1), array, returnType.arrayComponent(), null);
            }
        }
        array.visitEnd();
    }

    private static void visitValue(LuaState state, LuaValue value, AnnotationVisitor visitor, EClass<?> returnType, String name) throws LuaError, InvalidArgumentException {
        if (!value.isNil()) {
            if (returnType.raw().isAnnotation()) {
                AnnotationVisitor nextVisitor = visitor.visitAnnotation(name, returnType.raw().descriptorString());
                if (value.isString()) {
                    LuaTable nextTable = new LuaTable();
                    nextTable.rawset("value", value);
                    annotate(state, nextTable, nextVisitor, returnType);
                } else {
                    annotate(state, value, nextVisitor, returnType);
                }
                nextVisitor.visitEnd();
            } else if (returnType.raw().isEnum()) {
                visitor.visitEnum(name, returnType.raw().descriptorString(), value.checkString());
            } else {
                visitor.visit(name, TypeCoercions.toJava(state, value, returnType));
            }
        }
    }

    // I'm a hater of how ClassVisitor, MethodVisitor, FieldVisitor, etc. aren't all under a common interface.
    public static AnnotationVisitor attachAnnotation(MethodVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public static AnnotationVisitor attachAnnotation(FieldVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public static void annotateMethod(LuaState state, LuaTable annotationTable, MethodVisitor methodVisitor, EClass<?> annotation) throws InvalidArgumentException, LuaError {
        if (annotation.raw().isAnnotation()) {
            AnnotationVisitor visitor = attachAnnotation(methodVisitor, annotation.raw());
            annotate(state, annotationTable, visitor, annotation);
            visitor.visitEnd();
            return;
        }
        throw new InvalidArgumentException("Class must be an annotation");
    }
}
