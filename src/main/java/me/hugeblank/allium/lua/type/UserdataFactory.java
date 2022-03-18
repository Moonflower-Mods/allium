// Eldritch horrors, sponsored by hugeblank!
// This class converts all public methods from any class from Java -> Lua.
// It's one saving grace is that I've restricted it from chewing through *every* class.
// The fact that it could though... *shivers* euhh. I'm so sorry.
// If someone wants to SCP this, please by all means do so.
package me.hugeblank.allium.lua.type;

import com.google.common.primitives.Primitives;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.Mappings;
import org.apache.commons.lang3.ClassUtils;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

public class UserdataFactory<T> {
    private static final Map<Class<?>, UserdataFactory<?>> FACTORIES = new HashMap<>();
    private final Map<String, List<Method>> cachedMethods = new HashMap<>();
    private final Map<String, Field> cachedFields = new HashMap<>();
    private final Class<T> clazz;
    private final List<Method> methods;
    private final LuaTable metatable = new LuaTable();

    {
        // TODO: logical operators!!!
        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                String name = arg2.checkString(); // mapped name
                List<Method> matchedMethods = cachedMethods.get(name);
                if (matchedMethods == null) {
                    var collectedMatches = new ArrayList<Method>();

                    collectMethods(UserdataFactory.this.clazz, UserdataFactory.this.methods, name, collectedMatches::add);

                    cachedMethods.put(name, collectedMatches);

                    matchedMethods = collectedMatches;
                }

                if (matchedMethods.size() > 0) return new UDFFunctions<>(clazz, matchedMethods);

                Field matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, Arrays.stream(clazz.getFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toList(), name);
                }

