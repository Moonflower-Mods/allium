package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.event.MixinEventType;
import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.InvalidMixinException;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import dev.hugeblank.allium.lua.type.annotation.OptionalArg;
import dev.hugeblank.allium.util.asm.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.util.Identifier;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/* Notes on implementation:
 - Accessors & Invokers MUST have target name set as the value in the annotation. We use that to determine which
 method/field to invoke/access
*/

@LuaWrapped
public class MixinClassBuilder {
    public static final Map<String, MixinClassInfo> MIXINS = new HashMap<>();

    private final String className = AsmUtil.getUniqueMixinClassName();
    private final VisitedClass visitedClass;
    private final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private int methodIndex = 0;
    private final Script script;
    private final boolean asInterface;
    private final AnnotationHelper annotationHelper;

    public MixinClassBuilder(String target, boolean asInterface, Script script) throws LuaError {
        if (AlliumPreLaunch.isComplete()) throw new IllegalStateException("Mixin cannot be created outside of preLaunch phase.");
        this.script = script;
        this.asInterface = asInterface;
        this.visitedClass = VisitedClass.ofClass(target);
        this.annotationHelper = new AnnotationHelper(visitedClass);

        EClass<?> superClass = EClass.fromJava(Object.class);
        this.c.visit(
                V17,
                ACC_PUBLIC | (asInterface ? (ACC_INTERFACE | ACC_ABSTRACT) : 0),
                className,
                null,
                superClass.name().replace('.', '/'),
                null
        );

        AnnotationVisitor ma = this.c.visitAnnotation(Mixin.class.descriptorString(), false);
        AnnotationVisitor aa = ma.visitArray("value");
        aa.visit(null, Type.getObjectType(visitedClass.name()));
        aa.visitEnd();
        ma.visitEnd();
    }

    @LuaWrapped
    public MixinEventType inject(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        // TODO: LocalCapture
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        return writeInject(eventName, annotations, EClass.fromJava(Inject.class));
    }

