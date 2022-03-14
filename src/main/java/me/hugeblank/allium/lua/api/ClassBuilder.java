package me.hugeblank.allium.lua.api;

import com.google.common.primitives.Primitives;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.UserdataFactory;
import me.hugeblank.allium.util.AsmUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class ClassBuilder {
    public static int id = 0;

    protected final Class<?> superClass;
    protected final String className;
    protected final LuaState state;
    protected final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final List<Method> methods = new ArrayList<>();
    private final Map<String, LuaFunction> storedFunctions = new HashMap<>();

    public ClassBuilder(Class<?> superClass, Class<?>[] interfaces, LuaState state) {
        this.state = state;
        this.className = "allium/GeneratedClass_" + id;

        this.c.visit(
                V17,
                ACC_PUBLIC,
                className,
                null,
                superClass.getName().replace('.', '/'),
                Arrays.stream(interfaces).map(x -> x.getName().replace('.', '/')).toArray(String[]::new)
        );

        this.c.visitField(ACC_PUBLIC | ACC_STATIC, "allium$luaState", Type.getDescriptor(LuaState.class), null, null);

        for (Constructor<?> superCtor : superClass.getConstructors()) {
            if (!Modifier.isPublic(superCtor.getModifiers())) continue;

            var desc = Type.getConstructorDescriptor(superCtor);
            var m = c.visitMethod(superCtor.getModifiers(), "<init>", desc, null, null);
            var args = Type.getArgumentTypes(desc);

            m.visitVarInsn(ALOAD, 0);

            int argIndex = 1;

            for (Type arg : args) {
                m.visitVarInsn(arg.getOpcode(ILOAD), argIndex);

                argIndex += arg.getSize();
            }

            m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>", desc, false);
            m.visitInsn(RETURN);

            m.visitMaxs(0, 0);
        }

        this.superClass = superClass;
        this.methods.addAll(List.of(this.superClass.getMethods()));
        for (var inrf : interfaces) {
            this.methods.addAll(List.of(inrf.getMethods()));
        }
        id++;
    }

    public static LuaTable createLua(Class<?> superClass, Class<?>[] interfaces, LuaState state) {
        var builder = new ClassBuilder(superClass, interfaces, state);


        return LibBuilder.create("javaclass")
                .set("overrideMethod", (unused, args) -> {
                    try {
                        var methodName = args.arg(2).checkString();
                        var paramsTable = args.arg(3).checkTable().checkTable();
                        var function = args.arg(4).checkFunction();

                        var params = new Class[paramsTable.length()];

                        for (int i = 0; i < paramsTable.length(); i++) {
                            var val = paramsTable.rawget(i + 1);
                            params[i] = JavaLib.asClass(val);
                        }

                        builder.overrideMethod(methodName, params, function);
                        return Constants.NIL;
                    } catch (Exception e) {
                        if (e instanceof LuaError le) {
                            throw le;
                        } else {
                            throw new LuaError(e);
                        }
                    }
                })
                .set("createMethod", (unused, args) -> {
                    try {
                        var methodName = args.arg(2).checkString();
                        var paramsTable = args.arg(3).checkTable().checkTable();
                        var returnClass = JavaLib.asClass(args.arg(4));
                        var isStatic = args.arg(5).checkBoolean();
                        var function = args.arg(6).checkFunction();

                        var params = new Class[paramsTable.length()];

                        for (int i = 0; i < paramsTable.length(); i++) {
                            var val = paramsTable.rawget(i + 1);
                            params[i] = JavaLib.asClass(val);
                        }

                        builder.createMethod(methodName, params, returnClass, isStatic, function);
                        return Constants.NIL;
                    } catch (Exception e) {
                        if (e instanceof LuaError le) {
                            throw le;
                        } else {
                            throw new LuaError(e);
                        }
                    }
                })
                .set("build", (unused, args) -> {
                    try {
                        return UserdataFactory.toLuaValue(builder.build());
                    } catch (Exception e) {
                        if (e instanceof LuaError le) {
                            throw le;
                        } else {
                            throw new LuaError(e);
                        }
                    }
                })
                .buildTable();
    }

    public void overrideMethod(String methodName, Class<?>[] parameters, LuaFunction func) {
        var methods = new ArrayList<Method>();

        var funcFieldName = "allium$func_" + methodName;

        c.visitField(ACC_PUBLIC | ACC_STATIC, funcFieldName, Type.getDescriptor(LuaFunction.class), null, null);

        storedFunctions.put(funcFieldName, func);

        UserdataFactory.collectMethods(this.superClass, this.methods, methodName, methods::add);

        for (var method : methods) {
            var methParams = method.getParameterTypes();

            if (methParams.length == parameters.length) {
                boolean match = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (methParams[i] != parameters[i]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    var desc = Type.getMethodDescriptor(method);
                    var isStatic = Modifier.isStatic(method.getModifiers());
                    var m = c.visitMethod(method.getModifiers() & ~Modifier.ABSTRACT, method.getName(), desc, null, null);
                    int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;

                    if (isStatic) varPrefix -= 1;

                    m.visitLdcInsn(method.getParameterCount() + 1);
                    m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
                    m.visitVarInsn(ASTORE, varPrefix);

                    if (!isStatic) {
                        m.visitVarInsn(ALOAD, varPrefix);
                        m.visitLdcInsn(0);
                        m.visitVarInsn(ALOAD, 0);
                        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false);
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

                        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false);
                        m.visitInsn(AASTORE);

                        argIndex += args[i].getSize();
                    }

                    var ret = Type.getType(method.getReturnType());
                    var isVoid = ret.getSort() == Type.VOID;

                    m.visitFieldInsn(GETSTATIC, className, "allium$luaState", Type.getDescriptor(LuaState.class));
                    if (!isVoid) m.visitInsn(DUP);
                    m.visitFieldInsn(GETSTATIC, className, funcFieldName, Type.getDescriptor(LuaFunction.class));
                    m.visitInsn(SWAP);
                    m.visitVarInsn(ALOAD, varPrefix);
                    m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false);
                    m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(LuaFunction.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false);

                    if (!isVoid) {
                        m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false);
                        m.visitLdcInsn(Type.getType(Primitives.wrap(method.getReturnType())));
                        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Ljava/lang/Class;)Ljava/lang/Object;", false);
                        m.visitTypeInsn(CHECKCAST, Type.getInternalName(Primitives.wrap(method.getReturnType())));

                        if (ret.getSort() != Type.ARRAY && ret.getSort() != Type.OBJECT) {
                            AsmUtil.unwrapPrimitive(m, ret);
                        } else {
                            m.visitTypeInsn(CHECKCAST, ret.getInternalName());
                        }
                    }

                    m.visitInsn(ret.getOpcode(IRETURN));

                    m.visitMaxs(0, 0);

                    return;
                }
            }
        }
    }

    public void createMethod(String methodName, Class[] params, Class<?> returnClass, boolean isStatic, LuaFunction func) {
        var methods = new ArrayList<Method>();

        var funcFieldName = "allium$func_" + methodName;

        c.visitField(ACC_PUBLIC | ACC_STATIC, funcFieldName, Type.getDescriptor(LuaFunction.class), null, null);

        storedFunctions.put(funcFieldName, func);

        UserdataFactory.collectMethods(this.superClass, this.methods, methodName, methods::add);

        var paramsType = Arrays.stream(params).map(Type::getType).toArray((i) -> new Type[i]);
        var returnType = returnClass == null ? Type.getType(returnClass) : Type.VOID_TYPE;

        var desc = Type.getMethodDescriptor(returnType, paramsType);
        var m = c.visitMethod((isStatic ? Modifier.STATIC : 0) | Modifier.PUBLIC, methodName, desc, null, null);
        int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;

        if (isStatic) varPrefix -= 1;

        m.visitLdcInsn(params.length + 1);
        m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
        m.visitVarInsn(ASTORE, varPrefix);

        if (!isStatic) {
            m.visitVarInsn(ALOAD, varPrefix);
            m.visitLdcInsn(0);
            m.visitVarInsn(ALOAD, 0);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false);
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

            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitInsn(AASTORE);

            argIndex += args[i].getSize();
        }

        var isVoid = returnType.getSort() == Type.VOID;

        m.visitFieldInsn(GETSTATIC, className, "allium$luaState", Type.getDescriptor(LuaState.class));
        if (!isVoid) m.visitInsn(DUP);
        m.visitFieldInsn(GETSTATIC, className, funcFieldName, Type.getDescriptor(LuaFunction.class));
        m.visitInsn(SWAP);
        m.visitVarInsn(ALOAD, varPrefix);
        m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false);
        m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(LuaFunction.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false);

        if (!isVoid) {
            m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitLdcInsn(Type.getType(Primitives.wrap(returnClass)));
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(UserdataFactory.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Ljava/lang/Class;)Ljava/lang/Object;", false);
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(Primitives.wrap(returnClass)));

            if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                AsmUtil.unwrapPrimitive(m, returnType);
            } else {
                m.visitTypeInsn(CHECKCAST, returnType.getInternalName());
            }
        }

        m.visitInsn(returnType.getOpcode(IRETURN));

        m.visitMaxs(0, 0);

    }

    public byte[] getByteArray() {
        return c.toByteArray();
    }

    public Class<?> build() {
        byte[] classBytes = c.toByteArray();

        if (Allium.DEVELOPMENT) {
            Path classPath = Path.of("allium-dump", className + ".class");

            try {
                Files.createDirectories(classPath.getParent());
                Files.write(classPath, classBytes);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't dump class", e);
            }
        }

        Class<?> klass = DefiningClassLoader.INSTANCE.defineClass(className.replace('/', '.'), classBytes);

        try {
            klass.getField("allium$luaState").set(null, state);

            for (Map.Entry<String, LuaFunction> funcField : storedFunctions.entrySet()) {
                klass.getField(funcField.getKey()).set(null, funcField.getValue());
            }
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException("Failed to initialize class", roe);
        }

        return klass;
    }

    public String getName() {
        return this.className;
    }

    private static class DefiningClassLoader extends ClassLoader {
        public static final DefiningClassLoader INSTANCE = new DefiningClassLoader(DefiningClassLoader.class.getClassLoader());

        public DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