                if (matchedField != null) {
                    try {
                        return toLuaValue(matchedField.get(toJava(state, arg1, clazz)));
                    } catch (Exception e) {
                        // Silend
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

                Field matchedField = cachedFields.get(name);
                if (matchedField == null) {
                    matchedField = findField(clazz, Arrays.stream(clazz.getFields()).filter(field -> !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())).toList(), name);
                }

                if (matchedField != null) {
                    try {
                        matchedField.set(toJava(state, arg1, clazz), toJava(state, arg3, matchedField.getType()));
                    } catch (Exception e) {
                        // Silent
                    }
                }

                return Constants.NIL;
            }
        });
    }

    protected UserdataFactory(Class<T> clazz) {
        this.clazz = clazz;
        this.methods = Arrays.asList(clazz.getMethods());
    }

    public static <T> UserdataFactory<T> of(Class<T> clazz) {
        return (UserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, UserdataFactory::new);
    }

    public static LuaUserdata getUserData(Object instance) {
        return FACTORIES.computeIfAbsent(instance.getClass(), UserdataFactory::new).create(instance);
    }

    public static void collectMethods(Class<?> sourceClass, List<Method> methods, String name, Consumer<Method> consumer) {
        methods.forEach((method -> {
            var methodName = method.getName();

            if (methodName.equals(name) || methodName.equals("allium$" + name)) {
                consumer.accept(method);
            }

            if (methodName.startsWith("allium_private$")) {
                return;
            }

            if (!Allium.DEVELOPMENT) {
                if (Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, method)).split("#")[1].equals(name)) {
                    consumer.accept(method);
                }

                for (var clazz : ClassUtils.getAllSuperclasses(sourceClass)) {
                    if (Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1].equals(name)) {
                        consumer.accept(method);
                    }
                }

                for (var clazz : ClassUtils.getAllInterfaces(sourceClass)) {
                    if (Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, method)).split("#")[1].equals(name)) {
                        consumer.accept(method);
                    }
                }
            }
        }));
    }

    public static Field findField(Class<?> sourceClass, List<Field> fields, String name) {
        for (var field : fields) {
            if (Allium.DEVELOPMENT) {
                if (field.getName().equals(name)) {
                    return field;
                }
            } else {
                if (Allium.MAPPINGS.getYarn(Mappings.asMethod(sourceClass, field)).split("#")[1].equals(name)) {
                    return field;
                }

                for (var clazz : ClassUtils.getAllSuperclasses(sourceClass)) {
                    if (Allium.MAPPINGS.getYarn(Mappings.asMethod(clazz, field)).split("#")[1].equals(name)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }

    public static Object[] toJavaArguments(LuaState state, Varargs args, final int offset, Class<?>[] parameters) throws LuaError, InvalidArgumentException {
        if (args.count() - offset + 1 < parameters.length) {
            throw new InvalidArgumentException();
        }

        Object[] arguments = new Object[parameters.length];

        int ind = offset;
        for (Class<?> clatz : parameters) { // For each parameter in the matched call
            var arg = toJava(state, args.arg(ind), clatz);
            arguments[ind - offset] = arg;
            ind++;
        }

        return arguments;
    }

    public static Object toJava(LuaState state, LuaValue value, Class<?> clatz) throws LuaError, InvalidArgumentException {
        if (clatz.isAssignableFrom(value.getClass()) && clatz != Object.class) {
            return value;
        }


        if (clatz.isArray()) {
            if (!value.isTable())
                throw new LuaError(
                        "Expected table of "
                                + clatz.getComponentType()
                                + "s, got "
                                + value.typeName()
                );
            LuaTable table = value.checkTable();
            int length = table.length();
            Object arr = Array.newInstance(clatz.getComponentType(), table.length());
            for (int i = 0; i < length; i++) {
                Array.set(arr, i, toJava(state, table.rawget(i + 1), clatz.getComponentType()));
            }
            return clatz.cast(arr);
        }

        if (clatz.isPrimitive()) {
            if (value.isInteger() && clatz.isAssignableFrom(int.class)) { // int
                return value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(byte.class)) { // byte
                return (byte) value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(short.class)) { // short
                return (short) value.toInteger();
            } else if (value.isInteger() && clatz.isAssignableFrom(char.class)) { // char
                return (char) value.toInteger();
            } else if (value.isNumber() && clatz.isAssignableFrom(double.class)) { // double
                return value.toDouble();
            } else if (value.isNumber() && clatz.isAssignableFrom(float.class)) { // float
                return (float) value.toDouble();
            } else if (value.isLong() && clatz.isAssignableFrom(long.class)) { // long
                return value.toLong();
            } else if (value.isBoolean() && clatz.isAssignableFrom(boolean.class)) { // boolean
                return value.toBoolean();
            }
        } else if (value.isString() && clatz.isAssignableFrom(String.class)) { // string
            return value.toString();
        } else if (value.isFunction() && clatz.isInterface()) { // Callbacks
            var func = value.checkFunction();

            int unimplemented = 0;
            for (var meth : clatz.getMethods()) {
                if (Modifier.isAbstract(meth.getModifiers())) {
                    unimplemented++;
                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return Proxy.newProxyInstance(clatz.getClassLoader(), new Class[]{clatz},
                        (p, m, params) -> {
                            if (m.isDefault()) {
                                return InvocationHandler.invokeDefault(p, m, params);
                            }

                            var args = new LuaValue[params.length];
                            for (int i = 0; i < params.length; i++) {
                                args[i] = toLuaValue(params[i]);
                            }

                            return toJava(state, func.invoke(state, ValueFactory.varargsOf(args)).first(), Object.class);
                        });
            } else {
                return value.checkUserdata(clatz);
            }
        } else if (value.isUserdata()) {
            var userData = value.checkUserdata();

            if (clatz.isAssignableFrom(userData.getClass())) {
                return value.checkUserdata();
            }
        }

        if (value.isNil()) {
            return null;
        }

        throw new InvalidArgumentException();
    }

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? out.getClass() : Object.class);
    }

    public static LuaValue toLuaValue(Object out, Class<?> ret) {
        if (out instanceof LuaValue) {
            return (LuaValue) out;
        }

        if (out != null && ret.isArray()) {
            var table = new LuaTable();
            int length = Array.getLength(out);
            for (int i = 1; i <= length; i++) {
                table.rawset(i, toLuaValue(Array.get(out, i)));
            }
            return table;
        } else if (out != null) {
            ret = Primitives.unwrap(ret);

            if (ret.isPrimitive()) {
                if (ret.equals(int.class)) { // int
                    return ValueFactory.valueOf((int) out);
                } else if (ret.equals(double.class)) { // double
                    return ValueFactory.valueOf((double) out);
                } else if (ret.equals(float.class)) { // float
                    return ValueFactory.valueOf((float) out);
                } else if (ret.equals(long.class)) { // long
                    return ValueFactory.valueOf((long) out);
                } else if (ret.equals(boolean.class)) { // boolean
                    return ValueFactory.valueOf((boolean) out);
                } else if (ret.equals(short.class)) { // short
                    return ValueFactory.valueOf((short) out);
                } else if (ret.equals(byte.class)) { // byte
                    return ValueFactory.valueOf((byte) out);
                } else if (ret.equals(char.class)) { // char
                    return ValueFactory.valueOf((char) out);
                }
            } else if (ret.equals(String.class)) { // string
                return ValueFactory.valueOf((String) out);
            } else if (ret.isAssignableFrom(out.getClass())) {
                return new UserdataFactory<>(ret).create(ret.cast(out));
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
        private final Class<T> clazz;
        private final List<Method> matches;

        public UDFFunctions(Class<T> clazz, List<Method> matches) {
            this.clazz = clazz;
            this.matches = matches;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
            StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                    this.matches.get(0).getName() + "\" for \"" + clazz.getName() + "\"" +
                    "\nThe following are correct argument types:\n"
            );

            try {
                T instance = args.arg(1).checkUserdata(clazz);
                for (Method method : matches) { // For each matched method from the index call
                    var parameters = method.getParameterTypes();
                    try {
                        var jargs = toJavaArguments(state, args, 2, parameters);

                        if (jargs.length == parameters.length) { // Found a match!
                            try { // Get the return type, invoke method, cast returned value, cry.
                                Class<?> ret = method.getReturnType();
                                Object out = method.invoke(instance, jargs);
                                return toLuaValue(out, ret);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new LuaError(e);
                            }
                        }
                    } catch (InvalidArgumentException | IllegalArgumentException e) {
                        var params = new StringBuilder();

                        for (var clazz : parameters) {
                            params.append(clazz.getName()).append(", ");
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
}