    @LuaWrapped
    public void redirect(String eventName, LuaTable annotations, @OptionalArg Boolean instanceOf) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        // TODO: This doesn't actually work.
        // TODO: instanceOf mode
        writeInject(eventName, annotations, EClass.fromJava(Redirect.class));
    }

    // There's some disadvantages to this system. All shadowed values are made public, and forced to be modifiable.
    // Generally that's what's desired so can ya fault me for doing it this way?
    // TODO: test
    @LuaWrapped
    public void shadow(String target) {
        if (visitedClass.containsField(target)) {
            VisitedField visitedField = visitedClass.getField(target);
            FieldVisitor fieldVisitor = c.visitField(
                    visitedField.access() | ACC_PUBLIC,
                    visitedField.descriptor(),
                    visitedField.name(),
                    visitedField.signature(),
                    null
            );
            //noinspection DuplicatedCode
            annotationHelper.attachAnnotation(fieldVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedField.access() & ACC_FINAL) != 0) {
                annotationHelper.attachAnnotation(fieldVisitor, Final.class).visitEnd();
            }
            if ((visitedField.access() & ACC_SYNTHETIC) != 0) {
                annotationHelper.attachAnnotation(fieldVisitor, Dynamic.class).visitEnd();
            }
            fieldVisitor.visitEnd();
        } else if (visitedClass.containsMethod(target)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(target);
            MethodVisitor methodVisitor = c.visitMethod(
                    visitedMethod.access() | ACC_PUBLIC | ACC_ABSTRACT,
                    visitedMethod.descriptor(),
                    visitedMethod.name(),
                    visitedMethod.signature(),
                    null
            );
            //noinspection DuplicatedCode
            annotationHelper.attachAnnotation(methodVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedMethod.access() & ACC_FINAL) != 0) {
                annotationHelper.attachAnnotation(methodVisitor, Final.class).visitEnd();
            }
            if ((visitedMethod.access() & ACC_SYNTHETIC) != 0) {
                annotationHelper.attachAnnotation(methodVisitor, Dynamic.class).visitEnd();
            }
            methodVisitor.visitEnd();
        }
    }

    @LuaWrapped
    public void accessor(LuaTable annotations) throws InvalidArgumentException, InvalidMixinException, LuaError {
        // Shorthand method for writing both setter and getter accessor methods
        setAccessor(annotations);
        getAccessor(annotations);
    }

    @LuaWrapped
    public void setAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        writeAccessor(true, annotations);
    }

    @LuaWrapped
    public void getAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        writeAccessor(false, annotations);
    }

    @LuaWrapped
    public void invoker(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String methodName = getTargetValue(annotations);
        if (visitedClass.containsMethod(methodName)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(methodName);
            String mappedName = visitedMethod.mappedName();
            this.writeMethod(
                    visitedMethod,
                    ((visitedMethod.access() & ACC_STATIC) != 0) ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    "invoke" +
                            mappedName.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                            mappedName.substring(1), // Rest of name
                    List.of(Type.getArgumentTypes(visitedMethod.descriptor())),
                    Type.getReturnType(visitedMethod.descriptor()),
                    ((visitedMethod.access() & ACC_STATIC) != 0) ? List.of((visitor, desc, offset) -> {
                        AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(AssertionError.class), "()V").run();
                        visitor.visitInsn(ATHROW);
                        visitor.visitMaxs(0,0);
                    }) : List.of(),
                    (methodVisitor) -> annotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, EClass.fromJava(Invoker.class))
            );
        }
    }

    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String fieldName = getTargetValue(annotations);
        if (visitedClass.containsField(fieldName)) {
            VisitedField visitedField = visitedClass.getField(fieldName);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            String mappedName = visitedField.mappedName();
            this.writeMethod(
                    visitedField,
                    ((visitedField.access() & ACC_STATIC) != 0) ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    (isSetter ? "set" : "get") + // set or get
                            mappedName.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                            mappedName.substring(1), // Rest of name
                    isSetter ? List.of(visitedFieldType) : List.of(),
                    isSetter ? Type.VOID_TYPE : visitedFieldType,
                    ((visitedField.access() & ACC_STATIC) != 0) ? List.of((visitor, desc, offset) -> {
                        AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(UnsupportedOperationException.class), "()V").run();
                        visitor.visitInsn(ATHROW);
                        visitor.visitMaxs(0,0);
                    }) : List.of(),
                    (methodVisitor) -> annotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, EClass.fromJava(Accessor.class))
            );
        }
    }

    private String getTargetValue(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        if (!asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "interface");
        String name = null;
        if (annotations.rawget("value").isString()) {
            name = annotations.rawget("value").checkString();
        } else if (annotations.rawget(1).isString()) {
            name = annotations.rawget(1).checkString();
        }
        if (name == null) {
            throw new InvalidArgumentException("Expected field name at key 'value' or index 1");
        } else {
            return name;
        }
    }

    private void loadMap(VisitFieldInsn visitMethod) {
        visitMethod.visit(GETSTATIC, Type.getInternalName(MixinEventType.class), "EVENT_MAP", Type.getDescriptor(Map.class));
    }

    private MixinEventType writeInject(String eventName, LuaTable annotations, EClass<?> clazz) throws LuaError, InvalidMixinException, InvalidArgumentException {
        String descriptor = annotations.rawget("method").checkString();
        if (visitedClass.containsMethod(descriptor)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(descriptor);
            List<Type> params = new ArrayList<>(List.of(Type.getArgumentTypes(visitedMethod.descriptor())));
            params.add(Type.getType(CallbackInfo.class));
            List<Type> locals = new ArrayList<>(params);
            if ((visitedMethod.access() & ACC_STATIC) == 0) {
                locals.add(0, Type.getType("L"+visitedClass.name()+";"));
            }

            this.writeMethod(
                    visitedMethod,
                    ACC_PRIVATE,
                    script.getId() + "$" +
                            visitedMethod.name()
                            .replace("<", "")
                            .replace(">", "") +
                            methodIndex++,
                    params,
                    Type.VOID_TYPE,
                    List.of((methodVisitor, desc, thisVarOffset) -> {
                        // descriptor +1 for CallbackInfo
                        int varPrefix = (Type.getArgumentsAndReturnSizes(visitedMethod.descriptor()) >> 2)+1;
                        AsmUtil.createArray(methodVisitor, varPrefix, locals, Object.class, (visitor, index, arg) -> {
                            visitor.visitVarInsn(ALOAD, index); // <- 2
                            AsmUtil.wrapPrimitive(visitor, arg); // <- 2 | -> 2 (sometimes)
                            if (index == 0) {
                                visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
                            }
                            //visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
                        }); // <- 0
                        loadMap(methodVisitor::visitFieldInsn); // <- 1
                        Runnable identifier = AsmUtil.visitObjectDefinition(
                                methodVisitor,
                                Type.getInternalName(Identifier.class),
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))
                        ); // <- 2, 3
                        methodVisitor.visitLdcInsn(script.getId()+":"+eventName); // <- 4
                        identifier.run(); // -> 3, 4
                        //methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
                        methodVisitor.visitMethodInsn(
                                INVOKEINTERFACE,
                                Type.getInternalName(Map.class),
                                "get",
                                Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                                true
                        ); // -> 1, 2 | <- 1
                        methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(MixinEventType.class)); // <- 1 | -> 1
                        methodVisitor.visitInsn(SWAP); // 0 <-> 1
                        methodVisitor.visitMethodInsn(
                                INVOKEVIRTUAL,
                                Type.getInternalName(MixinEventType.class),
                                "invoke",
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object[].class)),
                                false
                        ); // -> 0, 1

                        methodVisitor.visitInsn(RETURN);
                        methodVisitor.visitMaxs(0, 0);
                    }),
                    (methodVisitor) -> annotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, clazz)
            );

            List<String> paramNames = new ArrayList<>();
            locals.forEach((type) -> paramNames.add(AsmUtil.getWrappedTypeName(type)));
            return new MixinEventType(new Identifier(script.getId(), eventName), paramNames);

        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        }
    }

    private void writeMethod(
            VisitedElement visitedValue,
            int accessOverride,
            String name,
            List<Type> params,
            Type returnType,
            List<MethodWriteFactory> methodFactories,
            AnnotationFactory annotationFactory
    ) throws LuaError, InvalidArgumentException, InvalidMixinException {
        var desc = Type.getMethodDescriptor(returnType, params.toArray(Type[]::new));
        var methodVisitor = c.visitMethod(
                accessOverride | (visitedValue.access() & ACC_STATIC),
                name,
                desc,
                visitedValue instanceof VisitedMethod ? visitedValue.signature() : null,
                // I doubt this is necessary, but just in case.
                visitedValue instanceof VisitedMethod ? ((VisitedMethod) visitedValue).exceptions() : null
        );
        int thisVarOffset = (visitedValue.access() & ACC_STATIC) != 0 ? 0 : 1;

        annotationFactory.write(methodVisitor); // Apply annotations to this method
        if (!methodFactories.isEmpty()) {
            methodVisitor.visitCode();
            for (MethodWriteFactory mwf : methodFactories) {
                mwf.write(methodVisitor, desc, thisVarOffset);
            }
            methodVisitor.visitEnd();
        }
    }

    @LuaWrapped
    public MixinClassInfo build() {
        c.visitEnd();
        byte[] classBytes = c.toByteArray();
        AsmUtil.dumpClass(className, classBytes);

        // give the class back to the user for later use in the case of an interface.
        MixinClassInfo info = new MixinClassInfo(className, classBytes, asInterface);
        MIXINS.put(className + ".class", info);
        return info;
    }

    // Abuse of functional interfaces:

    @FunctionalInterface
    private interface VisitFieldInsn {
        void visit(int opcode, String owner, String name, String descriptor);
    }

    @FunctionalInterface
    private interface MethodWriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, int thisVarOffset) throws InvalidArgumentException, LuaError, InvalidMixinException;
    }

    @FunctionalInterface
    private interface AnnotationFactory {
        void write(MethodVisitor methodVisitor) throws InvalidArgumentException, LuaError, InvalidMixinException;
    }
}

