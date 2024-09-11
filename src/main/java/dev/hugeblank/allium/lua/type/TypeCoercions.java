package dev.hugeblank.allium.lua.type;

import dev.hugeblank.allium.lua.api.JavaLib;
import dev.hugeblank.allium.lua.type.annotation.CoerceToBound;
import dev.hugeblank.allium.lua.type.annotation.CoerceToNative;
import me.basiqueevangelist.enhancedreflection.api.*;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import net.minecraft.util.Identifier;
import org.squiddev.cobalt.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public class TypeCoercions {
    private static final Map<Class<?>, Function<EClass<?>, LuaToJavaConverter<?>>> FROM_LUA = new HashMap<>();
    private static final Map<Class<?>, Function<EClassUse<?>, JavaToLuaConverter<?>>> TO_LUA = new HashMap<>();

    public static <T> void registerJavaToLua(Class<T> klass, JavaToLuaConverter<T> serializer) {
        if (TO_LUA.put(klass, unused -> serializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerComplexJavaToLua(Class<T> klass, Function<EClassUse<T>, JavaToLuaConverter<T>> serializerFactory) {
        if (TO_LUA.put(klass, (Function<EClassUse<?>, JavaToLuaConverter<?>>)(Object) serializerFactory) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    public static <T> void registerLuaToJava(Class<T> klass, LuaToJavaConverter<T> deserializer) {
        if (FROM_LUA.put(klass, unused -> deserializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerLuaToJava(Class<T> klass, Function<EClass<T>, LuaToJavaConverter<T>> deserializerFactory) {
        if (FROM_LUA.put(klass, (Function<EClass<?>, LuaToJavaConverter<?>>)(Object) deserializerFactory) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
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

        var deserializerFactory = FROM_LUA.get(clatz.raw());
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

        var serializerFactory = TO_LUA.get(klass.raw());
        if (serializerFactory != null) {
            var serializer = (JavaToLuaConverter<Object>) serializerFactory.apply(ret);

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
                return new UDFFunctions(klass, Collections.singletonList(ifaceMethod), ifaceMethod.name(), out, false);
            } else {
                return UserdataFactory.of(klass).create(klass.cast(out));
            }
        } else if (klass.raw().isAssignableFrom(out.getClass())) {
            EClass<?> trueRet = EClass.fromJava(out.getClass());

            if (canMatch(trueRet, klass)) {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(trueRet).createBound(out);
                else
                    return UserdataFactory.of(trueRet).create(out);
            } else {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(klass).createBound(klass.cast(out));
                else
                    return UserdataFactory.of(klass).create(klass.cast(out));
            }
        } else {
            return Constants.NIL;
        }
    }

    private static boolean canMatch(EType type, EType other) {
        if (type.equals(other)) return true;

        if (type instanceof EClass<?> klass) {
            if (other instanceof EWildcard otherWildcard) {
                return otherWildcard.upperBounds().stream().allMatch(x -> canMatch(klass, x))
                    && otherWildcard.lowerBounds().stream().noneMatch(x -> canMatch(klass, x));
            } else if (other instanceof EClass<?> otherKlass) {
                if (otherKlass.raw().equals(klass.raw())) {
                    for (int i = 0; i < otherKlass.typeVariableValues().size(); i++)  {
                        var val = klass.typeVariableValues().get(i);
                        var otherVal = otherKlass.typeVariableValues().get(i);

                        if (otherVal instanceof EWildcard && canMatch(val, otherVal))
                            return true;
                    }
                }
            }

            if (klass.allSuperclasses().stream().anyMatch(x -> canMatch(x, other)))
                return true;

            if (klass.interfaces().stream().anyMatch(x -> canMatch(x, other)))
                return true;
        }

        return false;
    }

    static {
        TypeCoercions.registerJavaToLua(int.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(byte.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(short.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(char.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(double.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(float.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(long.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(boolean.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(String.class, ValueFactory::valueOf);

        TypeCoercions.registerComplexJavaToLua(List.class, use -> {
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

        TypeCoercions.registerComplexJavaToLua(Map.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> keyUse = use.typeVariableValues().get(0).upperBound();
            EClassUse<?> valueUse = use.typeVariableValues().get(1).upperBound();

            return map -> {
                LuaTable table = new LuaTable();

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                    table.rawset(TypeCoercions.toLuaValue(entry.getKey(), keyUse), TypeCoercions.toLuaValue(entry.getValue(), valueUse));
                }

                return table;
            };
        });

        TypeCoercions.registerLuaToJava(int.class, (state, val) -> val.isInteger() ? val.toInteger() : null);
        TypeCoercions.registerLuaToJava(byte.class, (state, val) -> val.isInteger() ? (byte)val.toInteger() : null);
        TypeCoercions.registerLuaToJava(short.class, (state, val) -> val.isInteger() ? (short)val.toInteger() : null);
        TypeCoercions.registerLuaToJava(char.class, (state, val) -> val.isInteger() ? (char)val.toInteger() : null);
        TypeCoercions.registerLuaToJava(double.class, (state, val) -> val.isNumber() ? val.toDouble() : null);
        TypeCoercions.registerLuaToJava(float.class, (state, val) -> val.isNumber() ? (float)val.toDouble() : null);
        TypeCoercions.registerLuaToJava(long.class, (state, val) -> val.isLong() ? val.toLong() : null);
        TypeCoercions.registerLuaToJava(boolean.class, (state, val) -> val.isBoolean() ? val.toBoolean() : null);
        TypeCoercions.registerLuaToJava(String.class, (state, val) -> val.isString() ? val.toString() : null);

        TypeCoercions.registerLuaToJava(EClass.class, (state, val) -> JavaLib.asClass(val));
        TypeCoercions.registerLuaToJava(Class.class, (state, val) -> {
            EClass<?> klass = JavaLib.asClass(val);
            if (klass == null) return null;
            else return klass.raw();
        });

        TypeCoercions.registerLuaToJava(List.class, klass -> {
            EClass<?> componentType = klass.typeVariableValues().get(0).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                List<Object> list = new ArrayList<>(length);

                for (int i = 0; i < length; i++) {
                    list.add(TypeCoercions.toJava(state, table.rawget(i + 1), componentType));
                }

                return list;
            };
        });

        TypeCoercions.registerLuaToJava(Map.class, klass -> {
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

                    map.put(TypeCoercions.toJava(state, k, keyType), TypeCoercions.toJava(state, v, valueType));
                }

                return map;
            };
        });

        TypeCoercions.registerLuaToJava(Identifier.class, (state, value) -> {
            if (!value.isString()) return null;

            return new Identifier(value.checkString());
        });
    }

}
