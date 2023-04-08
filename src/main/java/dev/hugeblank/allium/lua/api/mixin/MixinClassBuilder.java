package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.event.MixinEventType;
import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.InvalidMixinException;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.*;
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

import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/* Notes on implementation:
 - Accessors & Invokers MUST have target name set as the value in the annotation. We use that to determine which
 method/field to invoke/access
*/

@LuaWrapped
public class MixinClassBuilder {
    public static final Map<String, MixinClassInfo> MIXINS = new HashMap<>();

    protected static final Map<String, Map<String, VisitedMethod>> CLASS_VISITED_METHODS = new HashMap<>();
    protected static final Map<String, Map<String, VisitedField>> CLASS_VISITED_FIELDS = new HashMap<>();

    private final String className = AsmUtil.getUniqueMixinClassName();
    private final String targetClass;
    private final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private int methodIndex = 0;
    private final Script script;
    private final boolean asInterface;
    private final Map<String, VisitedMethod> visitedMethods;
    private final Map<String, VisitedField> visitedFields;

    public MixinClassBuilder(String targetClass, boolean asInterface, Script script) throws IOException {
        targetClass = Allium.DEVELOPMENT ? targetClass : Allium.MAPPINGS.getYarn(targetClass);
        this.targetClass = targetClass;
        this.script = script;
        this.asInterface = asInterface;
        // Get the methods and fields of this class without static init-ing it.
        if (CLASS_VISITED_METHODS.containsKey(targetClass)) {
            this.visitedMethods = CLASS_VISITED_METHODS.get(targetClass);
            this.visitedFields = CLASS_VISITED_FIELDS.get(targetClass);
        } else {
            this.visitedMethods = new HashMap<>();
            this.visitedFields = new HashMap<>();
            CLASS_VISITED_METHODS.put(targetClass, this.visitedMethods);
            CLASS_VISITED_FIELDS.put(targetClass, this.visitedFields);

            ClassReader reader = new ClassReader(targetClass);
            String finalTargetClass = targetClass;
            reader.accept(new ClassVisitor(ASM9) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    String[] mapped = Allium.MAPPINGS.getYarn(Mappings.asMethod(Allium.MAPPINGS.getIntermediary(finalTargetClass).get(0), name)).split("#");
                    if (!Allium.DEVELOPMENT && mapped.length == 2) {
                        visitedFields.put(name, new VisitedField(access, mapped[1], descriptor, signature, value));
                    } else { // Covers unmapped names
                        visitedFields.put(name, new VisitedField(access, name, descriptor, signature, value));
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    String[] mapped = Allium.MAPPINGS.getYarn(Mappings.asMethod(Allium.MAPPINGS.getIntermediary(finalTargetClass).get(0), name)).split("#");
                    if (!Allium.DEVELOPMENT && mapped.length == 2) {
                        String key = name;
                        if (!name.equals("<init>") && !name.equals("<clinit>")) {
                            key = key+descriptor;
                        }
                        visitedMethods.put(key, new VisitedMethod(access, mapped[1], descriptor, signature, exceptions));
                    } else { // Covers unmapped names
                        String key = name;
                        if (!name.equals("<init>") && !name.equals("<clinit>")) {
                            key = key+descriptor;
                        }
                        visitedMethods.put(key, new VisitedMethod(access, name, descriptor, signature, exceptions));
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }


            }, ClassReader.SKIP_FRAMES);
        }
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
        aa.visit(null, Type.getObjectType(targetClass));
        aa.visitEnd();
        ma.visitEnd();
    }

    @LuaWrapped
    public MixinEventType inject(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        return writeInject(eventName, annotations, EClass.fromJava(Inject.class));
    }

    @LuaWrapped
    public void redirect(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        // TODO: This doesn't actually work.
        // TODO: instanceOf mode
        // TODO: If LocalCapture, then parse bytecode for arguments.
        writeInject(eventName, annotations, EClass.fromJava(Redirect.class));
    }

