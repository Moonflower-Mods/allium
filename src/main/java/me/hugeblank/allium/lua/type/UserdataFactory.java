// Eldritch horrors, sponsored by hugeblank!
// This class converts all public methods from any class from Java -> Lua.
// It's one saving grace is that I've restricted it from chewing through *every* class.
// The fact that it could though... *shivers* euhh. I'm so sorry.
// If someone wants to SCP this, please by all means do so.
package me.hugeblank.allium.lua.type;

import me.hugeblank.allium.Allium;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserdataFactory<T> {
    private final Class<T> clazz;
    private final List<Method> methods;
    private final LuaTable metatable = new LuaTable();

    public UserdataFactory(Class<T> clazz) {
        this.clazz = clazz;
        this.methods = Arrays.asList(clazz.getMethods());
    }

    public LuaUserdata create(Object instance) {
        return new LuaUserdata(instance, metatable);
    }

    {
        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                String name = arg2.checkString(); // mapped name
                List<Method> matches = new ArrayList<>(); // intermediary method (in production [oh god.])
                    methods.forEach((method -> {
                        if (Allium.DEVELOPMENT) {
                            // Fun fact! Allium runs better in a dev environment!
                            // See below for more information.
                            if (method.getName().equals(name)) {
                                matches.add(method);
                            }
                        } else {
                            // This is tragic but a necessary evil
                            // methods can have the same mapped name, but the intermediary value changes
                            // O(n^(go f*ck yourself))
                            Allium.MAPPINGS.forEach((key, value) -> {
                                if (name.equals(value)) {
                                    matches.add(method);
                                }
                            });
                        }
                    }));
                if (matches.size() > 0) return new UDFFunctions<>(clazz, matches);
                return Constants.NIL;
            }
        });
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
            T instance = args.arg(1).checkUserdata(clazz);
            List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
            for (Method method : matches) { // For each matched method from the index call
                StringBuilder paramString = new StringBuilder();
                int ind = 2;
                Class<?>[] parameters = method.getParameterTypes();
                Object[] arguments = new Object[parameters.length];
                for (Class<?> clatz : parameters) { // For each parameter in the matched call
                    if (clatz.isArray()) {
                        if (!args.arg(ind).isTable())
                            throw new LuaError(
                                    "Expected table of "
                                    + clatz.getComponentType()
                                    + "s, got "
                                    + args.arg(ind).typeName()
                            );
                        LuaTable table = args.arg(ind).checkTable();
                        Object[] arr = new Object[table.getArrayLength()];
                        if (clatz.getComponentType().isPrimitive()) {
                            if (clatz.getComponentType().equals(int.class)) { // int
                                for (int i = 1; i <= arr.length; i++) {
                                    arr[i] = table.rawget(i).checkInteger();
                                }
                            } else if (clatz.getComponentType().equals(double.class)) { // double
                                for (int i = 1; i <= arr.length; i++) {
                                    arr[i] = table.rawget(i).checkDouble();
                                }
                            } else if (clatz.getComponentType().equals(long.class)) { // long
                                for (int i = 1; i <= arr.length; i++) {
                                    arr[i] = table.rawget(i).checkLong();
                                }
                            } else if (clatz.getComponentType().equals(boolean.class)) { // boolean
                                for (int i = 1; i <= arr.length; i++) {
                                    arr[i] = table.rawget(i).checkBoolean();
                                }
                            }
                        } else if (clatz.getComponentType().equals(String.class)) { // string
                            for (int i = 1; i <= arr.length; i++) {
                                arr[i] = table.rawget(i).checkString();
                            }
                        } else { // Is the argument provided by user of the right type?
                            for (int i = 1; i <= arr.length; i++) {
                                arr[i] = table.rawget(i).checkUserdata(clatz.getComponentType());
                            }
                        }
                        arguments[ind-2] = clatz.cast(arr);
                        ind++;
                    } else {
                        if (clatz.isPrimitive()) {
                            if (args.arg(ind).isInteger() && clatz.equals(int.class)) { // int
                                arguments[ind-2] = args.arg(ind).toInteger();
                                ind++;
                            } else if (args.arg(ind).isNumber() && clatz.equals(double.class)) { // double
                                arguments[ind-2] = args.arg(ind).toDouble();
                                ind++;
                            } else if (args.arg(ind).isLong() && clatz.equals(long.class)) { // long
                                arguments[ind-2] = args.arg(ind).toLong();
                                ind++;
                            } else if (args.arg(ind).isBoolean() && clatz.equals(boolean.class)) { // boolean
                                arguments[ind-2] = args.arg(ind).toBoolean();
                                ind++;
                            }
                        } else if (args.arg(ind).isString() && clatz.equals(String.class)) { // string
                            arguments[ind-2] = args.arg(ind).toString();
                            ind++;
                        } else if (args.arg(ind).isUserdata(clatz)) { // Is the argument provided by user of the right type?
                            arguments[ind-2] = args.arg(ind).checkUserdata(clatz);
                            ind++;
                        }
                    }
                    if (ind-1 == parameters.length) {
                        // Prepare for the worst by creating a string of
                        // these parameters in case there's no header match
                        paramString.append(clatz.getName());
                    } else {
                        paramString.append(clatz.getName()).append(", ");
                    }
                }
                paramList.add(paramString.toString());
                if (ind-2 == parameters.length) { // Found a match!
                    try { // Get the return type, invoke method, cast returned value, cry.
                        Class<?> ret = method.getReturnType();
                        Object out = method.invoke(instance, arguments);
                        if (out != null && ret.isArray()) {
                            Object[] outArr = (Object[]) out;
                            LuaValue[] luaArr = new LuaValue[outArr.length];
                            if (ret.getComponentType().isPrimitive()) {
                                if (ret.getComponentType().equals(int.class)) { // int
                                    for (int i = 1; i <= outArr.length; i++) {
                                        luaArr[i] = ValueFactory.valueOf((int)outArr[i]);
                                    }
                                } else if (ret.getComponentType().equals(double.class)) { // double
                                    for (int i = 1; i <= outArr.length; i++) {
                                        luaArr[i] = ValueFactory.valueOf((double)outArr[i]);
                                    }
                                } else if (ret.getComponentType().equals(long.class)) { // long
                                    for (int i = 1; i <= outArr.length; i++) {
                                        luaArr[i] = ValueFactory.valueOf((long)outArr[i]);
                                    }
                                } else if (ret.getComponentType().equals(boolean.class)) { // boolean
                                    for (int i = 1; i <= outArr.length; i++) {
                                        luaArr[i] = ValueFactory.valueOf((boolean)outArr[i]);
                                    }
                                }
                            } else if (ret.getComponentType().equals(String.class)) { // string
                                for (int i = 1; i <= outArr.length; i++) {
                                    luaArr[i] = ValueFactory.valueOf((String)outArr[i]);
                                }
                            } else { // Is the argument provided by user of the right type?
                                for (int i = 1; i <= outArr.length; i++) {
                                    if (
                                            ret.getComponentType().isAssignableFrom(outArr[i].getClass()) &&
                                            UserdataTypes.TYPES.containsKey(ret.getComponentType())
                                    ) {
                                        luaArr[i] = UserdataTypes.TYPES.get(ret).create(ret.cast(out));
                                    } else {
                                        for (Map.Entry<Class<?>, UserdataFactory<?>> udata : UserdataTypes.TYPES.entrySet()) {
                                            if (udata.getKey().isAssignableFrom(outArr[i].getClass())) {
                                                luaArr[i] = udata.getValue().create(outArr[i]);
                                            } else {
                                                throw new LuaError("Could not find valid userdata return for table element " + outArr[i].getClass().getName());
                                            }
                                        }
                                    }
                                }
                            }
                            // TODO: Does this return the array of values in Lua or an array of the array?
                            return ValueFactory.listOf(luaArr);
                        } else {
                            if (out != null && ret.isPrimitive()) {
                                if (ret.equals(int.class)) { // int
                                    return ValueFactory.valueOf((int) out);
                                } else if (ret.equals(double.class)) { // double
                                    return ValueFactory.valueOf((double) out);
                                } else if (ret.equals(long.class)) { // long
                                    return ValueFactory.valueOf((long) out);
                                } else if (ret.equals(boolean.class)) { // boolean
                                    return ValueFactory.valueOf((boolean) out);
                                }
                            } else if (out != null && ret.equals(String.class)) { // string
                                return ValueFactory.valueOf((String) out);
                            } else if (out != null && ret.isAssignableFrom(out.getClass()) && UserdataTypes.TYPES.containsKey(ret)) {
                                return UserdataTypes.TYPES.get(ret).create(ret.cast(out));
                            } else if (out != null) { // On the off chance this object can be cast to an unlocked class
                                // Why would anyone let me have access to a computer
                                for (Map.Entry<Class<?>, UserdataFactory<?>> udata : UserdataTypes.TYPES.entrySet()) {
                                    if (udata.getKey().isAssignableFrom(out.getClass())) {
                                        return udata.getValue().create(out);
                                    }
                                }
                                // Admit defeat
                                throw new LuaError("Could not find valid userdata return for " + ret.getName());
                            } else {
                                return Constants.NIL;
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new LuaError(e);
                    }
                }
            }
            StringBuilder error = new StringBuilder("Could not find parameter match for called function " +
                    this.matches.get(0).getName() +
                    "\nThe following are correct argument types:\n"
            );
            for (String headers : paramList) {
                error.append(headers).append("\n");
            }
            throw new LuaError(error.toString());
        }
    }
}
