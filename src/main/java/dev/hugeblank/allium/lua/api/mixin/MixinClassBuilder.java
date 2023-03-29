package dev.hugeblank.allium.lua.api.mixin;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.api.ClassBuilder;
import dev.hugeblank.allium.lua.event.SimpleEventType;
import dev.hugeblank.allium.lua.type.InvalidArgumentException;
import dev.hugeblank.allium.lua.type.InvalidMixinException;
import dev.hugeblank.allium.lua.type.TypeCoercions;
import dev.hugeblank.allium.lua.type.annotation.LuaStateArg;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.AsmUtil;
import dev.hugeblank.allium.util.EventInvoker;
import dev.hugeblank.allium.util.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class MixinClassBuilder extends ClassBuilder {
    private final Script script;
    private final String targetClass;
    protected static final Map<String, Map<String, VisitedMethod>> CLASS_VISITED_METHODS = new HashMap<>();
    protected final Map<String, VisitedMethod> visitedMethods;


    public MixinClassBuilder(String targetClass, Script script, @LuaStateArg LuaState state) throws IOException {
        super(EClass.fromJava(Object.class), List.of(), (ClassWriter template) -> {
            AnnotationVisitor ma = template.visitAnnotation(Mixin.class.descriptorString(), true);
            AnnotationVisitor aa = ma.visitArray("value");
            aa.visit(null, Type.getObjectType(targetClass));
            aa.visitEnd();
            ma.visitEnd();
        }, state);

        this.script = script;
        this.targetClass = targetClass;
        // Get the methods and fields of this class without static init-ing it.
        if (CLASS_VISITED_METHODS.containsKey(targetClass)) {
            this.visitedMethods = CLASS_VISITED_METHODS.get(targetClass);
        } else {
            this.visitedMethods = new HashMap<>();
            CLASS_VISITED_METHODS.put(targetClass, this.visitedMethods);

            ClassReader reader = new ClassReader(targetClass);
            reader.accept(new ClassVisitor(ASM9) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    visitedMethods.put(name+descriptor, new VisitedMethod(access, name, descriptor, signature, exceptions));
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }, ClassReader.SKIP_FRAMES);
        }
    }

    @LuaWrapped
    public SimpleEventType<?> inject(String eventName, LuaTable annotations) throws LuaError, InvalidMixinException, InvalidArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String descriptor = annotations.rawget("method").checkString();
        if (visitedMethods.containsKey(descriptor)) {
            VisitedMethod visitedMethod = visitedMethods.get(descriptor);
            List<Type> params = new ArrayList<>(List.of(Type.getArgumentTypes(visitedMethod.descriptor())));
            params.add(Type.getType(CallbackInfo.class));

            Pair<String, Class<?>> eventInvoker = this.writeEventInterface(params);
            SimpleEventType<? extends EventInvoker> eventType = new SimpleEventType<>(new Identifier(script.getId(), eventName));

            this.writeInjectMethod(
                    visitedMethod,
                    params,
                    ACC_PRIVATE | (visitedMethod.access() & ACC_STATIC),
                    eventType,
                    eventInvoker,
                    annotations
            );
            return eventType;
        }
        throw new InvalidMixinException(InvalidMixinException.Type.INVALID_METHOD, descriptor);
    }

    private Pair<String, Class<?>> writeEventInterface(List<Type> params) {
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
        return new Pair<>(className, AsmUtil.defineClass(className, classBytes));
    }

    private void writeInjectMethod(VisitedMethod visitedMethod, List<Type> params, int access, SimpleEventType<?> eventType, Pair<String, Class<?>> eventInvoker, LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException, NoSuchMethodException {
        var isStatic = (access & ACC_STATIC) != 0;

        var desc = Type.getMethodDescriptor(Type.VOID_TYPE, params.toArray(Type[]::new));
        var methodVisitor = c.visitMethod(access, visitedMethod.name(), desc, visitedMethod.signature(), null);
        int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;
        int thisVarOffset = isStatic ? 0 : 1;
        if (isStatic) varPrefix -= 1;

        AnnotationVisitor injectAnnotationVisitor = methodVisitor.visitAnnotation(Inject.class.descriptorString(), true);

        // method
        AnnotationVisitor methodArrayAnnotationVisitor = injectAnnotationVisitor.visitArray("method");
        methodArrayAnnotationVisitor.visit("method", annotations.rawget("method").checkString());
        methodArrayAnnotationVisitor.visitEnd();

        // cancellable
        if (annotations.rawget("cancellable").isBoolean()) {
            injectAnnotationVisitor.visit("cancellable", annotations.rawget("cancellable").checkBoolean());
        }

        // localcapture
        // TODO FIX
        if (annotations.rawget("locals").isUserdata()) {
            injectAnnotationVisitor.visit("locals", TypeCoercions.toJava(this.state, annotations.rawget("locals"), LocalCapture.class));
        }

        // at
        LuaTable target = annotations.rawget("at").checkTable();
        AnnotationVisitor atArrayAnnotationVisitor = injectAnnotationVisitor.visitArray("at");
        AnnotationVisitor atAnnotationVisitor = atArrayAnnotationVisitor.visitAnnotation(null, At.class.descriptorString());
        if (target.rawget(1).isString()) {
            atAnnotationVisitor.visit("value", target.rawget(1).checkString());
        } else if (target.rawget("value").isString()) {
            atAnnotationVisitor.visit("value", target.rawget("value").checkString());
        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.MISSING_TARGET, null);
        }
        if (target.rawget("target").isString()) {
            atAnnotationVisitor.visit("target", target.rawget("target").checkString());
        }

        atAnnotationVisitor.visitEnd();
        atArrayAnnotationVisitor.visitEnd();
        injectAnnotationVisitor.visitEnd();

        methodVisitor.visitCode();

        fields.storeAndGet(methodVisitor, eventType, SimpleEventType.class); // Store then get the event type
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(SimpleEventType.class), "invoker", "()L"+eventInvoker.getLeft() + ";", false);
        // eventType.invoker()
        var args = Type.getArgumentTypes(desc);
        for (int i = thisVarOffset; i < args.length+thisVarOffset; i++) {
            methodVisitor.visitVarInsn(ALOAD, i);
        }

        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, eventInvoker.getLeft(), "invoke", desc, false);
        // eventInvoker.invoke(...)

        methodVisitor.visitInsn(RETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }
}