    // There's some disadvantages to this system. All shadowed values are made public, and forced to be modifiable.
    // Generally that's what's desired so can ya fault me for doing it this way?
    // TODO: test
    @LuaWrapped
    public void shadow(String target) {
        if (visitedFields.containsKey(target)) {
            VisitedField visitedField = visitedFields.get(target);
            FieldVisitor fieldVisitor = c.visitField(
                    visitedField.access() | ACC_PUBLIC,
                    visitedField.descriptor(),
                    visitedField.name(),
                    visitedField.signature(),
                    null
            );
            //noinspection DuplicatedCode
            AnnotationHelper.attachAnnotation(fieldVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedField.access() & ACC_FINAL) != 0) {
                AnnotationHelper.attachAnnotation(fieldVisitor, Final.class).visitEnd();
            }
            if ((visitedField.access() & ACC_SYNTHETIC) != 0) {
                AnnotationHelper.attachAnnotation(fieldVisitor, Dynamic.class).visitEnd();
            }
            fieldVisitor.visitEnd();
        } else if (visitedMethods.containsKey(target)) {
            VisitedMethod visitedMethod = visitedMethods.get(target);
            MethodVisitor methodVisitor = c.visitMethod(
                    visitedMethod.access() | ACC_PUBLIC | ACC_ABSTRACT,
                    visitedMethod.descriptor(),
                    visitedMethod.name(),
                    visitedMethod.signature(),
                    null
            );
            //noinspection DuplicatedCode
            AnnotationHelper.attachAnnotation(methodVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedMethod.access() & ACC_FINAL) != 0) {
                AnnotationHelper.attachAnnotation(methodVisitor, Final.class).visitEnd();
            }
            if ((visitedMethod.access() & ACC_SYNTHETIC) != 0) {
                AnnotationHelper.attachAnnotation(methodVisitor, Dynamic.class).visitEnd();
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
        if (visitedMethods.containsKey(methodName)) {
            VisitedMethod visitedMethod = visitedMethods.get(methodName);
            this.writeMethod(
                    visitedMethod,
                    ((visitedMethod.access() & ACC_STATIC) != 0) ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    "invoke" +
                            visitedMethod.name().substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                            visitedMethod.name().substring(1), // Rest of name
                    List.of(Type.getArgumentTypes(visitedMethod.descriptor())),
                    Type.getReturnType(visitedMethod.descriptor()),
                    ((visitedMethod.access() & ACC_STATIC) != 0) ? List.of((visitor, desc, offset) -> {
                        AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(AssertionError.class), "()V").run();
                        visitor.visitInsn(ATHROW);
                        visitor.visitMaxs(0,0);
                    }) : List.of(),
                    (methodVisitor) -> AnnotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, EClass.fromJava(Invoker.class))
            );
        }
    }

    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String fieldName = getTargetValue(annotations);
        if (visitedFields.containsKey(fieldName)) {
            VisitedField visitedField = visitedFields.get(fieldName);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            this.writeMethod(
                    visitedField,
                    ((visitedField.access() & ACC_STATIC) != 0) ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    (isSetter ? "set" : "get") + // set or get
                            visitedField.name().substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                            visitedField.name().substring(1), // Rest of name
                    isSetter ? List.of(visitedFieldType) : List.of(),
                    isSetter ? Type.VOID_TYPE : visitedFieldType,
                    ((visitedField.access() & ACC_STATIC) != 0) ? List.of((visitor, desc, offset) -> {
                        AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(UnsupportedOperationException.class), "()V").run();
                        visitor.visitInsn(ATHROW);
                        visitor.visitMaxs(0,0);
                    }) : List.of(),
                    (methodVisitor) -> AnnotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, EClass.fromJava(Accessor.class))
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
        if (visitedMethods.containsKey(descriptor)) {
            VisitedMethod visitedMethod = visitedMethods.get(descriptor);
            List<Type> params = new ArrayList<>(List.of(Type.getArgumentTypes(visitedMethod.descriptor())));
            params.add(Type.getType(CallbackInfo.class));
            List<Type> locals = new ArrayList<>(params);
            if ((visitedMethod.access() & ACC_STATIC) == 0) {
                locals.add(0, Type.getType("L"+targetClass+";"));
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
                    (methodVisitor) -> AnnotationHelper.annotateMethod(script.getExecutor().getState(), annotations, methodVisitor, clazz)
            );

            List<String> paramNames = new ArrayList<>();
            locals.forEach((type) -> paramNames.add(AsmUtil.getWrappedTypeName(type)));
            return new MixinEventType(new Identifier(script.getId(), eventName), paramNames);

        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        }
    }

    private void writeMethod(
            VisitedValue visitedValue,
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

