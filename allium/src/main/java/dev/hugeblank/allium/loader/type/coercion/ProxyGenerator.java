package dev.hugeblank.allium.loader.type.coercion;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import dev.hugeblank.allium.util.AsmUtil;
import dev.hugeblank.allium.util.ClassFieldBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.RETURN;

public class ProxyGenerator {
    private static final String STATE_FIELD_NAME = "allium$luaState";
    private static final String FUNCTION_FIELD_NAME = "allium$luaFunction";
    private static final String INIT_DESCRIPTOR = "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/function/LuaFunction;)V";

    private static final Map<EClass<?>, BiFunction<LuaState, LuaFunction, Object>> PROXIES = new HashMap<>();

    public static BiFunction<LuaState, LuaFunction, Object> getProxyFactory(EClass<?> klass, EMethod implMethod) {
        var factory = PROXIES.get(klass);

        if (factory == null) {
            factory = generateProxyClass(klass, implMethod);
            PROXIES.put(klass, factory);
        }

        return factory;
    }

    @SuppressWarnings("unchecked")
    private static BiFunction<LuaState, LuaFunction, Object> generateProxyClass(EClass<?> iface, EMethod method) {
        String className = AsmUtil.getUniqueClassName();
        ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassFieldBuilder fields = new ClassFieldBuilder(className, c);

        c.visit(
            V17,
            ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            new String[] {iface.name().replace('.', '/')}
        );

        c.visitField(ACC_PRIVATE | ACC_FINAL, STATE_FIELD_NAME, "Lorg/squiddev/cobalt/LuaState;", null, null);
        c.visitField(ACC_PRIVATE | ACC_FINAL, FUNCTION_FIELD_NAME, "Lorg/squiddev/cobalt/function/LuaFunction;", null, null);

        var ctor = c.visitMethod(ACC_PUBLIC, "<init>", INIT_DESCRIPTOR, null, null);
        ctor.visitCode();

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitFieldInsn(PUTFIELD, className, STATE_FIELD_NAME, "Lorg/squiddev/cobalt/LuaState;");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 2);
        ctor.visitFieldInsn(PUTFIELD, className, FUNCTION_FIELD_NAME, "Lorg/squiddev/cobalt/function/LuaFunction;");

        ctor.visitInsn(RETURN);

        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        var desc = Type.getMethodDescriptor(method.raw());
        int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;

        var m = c.visitMethod(ACC_PUBLIC, method.name(), desc, null, null);
        m.visitCode();
        m.visitLdcInsn(method.parameters().size());
        m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
        m.visitVarInsn(ASTORE, varPrefix);

        int argIndex = 1;
        var args = Type.getArgumentTypes(desc);
        for (int i = 0; i < args.length; i++) {
            m.visitVarInsn(ALOAD, varPrefix);
            m.visitLdcInsn(i);
            m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex);

            if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                AsmUtil.wrapPrimitive(m, args[i]);
            }

            fields.storeAndGet(m, method.parameters().get(i).parameterType().lowerBound().wrapPrimitive(), EClass.class);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitInsn(AASTORE);

            argIndex += args[i].getSize();
        }

        var ret = Type.getType(method.returnType().upperBound().raw());
        var isVoid = ret.getSort() == Type.VOID;

        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, className, STATE_FIELD_NAME, Type.getDescriptor(LuaState.class));
        if (!isVoid) m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, className, FUNCTION_FIELD_NAME, Type.getDescriptor(LuaFunction.class));
        m.visitInsn(SWAP);
        m.visitVarInsn(ALOAD, varPrefix);
        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false);
        m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(LuaFunction.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false);

        if (!isVoid) {
            m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false);
            fields.storeAndGet(m, method.returnType().upperBound().wrapPrimitive(), EClass.class);

            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false);
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(method.returnType().upperBound().wrapPrimitive().raw()));

            if (ret.getSort() != Type.ARRAY && ret.getSort() != Type.OBJECT) {
                AsmUtil.unwrapPrimitive(m, ret);
            }
        }

        m.visitInsn(ret.getOpcode(IRETURN));

        m.visitMaxs(0, 0);
        m.visitEnd();

        m = c.visitMethod(ACC_PUBLIC | ACC_STATIC, "getFactoryMethod", "()Ljava/util/function/BiFunction;", null, null);
        m.visitCode();
        m.visitInvokeDynamicInsn(
            "apply",
            "()Ljava/util/function/BiFunction;",
            AsmUtil.LAMBDA_METAFACTORY,
            Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            new Handle(H_NEWINVOKESPECIAL, className, "<init>", INIT_DESCRIPTOR, false),
            Type.getMethodType("(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/function/LuaFunction;)L" + className + ";")
        );
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();

        byte[] classBytes = c.toByteArray();
        Class<?> klass = AsmUtil.defineClass(className, classBytes);
        fields.apply(klass);

        try {
            return (BiFunction<LuaState, LuaFunction, Object>) klass.getMethod("getFactoryMethod").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Couldn't get factory method", e);
        }
    }
}
