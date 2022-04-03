// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.*;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.JavaLib;
import me.hugeblank.allium.util.Mappings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class UserdataFactory<T> {
    private static final Map<EClass<?>, UserdataFactory<?>> FACTORIES = new HashMap<>();
    private static final Map<Class<?>, Function<EClass<?>, LuaDeserializer<?>>> DESERIALIZERS = new HashMap<>();
    private static final Map<Class<?>, Function<EClassUse<?>, LuaSerializer<?>>> SERIALIZERS = new HashMap<>();
    private final Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
    private final EClass<T> clazz;
    private final List<EMethod> methods;
    private final LuaTable metatable;
    private @Nullable LuaTable boundMetatable;
    private final @Nullable EMethod indexImpl;
    private final @Nullable EMethod newIndexImpl;

    protected UserdataFactory(EClass<T> clazz) {
        this.clazz = clazz;
        this.methods = clazz.methods();
        this.indexImpl = tryFindOp(LuaIndex.class, 1,"get");
        this.newIndexImpl = tryFindOp(null, 2, "set", "put");
        this.metatable = createMetatable(false);
    }

    private @Nullable EMethod tryFindOp(@Nullable Class<? extends Annotation> annotation, int minParams, String... specialNames) {
        EMethod method = null;

        if (annotation != null)
            method = clazz
                .methods()
                .stream()
                .filter(x ->
                    !x.isStatic()
                 && x.hasAnnotation(annotation))
                .findAny()
                .orElse(null);

        if (method != null) return  method;

        method = clazz
            .methods()
            .stream()
            .filter(x ->
                !x.isStatic()
             && !AnnotationUtils.isHiddenFromLua(x)
             && ArrayUtils.contains(specialNames, x.name())
             && x.parameters().size() >= minParams)
            .findAny()
            .orElse(null);

        return method;
    }

    private LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = new LuaTable();

        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                if (indexImpl != null) {
                    var parameters = indexImpl.parameters();
                    try {
                        var jargs = toJavaArguments(state, arg2, 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = toJava(state, arg1, clazz);
                                EClassUse<?> ret = indexImpl.returnTypeUse().upperBound();
                                Object out = indexImpl.invoke(instance, jargs);
                                // If out is null, we can assume the index is nil
                                if (out == null) throw new InvalidArgumentException();
                                return toLuaValue(out, ret);
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                var target = e.getTargetException();

                                if (target instanceof LuaError err) {
                                    throw err;
                                } else if (target instanceof IndexOutOfBoundsException) {
                                    // Continue.
                                } else {
                                    throw new LuaError(target);
                                }
                            } catch (InvalidArgumentException ignore) {}
                        }
                    } catch (InvalidArgumentException | IllegalArgumentException e) {
                        // Continue.
                    }
                }

                String name = arg2.checkString(); // mapped name

                if (name.equals("allium_java_class")) {
                    return UserdataFactory.of(EClass.fromJava(EClass.class)).create(clazz);
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = resolveProperty(name);

                    cachedProperties.put(name, cachedProperty);
                }

                return cachedProperty.get(name, state, arg1.checkUserdata(clazz.raw()), isBound);
            }
        });

        metatable.rawset("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
                if (newIndexImpl != null) {
                    var parameters = newIndexImpl.parameters();
                    try {
                        var jargs = toJavaArguments(state, ValueFactory.varargsOf(arg1, arg2), 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = toJava(state, arg1, clazz);
                                EClassUse<?> ret = newIndexImpl.returnTypeUse().upperBound();
                                Object out = newIndexImpl.invoke(instance, jargs);
                                return toLuaValue(out, ret);
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                if (e.getTargetException() instanceof LuaError err)
                                    throw err;

                                throw new LuaError(e);
                            }
                        }
                    } catch (InvalidArgumentException | IllegalArgumentException e) {
                        // Continue.
                    }
                }

                String name = arg2.checkString(); // mapped name

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = resolveProperty(name);

                    cachedProperties.put(name, cachedProperty);
                }

                cachedProperty.set(name, state, arg1.checkUserdata(clazz.raw()), arg3);

                return Constants.NIL;
            }
        });

        var comparableInst = clazz.allInterfaces().stream().filter(x -> x.raw() == Comparable.class).findFirst().orElse(null);
        if (comparableInst != null) {
            var bound = comparableInst.typeVariableValues().get(0).lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }

        return metatable;
    }

    private PropertyData<? super T> resolveProperty(String name) {
        List<EMethod> foundMethods = new ArrayList<>();

        collectMethods(clazz, this.methods, name, foundMethods::add);

        if (foundMethods.size() > 0)
            return new MethodData(foundMethods, name);

        EField field = findField(clazz, clazz.fields(), name);

        if (field != null)
            return new FieldData(field);

        EMethod getter = findMethod(clazz, this.methods, "get" + StringUtils.capitalize(name), method -> AnnotationUtils.countLuaArguments(method) == 0);

        if (getter != null) {
            EMethod setter = findMethod(clazz, this.methods, "set" + StringUtils.capitalize(name), method -> AnnotationUtils.countLuaArguments(method) == 1);

            return new PropertyMethodData(getter, setter);
        }

        return EmptyData.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> UserdataFactory<T> of(EClass<T> clazz) {
        return (UserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, UserdataFactory::new);
    }

    public static LuaUserdata getUserData(Object instance) {
        return FACTORIES.computeIfAbsent(EClass.fromJava(instance.getClass()), UserdataFactory::new).create(instance);
    }

    public static <T> void registerSerializer(Class<T> klass, LuaSerializer<T> serializer) {
        if (SERIALIZERS.put(klass, unused -> serializer) != null)
            throw new IllegalStateException("Serializer already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerComplexSerializer(Class<T> klass, Function<EClassUse<T>, LuaSerializer<T>> serializerFactory) {
        if (SERIALIZERS.put(klass, (Function<EClassUse<?>, LuaSerializer<?>>)(Object) serializerFactory) != null)
            throw new IllegalStateException("Serializer already registered for " + klass);
    }

    public static <T> void registerDeserializer(Class<T> klass, LuaDeserializer<T> deserializer) {
        if (DESERIALIZERS.put(klass, unused -> deserializer) != null)
            throw new IllegalStateException("Deserializer already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerComplexDeserializer(Class<T> klass, Function<EClass<T>, LuaDeserializer<T>> deserializerFactory) {
        if (DESERIALIZERS.put(klass, (Function<EClass<?>, LuaDeserializer<?>>)(Object) deserializerFactory) != null)
            throw new IllegalStateException("Deserializer already registered for " + klass);
    }

    public static void collectMethods(EClass<?> sourceClass, List<EMethod> methods, String name, Consumer<EMethod> consumer) {
        methods.forEach((method -> {
            if (AnnotationUtils.isHiddenFromLua(method)) return;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        consumer.accept(method);
                    }
                }

                return;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                consumer.accept(method);
            }

            if (methodName.startsWith("allium_private$")) {
                return;
            }

            if (!Allium.DEVELOPMENT) {
                var mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    consumer.accept(method);
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }
                }
            }
        }));
    }

    public static EMethod findMethod(EClass<?> sourceClass, List<EMethod> methods, String name, Predicate<EMethod> filter) {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;
            if (!filter.test(method)) continue;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return method;
                    }
                }

                continue;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                return method;
            }

            if (methodName.startsWith("allium_private$")) {
                continue;
            }

            if (!Allium.DEVELOPMENT) {
                var mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    return method;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public static EField findField(EClass<?> sourceClass, Collection<EField> fields, String name) {
        for (var field : fields) {
            if (AnnotationUtils.isHiddenFromLua(field)) continue;

            String[] altNames = AnnotationUtils.findNames(field);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return field;
                    }
                }

                continue;
            }

            if (Allium.DEVELOPMENT) {
                if (field.name().equals(name)) {
                    return field;
                }
            } else {
                if (Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, field)).split("#")[1].equals(name)) {
                    return field;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    if (Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, field)).split("#")[1].equals(name)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }

    public static String paramsToPrettyString(List<EParameter> parameters) {
        var sb = new StringBuilder();
        boolean isFirst = true;
        boolean optionalsStarted = false;

        for (var param : parameters) {
            if (param.hasAnnotation(LuaStateArg.class)) {
                continue;
            }

            if (!isFirst) sb.append(", ");
            isFirst = false;

            if (param.hasAnnotation(LuaArgs.class)) {
                sb.append("...");
            } else {
                if (param.hasAnnotation(OptionalArg.class)) {
                    if (!optionalsStarted)
                        sb.append("[");

                    optionalsStarted = true;
                }

                sb.append(param);
            }

        }

        if (optionalsStarted)
            sb.append("]");

        return sb.toString();
    }

    public static Object[] toJavaArguments(LuaState state, Varargs args, final int offset, List<EParameter> parameters) throws LuaError, InvalidArgumentException {
        Object[] arguments = new Object[parameters.size()];

        int filledJavaArguments = 0;
        int luaOffset = offset;
        for (EParameter param : parameters) { // For each parameter in the matched call
            if (param.hasAnnotation(LuaStateArg.class)) {
                if (!param.parameterType().upperBound().raw().equals(LuaState.class))
                    throw new InvalidArgumentException("@ProvideLuaState parameter must take LuaState!");

                arguments[filledJavaArguments] = state;
            } else if (param.hasAnnotation(LuaArgs.class)) {
                if (!param.parameterType().upperBound().raw().equals(Varargs.class))
                    throw new InvalidArgumentException("@LuaArgs parameter must take Varargs!");

                arguments[filledJavaArguments] = args.subargs(luaOffset);
                luaOffset = args.count() + 1;
            } else if (param.isVarArgs()) {
                Varargs sub = args.subargs(luaOffset);
                LuaTable table = new LuaTable();

                for (int i = 0; i < sub.count(); i++) {
                    table.rawset(i + 1, sub.arg(i + 1));
                }

                arguments[filledJavaArguments] = toJava(state, table, param.parameterType().upperBound());
                luaOffset = args.count() + 1;
            } else {
                Object arg;

                if (luaOffset > args.count()) {
                    if (param.hasAnnotation(OptionalArg.class)) {
                        arg = null;
                    } else {
                        throw new InvalidArgumentException("Not enough arguments!");
                    }
                } else {
                    arg = toJava(state, args.arg(luaOffset), param.parameterType().upperBound());
                    luaOffset++;
                }

                arguments[filledJavaArguments] = arg;
            }

            filledJavaArguments++;
        }

        if (luaOffset != args.count() + 1)
            throw new InvalidArgumentException("Too many arguments!");

        return arguments;
    }

    public static Object toJava(LuaState state, LuaValue value, Class<?> clatz) throws InvalidArgumentException, LuaError {
        return toJava(state, value, EClass.fromJava(clatz));
    }

    public static Object toJava(LuaState state, LuaValue value, EClass<?> clatz) throws LuaError, InvalidArgumentException {
        if (clatz.isAssignableFrom(value.getClass()) && !clatz.equals(CommonTypes.OBJECT)) {
            return value;
        }

        if (value.isNil())
            return null;

        if (value.isUserdata(clatz.wrapPrimitive().raw()))
            return value.toUserdata();

        clatz = clatz.unwrapPrimitive();

        var deserializerFactory = DESERIALIZERS.get(clatz.raw());
        if (deserializerFactory != null) {
            var deserializer = deserializerFactory.apply(clatz);

            if (deserializer != null) {
                Object result = deserializer.fromLua(state, value);

                if (result != null) return result;
            }
        }

        if (clatz.type() == ClassType.ARRAY) {
            if (!value.isTable())
                throw new LuaError(
                        "Expected table of "
                                + clatz.arrayComponent()
                                + "s, got "
                                + value.typeName()
                );
            LuaTable table = value.checkTable();
            int length = table.length();
            Object arr = Array.newInstance(clatz.arrayComponent().raw(), table.length());
            for (int i = 0; i < length; i++) {
                Array.set(arr, i, toJava(state, table.rawget(i + 1), clatz.arrayComponent()));
            }
            return clatz.cast(arr);
        }

        if (value.isFunction() && clatz.type() == ClassType.INTERFACE) { // Callbacks
            var func = value.checkFunction();
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : clatz.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return ProxyGenerator.getProxyFactory(clatz, ifaceMethod).apply(state, func);
            } else {
                return value.checkUserdata(clatz.raw());
            }
        }

        throw new InvalidArgumentException("Couldn't convert " + value + " to java! Target type is " + clatz);
    }

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? EClass.fromJava(out.getClass()) : CommonTypes.OBJECT);
    }

    public static LuaValue toLuaValue(Object out, EClass<?> ret) {
        return toLuaValue(out, ret.asEmptyUse());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static LuaValue toLuaValue(Object out, EClassUse<?> ret) {
        EClass<?> klass = ret.type();

        klass = klass.unwrapPrimitive();

        if (out == null) {
            return Constants.NIL;
        } else if (out instanceof LuaValue) {
            return (LuaValue) out;
        }

        var serializerFactory = SERIALIZERS.get(klass.raw());
        if (serializerFactory != null) {
            var serializer = (LuaSerializer<Object>) serializerFactory.apply(ret);

            if (serializer != null) {
                LuaValue result = serializer.toLua(out);

                if (result != null) return result;
            }
        }

        if (klass.type() == ClassType.ARRAY) {
            var table = new LuaTable();
            int length = Array.getLength(out);
            for (int i = 1; i <= length; i++) {
                table.rawset(i, toLuaValue(Array.get(out, i - 1), ret.arrayComponent()));
            }
            return table;
        } else if (klass.type() == ClassType.INTERFACE && klass.hasAnnotation(FunctionalInterface.class)) {
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : klass.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return new UDFFunctions(klass, Collections.singletonList(ifaceMethod), ifaceMethod.name(), out);
            } else {
                return UserdataFactory.of(klass).create(klass.cast(out));
            }
        } else if (klass.raw().isAssignableFrom(out.getClass())) {
            if (klass.isGeneric()) {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(klass).createBound(klass.cast(out));
                else
                    return UserdataFactory.of(klass).create(klass.cast(out));
            } else {
                EClass<?> trueRet = EClass.fromJava(out.getClass());
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(trueRet).createBound(out);
                else
                    return UserdataFactory.of(trueRet).create(out);
            }
        } else {
            return Constants.NIL;
        }
    }

    public LuaUserdata create(Object instance) {
        return new LuaUserdata(instance, metatable);
    }

    public LuaUserdata createBound(Object instance) {
        if (boundMetatable == null)
            boundMetatable = createMetatable(true);

        return new LuaUserdata(instance, boundMetatable);
    }

    static {
        registerSerializer(int.class, ValueFactory::valueOf);
        registerSerializer(byte.class, ValueFactory::valueOf);
        registerSerializer(short.class, ValueFactory::valueOf);
        registerSerializer(char.class, ValueFactory::valueOf);
        registerSerializer(double.class, ValueFactory::valueOf);
        registerSerializer(float.class, ValueFactory::valueOf);
        registerSerializer(long.class, ValueFactory::valueOf);
        registerSerializer(boolean.class, ValueFactory::valueOf);
        registerSerializer(String.class, ValueFactory::valueOf);

        registerComplexSerializer(List.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> componentUse = use.typeVariableValues().get(0).upperBound();

            return list -> {
                LuaTable table = new LuaTable();
                int length = list.size();

                for (int i = 0; i < length; i++) {
                    table.rawset(i + 1, toLuaValue(list.get(i), componentUse));
                }

                return table;
            };
        });

        registerComplexSerializer(Map.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> keyUse = use.typeVariableValues().get(0).upperBound();
            EClassUse<?> valueUse = use.typeVariableValues().get(1).upperBound();

            return map -> {
                LuaTable table = new LuaTable();

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                    table.rawset(toLuaValue(entry.getKey(), keyUse), toLuaValue(entry.getValue(), valueUse));
                }

                return table;
            };
        });

        registerDeserializer(int.class, (state, val) -> val.isInteger() ? val.toInteger() : null);
        registerDeserializer(byte.class, (state, val) -> val.isInteger() ? (byte)val.toInteger() : null);
        registerDeserializer(short.class, (state, val) -> val.isInteger() ? (short)val.toInteger() : null);
        registerDeserializer(char.class, (state, val) -> val.isInteger() ? (char)val.toInteger() : null);
        registerDeserializer(double.class, (state, val) -> val.isNumber() ? val.toDouble() : null);
        registerDeserializer(float.class, (state, val) -> val.isNumber() ? (float)val.toDouble() : null);
        registerDeserializer(long.class, (state, val) -> val.isLong() ? val.toLong() : null);
        registerDeserializer(boolean.class, (state, val) -> val.isBoolean() ? val.toBoolean() : null);
        registerDeserializer(String.class, (state, val) -> val.isString() ? val.toString() : null);

        registerDeserializer(EClass.class, (state, val) -> JavaLib.asClass(val));
        registerDeserializer(Class.class, (state, val) -> {
            EClass<?> klass = JavaLib.asClass(val);
            if (klass == null) return null;
            else return klass.raw();
        });

        registerComplexDeserializer(List.class, klass -> {
            EClass<?> componentType = klass.typeVariableValues().get(0).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                List<Object> list = new ArrayList<>(length);

                for (int i = 0; i < length; i++) {
                    list.add(toJava(state, table.rawget(i + 1), componentType));
                }

                return list;
            };
        });

        registerComplexDeserializer(Map.class, klass -> {
            EClass<?> keyType = klass.typeVariableValues().get(0).upperBound();
            EClass<?> valueType = klass.typeVariableValues().get(1).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                Map<Object, Object> map = new HashMap<>(length);

                LuaValue k = Constants.NIL;
                while (true) {
                    Varargs n = table.next(k);
                    if ((k = n.arg(1)).isNil())
                        break;
                    LuaValue v = n.arg(2);

                    map.put(toJava(state, k, keyType), toJava(state, v, valueType));
                }

                return map;
            };
        });
    }

    private interface PropertyData<T> {
        LuaValue get(String name, LuaState state, T instance, boolean isBound) throws LuaError;

        default void set(String name, LuaState state, T instance, LuaValue value) throws LuaError {
            throw new LuaError("property '" + name + "' doesn't support set");
        }
    }

    private static class EmptyData implements PropertyData<Object> {
        public static EmptyData INSTANCE = new EmptyData();

        @Override
        public LuaValue get(String name, LuaState state, Object instance, boolean isBound) throws LuaError {
            return Constants.NIL;
        }

        @Override
        public void set(String name, LuaState state, Object instance, LuaValue value) throws LuaError {
            throw new LuaError("property '" + name + "' doesn't exist");
        }
    }

    private class MethodData implements PropertyData<T> {
        public final List<EMethod> methods;
        public final UDFFunctions<T> unboundFunction;

        public MethodData(List<EMethod> methods, String name) {
            this.methods = methods;
            this.unboundFunction = new UDFFunctions<>(clazz, methods, name, null);
        }

        @Override
        public LuaValue get(String name, LuaState state, T instance, boolean isBound) {
            if (isBound)
                return new UDFFunctions<>(clazz, methods, name, instance);
            else
                return unboundFunction;
        }
    }

    private class FieldData implements PropertyData<T> {
        public final EField field;

        private FieldData(EField field) {
            this.field = field;
        }

        @Override
        public LuaValue get(String name, LuaState state, Object instance, boolean isBound) throws LuaError {
            try {
                return toLuaValue(field.get(instance), field.fieldTypeUse().upperBound());
            } catch (IllegalAccessException e) {
                throw new LuaError(e);
            }
        }
    }

    private class PropertyMethodData implements PropertyData<T> {
        public final EMethod getter;
        public final @Nullable EMethod setter;

        private PropertyMethodData(EMethod getter, @Nullable EMethod setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public LuaValue get(String name, LuaState state, T instance, boolean isBound) throws LuaError {
            var params = getter.parameters();
            try {
                var jargs = toJavaArguments(state, Constants.NONE, 1, params);

                EClassUse<?> ret = getter.returnTypeUse().upperBound();
                Object out = getter.invoke(instance, jargs);
                return toLuaValue(out, ret);
            } catch (InvalidArgumentException e) {
                throw new IllegalStateException("Getter for '" + name + "' needs arguments");
            } catch (ReflectiveOperationException roe) {
                throw new LuaError(roe);
            }
        }

        @Override
        public void set(String name, LuaState state, T instance, LuaValue value) throws LuaError {
            if (setter == null) {
                PropertyData.super.set(name, state, instance, value);
                return;
            }

            var params = setter.parameters();
            try {
                var jargs = toJavaArguments(state, value, 1, params);

                setter.invoke(instance, jargs);
            } catch (InvalidArgumentException e) {
                throw new IllegalStateException("Setter for '" + name + "' needs more than one argument");
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof LuaError luaError)
                    throw luaError;

                throw new LuaError(e.getTargetException());
            } catch (ReflectiveOperationException e) {
                throw new LuaError(e);
            }
        }
    }

    private static final class UDFFunctions<T> extends VarArgFunction {
        private final EClass<T> clazz;
        private final List<EMethod> matches;
        private final String name;
        private final T boundReceiver;

        public UDFFunctions(EClass<T> clazz, List<EMethod> matches, String name, T boundReceiver) {
            this.clazz = clazz;
            this.matches = matches;
            this.name = name;
            this.boundReceiver = boundReceiver;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
            StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                    name + "\" for \"" + clazz.name() + "\"" +
                    "\nThe following are correct argument types:\n"
            );

            try {
                T instance = boundReceiver != null ? boundReceiver : args.arg(1).checkUserdata(clazz.raw());
                for (EMethod method : matches) { // For each matched method from the index call
                    var parameters = method.parameters();
                    try {
                        var jargs = toJavaArguments(state, args, boundReceiver == null ? 2 : 1, parameters);

                        if (jargs.length == parameters.size()) { // Found a match!
                            try { // Get the return type, invoke method, cast returned value, cry.
                                EClassUse<?> ret = method.returnTypeUse().upperBound();
                                Object out = method.invoke(instance, jargs);
                                if (ret.type().raw() == Varargs.class)
                                    return (Varargs) out;
                                else
                                    return toLuaValue(out, ret);
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                if (e.getTargetException() instanceof LuaError err)
                                    throw err;

                                throw new LuaError(e.getTargetException());
                            }
                        }
                    } catch (InvalidArgumentException e) {
                        paramList.add(UserdataFactory.paramsToPrettyString(parameters));
                    }
                }
            } catch (Exception e) {
                if (e instanceof LuaError) {
                    throw e;
                } else {
                    e.printStackTrace();
                    error = new StringBuilder(e.toString());
                }
            }

            for (String headers : paramList) {
                error.append(headers).append("\n");
            }

            throw new LuaError(error.toString());
        }
    }

    public static final class InvalidArgumentException extends Exception {
        public InvalidArgumentException() {
            super();
        }


        public InvalidArgumentException(String message) {
            super(message);
        }


        public InvalidArgumentException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidArgumentException(Throwable cause) {
            super(cause);
        }
    }

    private static final class LessFunction extends TwoArgFunction {
        private final EClass<?> bound;

        public LessFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
            Comparable<Object> cmp = arg1.checkUserdata(Comparable.class);
            Object cmp2 = arg2.checkUserdata(bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0);
        }
    }

    private static final class LessOrEqualFunction extends TwoArgFunction {
        private final EClass<?> bound;

        public LessOrEqualFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
            Comparable<Object> cmp = arg1.checkUserdata(Comparable.class);
            Object cmp2 = arg2.checkUserdata(bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0 || cmp.equals(cmp2));
        }
    }

}
