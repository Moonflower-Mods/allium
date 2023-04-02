package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.event.SimpleEventType;
import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.InvalidMixinException;
import dev.hugeblank.allium.lua.type.ProxyGenerator;
import dev.hugeblank.allium.lua.type.annotation.LuaStateArg;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class MixinClassBuilder {
    public static final Map<String, byte[]> GENERATED_MIXIN_BYTES = new HashMap<>();
    public static final Map<String, List<SimpleEventType<?>>> GENERATED_EVENTS = new HashMap<>();
    protected static final Map<String, Map<String, VisitedMethod>> CLASS_VISITED_METHODS = new HashMap<>();
    protected static final Map<String, Map<String, VisitedField>> CLASS_VISITED_FIELDS = new HashMap<>();

    private final String className = AsmUtil.getUniqueMixinClassName();
    private final String targetClass;
    private final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final List<Consumer<MethodVisitor>> fields = new ArrayList<>();
    private final Script script;
    private final LuaState state;
    private final boolean asInterface;
    private final Map<String, VisitedMethod> visitedMethods;
    private final Map<String, VisitedField> visitedFields;

    public MixinClassBuilder(String targetClass, Script script, boolean asInterface, @LuaStateArg LuaState state) throws IOException {
        targetClass = Allium.DEVELOPMENT ? targetClass : Allium.MAPPINGS.getYarn(targetClass);
        this.targetClass = targetClass;
        this.state = state;
        this.asInterface = asInterface;
        this.script = script;
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
        GENERATED_EVENTS.put(className, new ArrayList<>());
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
    public SimpleEventType<EventInvoker> inject(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        return writeInjectable(eventName, annotations, EClass.fromJava(Inject.class));
    }

    @LuaWrapped
    public SimpleEventType<EventInvoker> redirect(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        // TODO: This doesn't actually work.
        // TODO: instanceOf mode
        // TODO: If LocalCapture, then parse bytecode for arguments.
        return writeInjectable(eventName, annotations, EClass.fromJava(Redirect.class));
    }

    // There's some disadvantages to this system. All shadowed values are made public, and forced to be modifiable.
    // Generally that's what's desired so can ya fault me for doing it this way?
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
            AnnotationHelpers.attachAnnotation(fieldVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedField.access() & ACC_FINAL) != 0) {
                AnnotationHelpers.attachAnnotation(fieldVisitor, Final.class).visitEnd();
            }
            if ((visitedField.access() & ACC_SYNTHETIC) != 0) {
                AnnotationHelpers.attachAnnotation(fieldVisitor, Dynamic.class).visitEnd();
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
            AnnotationHelpers.attachAnnotation(methodVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedMethod.access() & ACC_FINAL) != 0) {
                AnnotationHelpers.attachAnnotation(methodVisitor, Final.class).visitEnd();
            }
            if ((visitedMethod.access() & ACC_SYNTHETIC) != 0) {
                AnnotationHelpers.attachAnnotation(methodVisitor, Dynamic.class).visitEnd();
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

    // TODO: throw new UnsupportedOperationException() in static Accessor methods.
    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        if (!asInterface) throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "interface");
        String fieldName = null;
        if (annotations.rawget("value").isString()) {
            fieldName = annotations.rawget("value").checkString();
        } else if (annotations.rawget(1).isString()) {
            fieldName = annotations.rawget(1).checkString();
        }
        if (fieldName == null) throw new InvalidArgumentException("Expected field name at key 'value' or index 1");
        if (visitedFields.containsKey(fieldName)) {
            VisitedField visitedField = visitedFields.get(fieldName);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            this.writeMethod(
                    visitedField,
                    ((visitedField.access() & ACC_STATIC) != 0) ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    (isSetter ? "set" : "get") + // set or get
                            visitedField.name().substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                            visitedField.name().substring(1), // Rest of field name
                    isSetter ? List.of(visitedFieldType) : List.of(),
                    isSetter ? Type.VOID_TYPE : visitedFieldType,
                    List.of((methodVisitor, desc, thisVarOffset) -> methodVisitor.visitEnd()),
                    (methodVisitor) -> AnnotationHelpers.annotateMethod(state, annotations, methodVisitor, EClass.fromJava(Accessor.class))
            );
        }
    }


    private SimpleEventType<EventInvoker> writeInjectable(String eventName, LuaTable annotations, EClass<?> clazz) throws LuaError, InvalidMixinException, InvalidArgumentException {
        String descriptor = annotations.rawget("method").checkString();
        if (visitedMethods.containsKey(descriptor)) {
            VisitedMethod visitedMethod = visitedMethods.get(descriptor);
            List<Type> params = new ArrayList<>(List.of(Type.getArgumentTypes(visitedMethod.descriptor())));
            params.add(Type.getType(CallbackInfo.class));

            SimpleEventType<EventInvoker> eventType = new SimpleEventType<>(new Identifier(script.getId(), eventName));
            GENERATED_EVENTS.get(className).add(eventType);

            this.writeMethod(
                    visitedMethod,
                    ACC_PRIVATE,
                    visitedMethod.name()+GENERATED_EVENTS.get(className).size(),
                    params,
                    Type.VOID_TYPE,
                    List.of((methodVisitor, desc, thisVarOffset) -> {
                        int ind = GENERATED_EVENTS.get(className).size();
                        int varPrefix = (Type.getArgumentsAndReturnSizes(desc) >> 2)-(1-thisVarOffset);
                        // Write all the stuff needed to get the EventInvoker from the mixin.
                        fields.add((clinit) -> { // Add
                            clinit.visitFieldInsn(GETSTATIC, Type.getInternalName(MixinClassBuilder.class), "GENERATED_EVENTS", Type.getDescriptor(Map.class));
                            clinit.visitLdcInsn(className);
                            clinit.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                            clinit.visitTypeInsn(CHECKCAST, Type.getInternalName(List.class));
                            clinit.visitLdcInsn(ind-1);
                            clinit.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(List.class), "get", "(I)Ljava/lang/Object;", true);
                            clinit.visitTypeInsn(CHECKCAST, Type.getInternalName(SimpleEventType.class));

                            clinit.visitFieldInsn(Opcodes.PUTSTATIC, className, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class));
                        });
                        FieldVisitor fv = c.visitField(ACC_PRIVATE | ACC_STATIC, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class), null, null);
                        fv.visitEnd();

                        methodVisitor.visitFieldInsn(GETSTATIC, className, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class));
                        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(SimpleEventType.class), "invoker", "()Ljava/lang/Object;", false);
                        methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(EventInvoker.class));
                        // eventType.invoker()

                        var args = Type.getArgumentTypes(desc);
                        methodVisitor.visitLdcInsn(args.length+thisVarOffset);
                        methodVisitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
                        methodVisitor.visitVarInsn(ASTORE, varPrefix);

                        if (thisVarOffset == 1) {
                            ProxyGenerator.loadAndWrapLocal(methodVisitor, varPrefix, new Type[]{Type.getType("L"+ targetClass +";")}, 0, 0, 0);
                            methodVisitor.visitInsn(AASTORE);
                        }
                        for (int i = 0; i < args.length; i++) { // TODO: Fix to handle double sized values.
                            int index = i+thisVarOffset;
                            ProxyGenerator.loadAndWrapLocal(methodVisitor, varPrefix, args, index, i, index);
                            methodVisitor.visitInsn(AASTORE);
                        }
                        methodVisitor.visitVarInsn(ALOAD, varPrefix);
                        methodVisitor.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(EventInvoker.class), "invoke", Type.getMethodDescriptor(EventInvoker.class.getMethods()[0]), true);
                        // eventInvoker.invoke(...)

                        methodVisitor.visitInsn(RETURN);
                        methodVisitor.visitMaxs(0, 0);
                    }),
                    (methodVisitor) -> AnnotationHelpers.annotateMethod(state, annotations, methodVisitor, clazz)
            );
            return eventType;
        }
        throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
    }

    private void writeMethod(VisitedValue visitedValue, int access, String name, List<Type> params, Type returnType, List<MethodWriteFactory> methodFactories, AnnotationFactory annotationFactory) throws LuaError, InvalidArgumentException, InvalidMixinException {
        var desc = Type.getMethodDescriptor(returnType, params.toArray(Type[]::new));
        var methodVisitor = c.visitMethod(
                access | (visitedValue.access() & ACC_STATIC),
                name,
                desc,
                visitedValue.signature(),
                // I doubt this is necessary, but just in case.
                visitedValue instanceof VisitedMethod ? ((VisitedMethod) visitedValue).exceptions() : null
        );
        int thisVarOffset = (visitedValue.access() & ACC_STATIC) != 0 ? 0 : 1;

        annotationFactory.write(methodVisitor); // Apply annotations to this method
        methodVisitor.visitCode();
        for (MethodWriteFactory mwf : methodFactories) {
            mwf.write(methodVisitor, desc, thisVarOffset);
        }
        methodVisitor.visitEnd();
    }

    @LuaWrapped
    public MixinClassHolder build() {
        if (!this.fields.isEmpty()) {
            MethodVisitor clinit = c.visitMethod(ACC_STATIC ,"<clinit>", "()V", null, null);
            this.fields.forEach((consumer) -> consumer.accept(clinit));
            clinit.visitInsn(RETURN);
            clinit.visitMaxs(0,0);
            clinit.visitEnd();
        }

        byte[] classBytes = c.toByteArray();
        if (Allium.DEVELOPMENT) {
            Path classPath = Allium.DUMP_DIRECTORY.resolve(className + ".class");
            try {
                Files.createDirectories(classPath.getParent());
                Files.write(classPath, classBytes);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't dump class", e);
            }
        }

        GENERATED_MIXIN_BYTES.put(className + ".class", classBytes);
        // give the class back to the user for later use in the case of an interface.

        return new MixinClassHolder(className.replace("/", "."));
    }

    public static void cleanup() {
        GENERATED_MIXIN_BYTES.clear();
        CLASS_VISITED_METHODS.clear();
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

