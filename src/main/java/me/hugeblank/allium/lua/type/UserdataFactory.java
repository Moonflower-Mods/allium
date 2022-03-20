// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.*;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.Mappings;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

public class UserdataFactory<T> {
    private static final Map<EClass<?>, UserdataFactory<?>> FACTORIES = new HashMap<>();
    private final Map<String, List<EMethod>> cachedMethods = new HashMap<>();
    private final Map<String, EField> cachedFields = new HashMap<>();
    private final EClass<T> clazz;
    private final List<EMethod> methods;
    private final LuaTable metatable = new LuaTable();

    {
        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                String name = arg2.checkString(); // mapped name
                List<EMethod> matchedMethods = cachedMethods.get(name);
                if (matchedMethods == null) {
                    var collectedMatches = new ArrayList<EMethod>();

                    collectMethods(UserdataFactory.this.clazz, UserdataFactory.this.methods, name, collectedMatches::add);

                    cachedMethods.put(name, collectedMatches);

                    matchedMethods = collectedMatches;
                }

                if (matchedMethods.size() > 0) return new UDFFunctions<>(clazz, matchedMethods);

                EField matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, clazz.fields().stream().filter(field -> !field.isStatic()).toList(), name);
                    cachedFields.put(name, matchedField);
                }

                if (matchedField != null) {
                    try {
                        return toLuaValue(matchedField.get(toJava(state, arg1, clazz)));
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
                String name = arg2.checkString(); // mapped name

                EField matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, clazz.fields().stream().filter(field -> !field.isStatic() && !field.isFinal()).toList(), name);
                }

                if (matchedField != null) {
                    try {
                        matchedField.set(toJava(state, arg1, clazz), toJava(state, arg3, matchedField.fieldType().lowerBound()));
                    } catch (Exception e) {
                        // Silent
                    }
                }

                return Constants.NIL;
            }
        });
    }

    protected UserdataFactory(EClass<T> clazz) {
        this.clazz = clazz;
        this.methods = clazz.methods();

        var comparableInst = clazz.allInterfaces().stream().filter(x -> x.raw() == Comparable.class).findFirst().orElse(null);
        if (comparableInst != null) {
            var bound = comparableInst.typeVariableValues().get(0).lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }
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

    public static Object[] toJavaArguments(LuaState state, Varargs args, final int offset, List<EParameter> parameters) throws LuaError, InvalidArgumentException {
        if (args.count() - offset + 1 < parameters.size()) {
            throw new InvalidArgumentException();
        }

        Object[] arguments = new Object[parameters.size()];

        int ind = offset;
        for (EParameter param : parameters) { // For each parameter in the matched call
            var arg = toJava(state, args.arg(ind), param.parameterType().upperBound());
            arguments[ind - offset] = arg;
            ind++;
        }

        return arguments;
    }

    public static Object toJava(LuaState state, LuaValue value, Class<?> clatz) throws InvalidArgumentException, LuaError {
        return toJava(state, value, EClass.fromJava(clatz));
    }

    public static Object toJava(LuaState state, LuaValue value, EClass<?> clatz) throws LuaError, InvalidArgumentException {
        if (clatz.isAssignableFrom(value.getClass()) && !clatz.equals(CommonTypes.OBJECT)) {
            return value;
        }

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
                EMethod finalIfaceMethod = ifaceMethod;

                return Proxy.newProxyInstance(clatz.classLoader(), new Class[]{clatz.raw()},
                        (p, m, params) -> {
                            if (m.isDefault()) {
                                return InvocationHandler.invokeDefault(p, m, params);
                            }

                            var args = new LuaValue[params.length];
                            for (int i = 0; i < params.length; i++) {
                                args[i] = toLuaValue(params[i], finalIfaceMethod.parameters().get(i).parameterType().lowerBound());
                            }

                            return toJava(state, func.invoke(state, ValueFactory.varargsOf(args)).first(), finalIfaceMethod.returnType().upperBound());
                        });
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

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? EClass.fromJava(out.getClass()) : CommonTypes.OBJECT);
    }

    public static LuaValue toLuaValue(Object out, Class<?> ret) {
        return toLuaValue(out, EClass.fromJava(ret));
    }

    public static LuaValue toLuaValue(Object out, EClass<?> ret) {
        if (out instanceof LuaValue) {
            return (LuaValue) out;
        }

        if (out != null && ret.type() == ClassType.ARRAY) {
            var table = new LuaTable();
            int length = Array.getLength(out);
            for (int i = 1; i <= length; i++) {
                table.rawset(i, toLuaValue(Array.get(out, i - 1), ret.arrayComponent()));
            }
            return table;
        } else if (out != null) {
            ret = ret.unwrapPrimitive();

            if (ret.type() == ClassType.PRIMITIVE) {
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
            } else if (ret.equals(CommonTypes.STRING)) { // string
                return ValueFactory.valueOf((String) out);
            } else if (ret.raw().isAssignableFrom(out.getClass())) {
                return UserdataFactory.of(ret).create(ret.cast(out));
            } else {
                return Constants.NIL;
            }
        }

        return Constants.NIL;
    }

    public LuaUserdata create(Object instance) {
        return new LuaUserdata(instance, metatable);
    }

    private static final class UDFFunctions<T> extends VarArgFunction {
        private final EClass<T> clazz;
        private final List<EMethod> matches;

        public UDFFunctions(EClass<T> clazz, List<EMethod> matches) {
            this.clazz = clazz;
            this.matches = matches;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
            StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                    this.matches.get(0).name() + "\" for \"" + clazz.name() + "\"" +
                    "\nThe following are correct argument types:\n"
            );

            try {
                T instance = args.arg(1).checkUserdata(clazz.raw());
                for (EMethod method : matches) { // For each matched method from the index call
                    var parameters = method.parameters();
                    try {
                        var jargs = toJavaArguments(state, args, 2, parameters);

                        if (jargs.length == parameters.size()) { // Found a match!
                            try { // Get the return type, invoke method, cast returned value, cry.
                                EClass<?> ret = method.returnType().upperBound();
                                Object out = method.invoke(instance, jargs);
                                return toLuaValue(out, ret);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new LuaError(e);
                            }
                        }
                    } catch (InvalidArgumentException | IllegalArgumentException e) {
                        var params = new StringBuilder();

                        for (var param : parameters) {
                            params.append(param.parameterType().toString()).append(", ");
                        }

                        paramList.add(params.toString());
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
