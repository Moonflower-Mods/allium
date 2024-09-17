// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.util.AnnotationUtils;
import dev.hugeblank.allium.util.ArgumentUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.property.EmptyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class UserdataFactory<T> {
    private static final ConcurrentMap<EClass<?>, UserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();
    private final Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
    private final EClass<T> clazz;
    private final LuaTable metatable;
    private @Nullable LuaTable boundMetatable;
    private final @Nullable EMethod indexImpl;
    private final @Nullable EMethod newIndexImpl;

    protected UserdataFactory(EClass<T> clazz) {
        this.clazz = clazz;
        this.indexImpl = tryFindOp(LuaIndex.class, 1, "get");
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

        metatable.rawset("__tostring", new OneArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
                try {
                    return TypeCoercions.toLuaValue(Objects.requireNonNull(TypeCoercions.toJava(state, arg, clazz)).toString());
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        metatable.rawset("__pairs", new OneArgFunction() {
            // Technically, pairs is kinda cringe. In order to properly deliver all key-value pairs, we have to parse
            // the ENTIRE class. At least we cache everything along the way? Still not ideal.
            @Override
            public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
                Stream.Builder<EMember> memberBuilder = Stream.builder();
                clazz.methods().forEach(memberBuilder);
                clazz.fields().forEach(memberBuilder);
                try {
                    var instance = clazz.cast(TypeCoercions.toJava(state, arg, clazz));
                    Stream<Varargs> valueStream = memberBuilder.build()
                            .filter((member)->
                                    !clazz.hasAnnotation(LuaWrapped.class) ||
                                            (
                                                    clazz.hasAnnotation(LuaWrapped.class) &&
                                                    member.hasAnnotation(LuaWrapped.class)
                                            )
                            )
                            .map((member)-> {
                                String memberName = member.name();
                                if (member.hasAnnotation(LuaWrapped.class)) {
                                    String[] names = AnnotationUtils.findNames(member);
                                    if (names != null && names.length > 0) {
                                        memberName = names[0];
                                    }
                                }
                                PropertyData<? super T> propertyData = cachedProperties.get(memberName);

                                if (propertyData == null) { // caching
                                    propertyData = PropertyResolver.resolveProperty(clazz, memberName, member.isStatic());
                                    cachedProperties.put(memberName, propertyData);
                                }

                                if (!Allium.DEVELOPMENT) memberName = Allium.MAPPINGS.getYarn(memberName);
                                try {
                                    return ValueFactory.varargsOf(LuaString.valueOf(memberName), propertyData.get(
                                            memberName,
                                            state,
                                            isBound ? instance : null,
                                            isBound
                                    ));
                                } catch (LuaError e) {
                                    // I have no idea how this could happen, so it'll be interesting if we get an issue
                                    // report in the future with it...
                                    Allium.LOGGER.warn("Could not get property data for " + memberName, e);
                                    return Constants.NIL;
                                }
                            });

                    Iterator<Varargs> iterator = valueStream.iterator();
                    return new VarArgFunction() { // next
                        public Varargs invoke(LuaState state, Varargs varargs) throws LuaError, UnwindThrowable {
                            if (!iterator.hasNext()) return Constants.NIL;
                            return iterator.next();
                        }
                    };
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                String name = arg2.checkString(); // mapped name

                if (name.equals("allium_java_class")) {
                    return UserdataFactory.of(EClass.fromJava(EClass.class)).create(clazz);
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, false);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && indexImpl != null) {
                    var parameters = indexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, arg2, 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = TypeCoercions.toJava(state, arg1, clazz);
                                EClassUse<?> ret = indexImpl.returnTypeUse().upperBound();
                                Object out = indexImpl.invoke(instance, jargs);
                                return TypeCoercions.toLuaValue(out, ret);
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
                return cachedProperty.get(name, state, arg1.checkUserdata(clazz.raw()), isBound);
            }
        });

        metatable.rawset("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
                String name = arg2.checkString(); // mapped name

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, false);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && newIndexImpl != null) {
                    var parameters = newIndexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, ValueFactory.varargsOf(arg1, arg2), 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = TypeCoercions.toJava(state, arg1, clazz);
                                newIndexImpl.invoke(instance, jargs);
                                return Constants.NIL;
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

    @SuppressWarnings("unchecked")
    public static <T> UserdataFactory<T> of(EClass<T> clazz) {
        return (UserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, UserdataFactory::new);
    }

    public static LuaUserdata getUserData(Object instance) {
        return FACTORIES.computeIfAbsent(EClass.fromJava(instance.getClass()), UserdataFactory::new).create(instance);
    }

    public LuaUserdata create(Object instance) {
        return new LuaUserdata(instance, metatable);
    }

    public LuaUserdata createBound(Object instance) {
        if (boundMetatable == null)
            boundMetatable = createMetatable(true);

        return new LuaUserdata(instance, boundMetatable);
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
