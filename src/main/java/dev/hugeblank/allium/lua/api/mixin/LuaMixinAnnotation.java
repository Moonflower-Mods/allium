package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LuaMixinAnnotation {

    private static void annotate(LuaState state, LuaValue value, AnnotationVisitor visitor, EClass<?> clazz) throws LuaError, InvalidArgumentException {
        LuaTable table = value.checkTable();
        for (EMethod method : clazz.methods()) {
            String name = method.name();
            LuaValue nextValue = table.rawget(name);
            EClass<?> returnType = method.rawReturnType();
            if (!nextValue.isNil()) {
                if (returnType.raw().isArray()) {
                    AnnotationVisitor array = visitor.visitArray(name);
                    if (nextValue.isString()) {
                        visitValue(state, nextValue, array, returnType.arrayComponent(), null);
                    } else {
                        LuaTable nextTable = nextValue.checkTable();
                        for (int i = 0; i < nextTable.length(); i++) {
                            visitValue(state, nextTable.rawget(i + 1), array, returnType.arrayComponent(), null);
                        }
                    }
                    array.visitEnd();
                } else {
                    visitValue(state, nextValue, visitor, returnType, name);
                }
            }
        }
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

    public static void annotateMethod(LuaState state, LuaTable annotationTable, MethodVisitor methodVisitor, EClass<?> annotation) throws InvalidArgumentException, LuaError {
        if (annotation.raw().isAnnotation()) {
            boolean visible = true;
            if (annotation.hasAnnotation(Retention.class)) {
                visible = annotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME);
            }
            AnnotationVisitor visitor = methodVisitor.visitAnnotation(annotation.raw().descriptorString(), visible);
            annotate(state, annotationTable, visitor, annotation);
            visitor.visitEnd();
            return;
        }
        throw new InvalidArgumentException("Class must be an annotation");
    }
}
