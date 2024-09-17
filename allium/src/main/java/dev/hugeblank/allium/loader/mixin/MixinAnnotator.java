package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.util.Mappings;
import dev.hugeblank.allium.util.asm.VisitedClass;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.squiddev.cobalt.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A masterpiece
public class MixinAnnotator {

    private static final Map<Class<?>, Matcher> MAPPING_TYPES = new HashMap<>();

    private final VisitedClass visitedClass;

    public MixinAnnotator(VisitedClass visitedClass) {
        this.visitedClass = visitedClass;
    }

    private void annotate(LuaState state, LuaValue value, AnnotationVisitor visitor, EClass<?> clazz) throws LuaError, InvalidArgumentException {
        LuaTable table = value.checkTable();
        for (EMethod method : clazz.methods()) {
            String name = method.name();
            LuaValue nextValue = table.rawget(name);
            EClass<?> returnType = method.rawReturnType();
            Matcher matcher = MAPPING_TYPES.get(clazz.raw());
            boolean remap = matcher != null && matcher.matches(name);
            if (!nextValue.isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(state, visitor, remap, name, nextValue, returnType);
                } else {
                    visitValue(state, visitor, remap, name, nextValue, returnType);
                }
            } else if (name.equals("value") && !table.rawget(1).isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(state, visitor, remap, name, table.rawget(1), returnType);
                } else {
                    visitValue(state, visitor, remap, "value", table.rawget(1), returnType);
                }
            }
        }
    }

    private void annotateArray(LuaState state, AnnotationVisitor visitor, boolean remap, String name, LuaValue nextValue, EClass<?> returnType) throws LuaError, InvalidArgumentException {
        AnnotationVisitor array = visitor.visitArray(name);
        if (nextValue instanceof LuaString || (nextValue instanceof LuaTable table && table.length() == 0)) {
            visitValue(state, array, remap, null, nextValue, returnType.arrayComponent());
        } else {
            LuaTable nextTable = nextValue.checkTable();
            for (int i = 0; i < nextTable.length(); i++) {
                visitValue(state, array, remap, null, nextTable.rawget(i + 1), returnType.arrayComponent());
            }
        }
        array.visitEnd();
    }

    private void visitValue(LuaState state, AnnotationVisitor visitor, boolean remap, String name, LuaValue value, EClass<?> returnType) throws LuaError, InvalidArgumentException {
        if (!value.isNil()) {
            if (remap && value instanceof LuaString) {
                ITargetSelectorRemappable mapped = MemberInfo.parse(
                        value.checkString(),
                        BabysFirstSelectorContext.INSTANCE
                );
                // TODO: check if the user is targetting a field or method, then find the intermediary value with that tag (field_###/method_###)
                // This is so incredibly messed up.
                String mappedOwner = mapped.getOwner() == null ? visitedClass.mappedClassName() : mapped.getOwner();
                String unmappedOwner = mapped.getOwner() == null ? null : Allium.MAPPINGS.getIntermediary(Mappings.asClass(mappedOwner)).get(0).replace(".", "/");
                String unmappedName = Allium.MAPPINGS.getIntermediary(Mappings.asMethod(mappedOwner, mapped.getName())).get(0);
                if (unmappedName != null) {
                    String[] split = unmappedName.split("#");
                    unmappedName = split.length == 2 ? split[1] : null;
                }
                final String unmappedDesc;
                if (mapped.getDesc() == null) {
                    unmappedDesc = null;
                } else if (mapped.getDesc().startsWith("(")) {
                    unmappedDesc = unmapMethodDescriptor(mapped.getDesc());
                } else {
                    unmappedDesc = unmapFieldDescriptor(mapped.getDesc());
                }
                mapped = mapped.remapUsing(new MappingMethod(unmappedOwner, unmappedName, unmappedDesc), true);
                value = ValueFactory.valueOf(mapped.toString());
                //value = ValueFactory.valueOf(mappings.get(name).apply(visitedClass,  value.checkString()).fullMixinName());
            }
            if (returnType.raw().isAnnotation()) {
                AnnotationVisitor nextVisitor = visitor.visitAnnotation(name, returnType.raw().descriptorString());
                if (value instanceof LuaString) {
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
    public AnnotationVisitor attachAnnotation(MethodVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public AnnotationVisitor attachAnnotation(FieldVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public void annotateMethod(LuaState state, LuaTable annotationTable, MethodVisitor methodVisitor, EClass<?> annotation) throws InvalidArgumentException, LuaError {
        if (annotation.raw().isAnnotation()) {
            AnnotationVisitor visitor = attachAnnotation(methodVisitor, annotation.raw());
            annotate(state, annotationTable, visitor, annotation);
            visitor.visitEnd();
            return;
        }
        throw new InvalidArgumentException("Class must be an annotation");
    }

    private static void unmapTypeArg(StringBuilder unmappedDescriptor, Type arg) {
        if (arg.getSort() == Type.OBJECT) {
            unmappedDescriptor
                    .append("L")
                    .append(Allium.MAPPINGS.getIntermediary(Mappings.asClass(arg.getInternalName())).get(0).replace(".", "/"))
                    .append(";");
        } else {
            unmappedDescriptor.append(arg.getInternalName());
        }
    }

    private static String unmapFieldDescriptor(String descriptor) {
        StringBuilder builder = new StringBuilder();
        unmapTypeArg(builder, Type.getType(descriptor));
        return builder.toString();
    }

    private static String unmapMethodDescriptor(String descriptor) {
        StringBuilder unmappedDescriptor = new StringBuilder("(");
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            unmapTypeArg(unmappedDescriptor, arg);
        }
        unmappedDescriptor.append(")");
        unmapTypeArg(unmappedDescriptor, Type.getReturnType(descriptor));
        return unmappedDescriptor.toString();
    }

    static {
        MAPPING_TYPES.put(At.class, new Matcher("target"));

        Matcher injectors = new Matcher("method");
        MAPPING_TYPES.put(Inject.class, injectors);
        MAPPING_TYPES.put(Redirect.class, injectors);

        Matcher accessors = new Matcher("value");
        MAPPING_TYPES.put(Accessor.class, accessors);
        MAPPING_TYPES.put(Invoker.class, accessors);
    }

    private static class Matcher {
        private final List<String> matches;

        public Matcher(String... matches) {
            this.matches = List.of(matches);
        }

        public boolean matches(String key) {
            return matches.contains(key);
        }
    }
}
