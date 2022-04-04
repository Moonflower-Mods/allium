package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import me.hugeblank.allium.lua.type.TypeCoercions;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import me.hugeblank.allium.lua.type.property.PropertyResolver;
import me.hugeblank.allium.util.AsmUtil;
import me.hugeblank.allium.util.ClassFieldBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder {
    protected final EClass<?> superClass;
    protected final String className;
    protected final LuaState state;
    protected final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final List<EMethod> methods = new ArrayList<>();
    private final ClassFieldBuilder fields;

    @LuaWrapped
    public ClassBuilder(EClass<?> superClass, List<EClass<?>> interfaces, LuaState state) {
        this.state = state;
        this.className = AsmUtil.getUniqueClassName();
        this.fields = new ClassFieldBuilder(className, c);

        this.c.visit(
                V17,
                ACC_PUBLIC,
                className,
                null,
                superClass.name().replace('.', '/'),
                interfaces.stream().map(x -> x.name().replace('.', '/')).toArray(String[]::new)
        );

        for (EConstructor<?> superCtor : superClass.constructors()) {
            if (!superCtor.isPublic()) continue;

            var desc = Type.getConstructorDescriptor(superCtor.raw());
            var m = c.visitMethod(superCtor.modifiers(), "<init>", desc, null, null);
            m.visitCode();
            var args = Type.getArgumentTypes(desc);

            m.visitVarInsn(ALOAD, 0);

            int argIndex = 1;

            for (Type arg : args) {
                m.visitVarInsn(arg.getOpcode(ILOAD), argIndex);

                argIndex += arg.getSize();
            }

            m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass.raw()), "<init>", desc, false);
            m.visitInsn(RETURN);

            m.visitMaxs(0, 0);
            m.visitEnd();
        }

        this.superClass = superClass;
        this.methods.addAll(this.superClass.methods());
        for (var inrf : interfaces) {
            this.methods.addAll(inrf.methods());
        }
    }

    @LuaWrapped
    public void overrideMethod(String methodName, EClass<?>[] parameters, LuaFunction func) {
        var methods = new ArrayList<EMethod>();

        PropertyResolver.collectMethods(this.superClass, this.methods, methodName, false, methods::add);

        for (var method : methods) {
            var methParams = method.parameters();

            if (methParams.size() == parameters.length) {
                boolean match = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (!methParams.get(i).parameterType().equals(parameters[i])) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    writeMethod(
                        method.name(),
                        methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                        new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
                        method.modifiers() & ~ACC_ABSTRACT,
                        func
                    );

                    return;
                }
            }
        }
    }

    @LuaWrapped
    public void createMethod(String methodName, EClass<?>[] params, EClass<?> returnClass, boolean isStatic, LuaFunction func) {
        writeMethod(
            methodName,
            Arrays.stream(params).map(x -> new WrappedType(x, x)).toArray(WrappedType[]::new),
            returnClass == null ? null : new WrappedType(returnClass, returnClass),
            isStatic ? (ACC_PUBLIC | ACC_STATIC) : ACC_PUBLIC,
            func
        );

    }

    private void writeMethod(String methodName, WrappedType[] params, WrappedType returnClass, int access, LuaFunction func) {
        var paramsType = Arrays.stream(params).map(x -> x.raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        var returnType = returnClass == null ? Type.VOID_TYPE : Type.getType(returnClass.raw.raw());
        var isStatic = (access & ACC_STATIC) != 0;

        var desc = Type.getMethodDescriptor(returnType, paramsType);
        var m = c.visitMethod(access, methodName, desc, null, null);
        int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;

        m.visitCode();

        if (isStatic) varPrefix -= 1;

        m.visitLdcInsn(params.length + 1);
        m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
        m.visitVarInsn(ASTORE, varPrefix);

        if (!isStatic) {
            m.visitVarInsn(ALOAD, varPrefix);
            m.visitLdcInsn(0);
            m.visitVarInsn(ALOAD, 0);
            fields.storeAndGetComplex(m, EClass::fromJava, EClass.class, className);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitInsn(AASTORE);
        }

        int argIndex = 1;
        var args = Type.getArgumentTypes(desc);
        for (int i = 0; i < args.length; i++) {
            m.visitVarInsn(ALOAD, varPrefix);
            m.visitLdcInsn(i + 1);
            m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex);

            if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                AsmUtil.wrapPrimitive(m, args[i]);
            }

            fields.storeAndGet(m, params[i].real.wrapPrimitive(), EClass.class);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitInsn(AASTORE);

            argIndex += args[i].getSize();
        }

        var isVoid = returnClass == null || returnType.getSort() == Type.VOID;

        fields.storeAndGet(m, state, LuaState.class);
        if (!isVoid) m.visitInsn(DUP);
        fields.storeAndGet(m, func, LuaFunction.class);
        m.visitInsn(SWAP);
        m.visitVarInsn(ALOAD, varPrefix);
        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false);
        m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(LuaFunction.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false);

        if (!isVoid) {
            m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false);
            fields.storeAndGet(m, returnClass.real.wrapPrimitive(), EClass.class);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false);
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(returnClass.real.wrapPrimitive().raw()));

            if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                AsmUtil.unwrapPrimitive(m, returnType);
            } else {
                m.visitTypeInsn(CHECKCAST, returnType.getInternalName());
            }
        }

        m.visitInsn(returnType.getOpcode(IRETURN));

        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    public byte[] getByteArray() {
        return c.toByteArray();
    }

    @LuaWrapped
    public LuaValue build() {
        byte[] classBytes = c.toByteArray();

        Class<?> klass = AsmUtil.defineClass(className, classBytes);

        fields.apply(klass);

        return JavaLib.importClass(EClass.fromJava(klass));
    }

    public String getName() {
        return this.className;
    }

    private record WrappedType(EClass<?> raw, EClass<?> real) {
        public static WrappedType fromParameter(EParameter param) {
            return new WrappedType(param.rawParameterType(), param.parameterType().lowerBound());
        }
    }
}
