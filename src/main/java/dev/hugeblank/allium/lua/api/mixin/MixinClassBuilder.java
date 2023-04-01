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
import net.minecraft.util.Pair;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class MixinClassBuilder {
    public static final Map<String, byte[]> GENERATED_MIXIN_BYTES = new HashMap<>();
    public static final Map<String, List<SimpleEventType<?>>> GENERATED_EVENTS = new HashMap<>();
    protected static final Map<String, Map<String, VisitedMethod>> CLASS_VISITED_METHODS = new HashMap<>();

    private final String className = AsmUtil.getUniqueMixinClassName();
    private final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final List<Consumer<MethodVisitor>> clinit = new ArrayList<>();
    private final Script script;
    private final LuaState state;
    private final Map<String, VisitedMethod> visitedMethods;

    public MixinClassBuilder(String targetClass, Script script, @LuaStateArg LuaState state) throws IOException {
        targetClass = Allium.DEVELOPMENT ? targetClass : Allium.MAPPINGS.getYarn(targetClass);
        this.state = state;
        this.script = script;
        // Get the methods and fields of this class without static init-ing it.
        if (CLASS_VISITED_METHODS.containsKey(targetClass)) {
            this.visitedMethods = CLASS_VISITED_METHODS.get(targetClass);
        } else {
            this.visitedMethods = new HashMap<>();
            CLASS_VISITED_METHODS.put(targetClass, this.visitedMethods);

            ClassReader reader = new ClassReader(targetClass);
            String finalTargetClass = targetClass;
            reader.accept(new ClassVisitor(ASM9) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    String[] mapped = Allium.MAPPINGS.getYarn(Mappings.asMethod(Allium.MAPPINGS.getIntermediary(finalTargetClass).get(0), name)).split("#");
                    if (!Allium.DEVELOPMENT && mapped.length == 2) {
                        visitedMethods.put(name+descriptor, new VisitedMethod(access, mapped[1], descriptor, signature, exceptions));
                    } else { // Covers unmapped names
                        visitedMethods.put(name+descriptor, new VisitedMethod(access, name, descriptor, signature, exceptions));
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }, ClassReader.SKIP_FRAMES);
        }
        GENERATED_EVENTS.put(className, new ArrayList<>());
        EClass<?> superClass = EClass.fromJava(Object.class);
        this.c.visit(
                V17,
                ACC_PUBLIC,
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
        return writeInjectable(eventName, annotations, EClass.fromJava(Inject.class));
    }

    @LuaWrapped
    public SimpleEventType<EventInvoker> redirect(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException {
        return writeInjectable(eventName, annotations, EClass.fromJava(Redirect.class));
    }

    public SimpleEventType<EventInvoker> writeInjectable(String eventName, LuaTable annotations, EClass<?> clazz) throws LuaError, InvalidMixinException, InvalidArgumentException {
        String descriptor = annotations.rawget("method").checkString();
        if (visitedMethods.containsKey(descriptor)) {
            VisitedMethod visitedMethod = visitedMethods.get(descriptor);
            List<Type> params = new ArrayList<>(List.of(Type.getArgumentTypes(visitedMethod.descriptor())));
            params.add(Type.getType(CallbackInfo.class));

            SimpleEventType<EventInvoker> eventType = new SimpleEventType<>(new Identifier(script.getId(), eventName));
            GENERATED_EVENTS.get(className).add(eventType);

            this.writeMethod(
                    visitedMethod,
                    params,
                    (methodVisitor) -> LuaMixinAnnotation.annotateMethod(state, annotations, methodVisitor, clazz)
            );
            return eventType;
        }
        throw new InvalidMixinException(InvalidMixinException.Type.INVALID_METHOD, descriptor);
    }

    private Pair<String, Class<?>> writeEventInterface(List<Type> params) { // TODO: This can probably be converted to accessor/invoker.
        String className = AsmUtil.getUniqueClassName();
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classWriter.visit(
                V17,
                ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE,
                className,
                null,
                "java/lang/Object",
                new String[]{Type.getInternalName(EventInvoker.class)}
        );
        var desc = Type.getMethodDescriptor(Type.VOID_TYPE, params.toArray(Type[]::new));
        classWriter.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "invoke", desc, null, null);
        byte[] classBytes = classWriter.toByteArray();
        //GENERATED_EVENT_BYTES.put(className + ".class", classBytes);
        return new Pair<>(className, AsmUtil.defineClass(className, classBytes));
    }

    private void writeMethod(VisitedMethod visitedMethod, List<Type> params, AnnotationFactory factory) throws LuaError, InvalidArgumentException, InvalidMixinException {

        var desc = Type.getMethodDescriptor(Type.VOID_TYPE, params.toArray(Type[]::new));
        var methodVisitor = c.visitMethod(ACC_PRIVATE | (visitedMethod.access() & ACC_STATIC), visitedMethod.name(), desc, visitedMethod.signature(), null);
        int thisVarOffset = (visitedMethod.access() & ACC_STATIC) != 0 ? 0 : 1;
        int varPrefix = (Type.getArgumentsAndReturnSizes(desc) >> 2)-(1-thisVarOffset);

        factory.write(methodVisitor); // Apply annotations to this method

        methodVisitor.visitCode();

        // Write all the stuff needed to get the EventInvoker from the mixin.
        int ind = GENERATED_EVENTS.get(className).size();
        clinit.add((mv) -> { // Add
            mv.visitFieldInsn(GETSTATIC, Type.getInternalName(MixinClassBuilder.class), "GENERATED_EVENTS", Type.getDescriptor(Map.class));
            mv.visitLdcInsn(className);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Map.class), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(List.class));
            mv.visitLdcInsn(ind-1);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(List.class), "get", "(I)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(SimpleEventType.class));

            mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class));
        });
        FieldVisitor fv = c.visitField(ACC_PRIVATE | ACC_STATIC, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class), null, null);
        var a = fv.visitAnnotation(ClassFieldBuilder.GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("value", SimpleEventType.class.getName());
        a.visitEnd();
        fv.visitEnd();

        methodVisitor.visitFieldInsn(GETSTATIC, className, "allium$simpleEventType"+ind, Type.getDescriptor(SimpleEventType.class));
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(SimpleEventType.class), "invoker", "()Ljava/lang/Object;", false);
        methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(EventInvoker.class));
        // eventType.invoker()

        var args = Type.getArgumentTypes(desc);
        methodVisitor.visitLdcInsn(args.length-thisVarOffset+1);
        methodVisitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
        methodVisitor.visitVarInsn(ASTORE, varPrefix);

        for (int i = thisVarOffset; i < args.length+thisVarOffset; i++) {
            int index = i-thisVarOffset;
            ProxyGenerator.loadAndWrapLocal(methodVisitor, varPrefix, args, i, index);
            methodVisitor.visitInsn(AASTORE);
        }
        methodVisitor.visitVarInsn(ALOAD, varPrefix);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(EventInvoker.class), "invoke", Type.getMethodDescriptor(EventInvoker.class.getMethods()[0]), true);
        // eventInvoker.invoke(...)

        methodVisitor.visitInsn(RETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    @LuaWrapped
    public void build() {
        MethodVisitor clinit = c.visitMethod(ACC_STATIC ,"<clinit>", "()V", null, null);
        this.clinit.forEach((consumer) -> consumer.accept(clinit));
        clinit.visitInsn(RETURN);
        clinit.visitMaxs(0,0);
        clinit.visitEnd();

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
    }

    @FunctionalInterface
    private interface AnnotationFactory {
        void write(MethodVisitor methodVisitor) throws InvalidArgumentException, LuaError, InvalidMixinException;
    }
}

