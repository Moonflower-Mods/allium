// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.*;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.api.JavaLib;
import me.hugeblank.allium.util.Mappings;
import org.apache.commons.lang3.ArrayUtils;
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

public class UserdataFactory<T> {
    private static final Map<EClass<?>, UserdataFactory<?>> FACTORIES = new HashMap<>();
    private final Map<String, List<EMethod>> cachedMethods = new HashMap<>();
    private final Map<String, EField> cachedFields = new HashMap<>();
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
             && !AnnotationUtils.isHiddenFromLua(clazz, x)
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
                                EClass<?> ret = indexImpl.returnType().upperBound();
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
                List<EMethod> matchedMethods = cachedMethods.get(name);
                if (matchedMethods == null) {
                    var collectedMatches = new ArrayList<EMethod>();

                    collectMethods(UserdataFactory.this.clazz, UserdataFactory.this.methods, name, collectedMatches::add);

                    cachedMethods.put(name, collectedMatches);

                    matchedMethods = collectedMatches;
                }

                if (matchedMethods.size() > 0) return new UDFFunctions<>(clazz, matchedMethods, name, isBound ? arg1.checkUserdata(clazz.raw()) : null);

                EField matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, clazz.fields().stream().filter(field -> !field.isStatic()).toList(), name);
                    cachedFields.put(name, matchedField);
                }

                if (matchedField != null) {
                    try {
                        return toLuaValue(matchedField.get(arg1.checkUserdata(clazz.raw())));
                    } catch (Exception e) {
                        // Silent
                    }
                }

                if (name.equals("allium_java_class")) {
                    return UserdataFactory.toLuaValue(clazz);
                }

                return Constants.NIL;
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
                                EClass<?> ret = newIndexImpl.returnType().upperBound();
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

                EField matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, clazz.fields().stream().filter(field -> !field.isStatic() && !field.isFinal()).toList(), name);
                }

                if (matchedField != null) {
                    try {
                        matchedField.set(arg1.checkUserdata(clazz.raw()), toJava(state, arg3, matchedField.fieldType().lowerBound()));
                    } catch (Exception e) {
                        // Silent
                    }
                }

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

    @SuppressWarnings("unchecked")
    public static <T> UserdataFactory<T> of(EClass<T> clazz) {
        return (UserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, UserdataFactory::new);
    }

    public static LuaUserdata getUserData(Object instance) {
        return FACTORIES.computeIfAbsent(EClass.fromJava(instance.getClass()), UserdataFactory::new).create(instance);
    }

    public static void collectMethods(EClass<?> sourceClass, List<EMethod> methods, String name, Consumer<EMethod> consumer) {
        methods.forEach((method -> {
            if (AnnotationUtils.isHiddenFromLua(sourceClass, method)) return;

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

    public static EField findField(EClass<?> sourceClass, List<EField> fields, String name) {
        for (var field : fields) {
            if (AnnotationUtils.isHiddenFromLua(sourceClass, field)) continue;

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

        if (clatz.raw() == EClass.class)
            return JavaLib.asClass(value);
        else if (clatz.raw() == Class.class)
            return JavaLib.asClass(value).raw();

        clatz = clatz.unwrapPrimitive();

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

        if (value.isTable() && clatz.raw().equals(List.class)) {
            EClass<?> componentType = clatz.typeVariableValues().get(0).upperBound();
            LuaTable table = value.checkTable();
            int length = table.length();
            List<Object> list = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                list.add(toJava(state, table.rawget(i + 1), componentType));
            }

            return list;
        }

        if (value.isTable() && clatz.raw().equals(Map.class)) {
            EClass<?> keyType = clatz.typeVariableValues().get(0).upperBound();
            EClass<?> valueType = clatz.typeVariableValues().get(1).upperBound();
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
        }

        if (clatz.type() == ClassType.PRIMITIVE) {
            if (value.isInteger() && clatz.isAssignableFrom(CommonTypes.INT)) { // int
                return value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(CommonTypes.BYTE)) { // byte
                return (byte) value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(CommonTypes.SHORT)) { // short
                return (short) value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(CommonTypes.CHAR)) { // char
                return (char) value.toInteger();
            } else if (value.isNumber() && clatz.isAssignableFrom(CommonTypes.DOUBLE)) { // double
                return value.toDouble();
            } else if (value.isNumber() && clatz.isAssignableFrom(CommonTypes.FLOAT)) { // float
                return (float) value.toDouble();
            } else if (value.isLong() && clatz.isAssignableFrom(CommonTypes.LONG)) { // long
                return value.toLong();
            } else if (value.isBoolean() && clatz.isAssignableFrom(CommonTypes.BOOLEAN)) { // boolean
                return value.toBoolean();
            }
        } else if (value.isString() && clatz.isAssignableFrom(CommonTypes.STRING)) { // string
            return value.toString();
        } else if (value.isFunction() && clatz.type() == ClassType.INTERFACE) { // Callbacks
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
        } else if (value.isUserdata()) {
            var userData = value.checkUserdata();

            if (userData instanceof EClass<?> eClass && clatz.raw() == Class.class)
                return eClass.raw();

            if (clatz.raw().isAssignableFrom(userData.getClass())) {
                return value.checkUserdata();
            }
        }

        if (value.isNil()) {
            return null;
        }

        throw new InvalidArgumentException();
    }

    public static <T> LuaTable listToTable(List<T> list, EClass<T> klass) {
        LuaTable table = new LuaTable();
        int length = list.size();

        for (int i = 0; i < length; i++) {
            table.rawset(i + 1, toLuaValue(list.get(i), klass));
        }

        return table;
    }

    public static <K, V> LuaTable mapToTable(Map<K, V> map, EClass<K> keyType, EClass<V> valueType) {
        LuaTable table = new LuaTable();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            table.rawset(toLuaValue(entry.getKey(), keyType), toLuaValue(entry.getValue(), valueType));
        }

        return table;
    }

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? EClass.fromJava(out.getClass()) : CommonTypes.OBJECT);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static LuaValue toLuaValue(Object out, EClass<?> ret) {
        ret = ret.unwrapPrimitive();

        if (out == null) {
            return Constants.NIL;
        } else if (out instanceof LuaValue) {
            return (LuaValue) out;
        } else if (ret.type() == ClassType.ARRAY) {
            var table = new LuaTable();
            int length = Array.getLength(out);
            for (int i = 1; i <= length; i++) {
                table.rawset(i, toLuaValue(Array.get(out, i - 1), ret.arrayComponent()));
            }
            return table;
        } else if (ret.type() == ClassType.PRIMITIVE) {
            if (ret.equals(CommonTypes.INT)) { // int
                return ValueFactory.valueOf((int) out);
            } else if (ret.equals(CommonTypes.DOUBLE)) { // double
                return ValueFactory.valueOf((double) out);
            } else if (ret.equals(CommonTypes.FLOAT)) { // float
                return ValueFactory.valueOf((float) out);
            } else if (ret.equals(CommonTypes.LONG)) { // long
                return ValueFactory.valueOf((long) out);
            } else if (ret.equals(CommonTypes.BOOLEAN)) { // boolean
                return ValueFactory.valueOf((boolean) out);
            } else if (ret.equals(CommonTypes.SHORT)) { // short
                return ValueFactory.valueOf((short) out);
            } else if (ret.equals(CommonTypes.BYTE)) { // byte
                return ValueFactory.valueOf((byte) out);
            } else if (ret.equals(CommonTypes.CHAR)) { // char
                return ValueFactory.valueOf((char) out);
            }

            throw new IllegalStateException("Unknown primitive type" + ret);
        } else if (ret.equals(CommonTypes.STRING)) { // string
            return ValueFactory.valueOf((String) out);
        } else if (ret.type() == ClassType.INTERFACE && ret.hasAnnotation(FunctionalInterface.class)) {
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : ret.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return new UDFFunctions(ret, Collections.singletonList(ifaceMethod), ifaceMethod.name(), out);
            } else {
                return UserdataFactory.of(ret).create(ret.cast(out));
            }
        } else if (ret.raw().isAssignableFrom(out.getClass())) {
            return UserdataFactory.of(ret).create(ret.cast(out));
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
                                EClass<?> ret = method.returnType().upperBound();
                                Object out = method.invoke(instance, jargs);
                                if (ret.raw() == Varargs.class)
                                    return (Varargs) out;
                                else
                                    return toLuaValue(out, ret);
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                if (e.getTargetException() instanceof LuaError err)
                                    throw err;

                                throw new LuaError(e);
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
