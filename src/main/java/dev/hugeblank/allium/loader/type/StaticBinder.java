package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.property.EmptyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

public final class StaticBinder {
    private StaticBinder() {

    }

    public static LuaUserdata bindClass(EClass<?> clazz) {
        Map<String, PropertyData<?>> cachedProperties = new HashMap<>();
        GetClassFunction getClassFunc = new GetClassFunction(clazz);
        LuaTable metatable = new LuaTable();
        EMethod indexImpl = clazz.methods().stream().filter(x -> x.isStatic() && x.hasAnnotation(LuaIndex.class)).findAny().orElse(null);

        metatable.rawset("__pairs", new OneArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
                Stream.Builder<EMember> memberBuilder = Stream.builder();
                clazz.methods().forEach(memberBuilder);
                clazz.fields().forEach(memberBuilder);
                Stream<Varargs> valueStream = memberBuilder.build().filter((member)->
                        !clazz.hasAnnotation(LuaWrapped.class) ||
                                (
                                        clazz.hasAnnotation(LuaWrapped.class) &&
                                                member.hasAnnotation(LuaWrapped.class)
                                )
                ).map((member)-> {
                    String memberName = member.name();
                    if (member.hasAnnotation(LuaWrapped.class)) {
                        String[] names = AnnotationUtils.findNames(member);
                        if (names != null && names.length > 0) {
                            memberName = names[0];
                        }
                    }
                    PropertyData<?> propertyData = cachedProperties.get(memberName);

                    if (propertyData == null) { // caching
                        propertyData = PropertyResolver.resolveProperty(clazz, memberName, member.isStatic());
                        cachedProperties.put(memberName, propertyData);
                    }

                    if (!Allium.DEVELOPMENT) memberName = Allium.MAPPINGS.getYarn(memberName);
                    try {
                        return ValueFactory.varargsOf(LuaString.valueOf(memberName), propertyData.get(
                                memberName,
                                state,
                                null,
                                false
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
            }
        });

        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                if (arg2.isString()) {
                    String name = arg2.checkString(); // mapped name

                    if (name.equals("getClass")) {
                        return getClassFunc;
                    }

                    PropertyData<?> cachedProperty = cachedProperties.get(name);

                    if (cachedProperty == null) {
                        cachedProperty = PropertyResolver.resolveProperty(clazz, name, true);

                        cachedProperties.put(name, cachedProperty);
                    }

                    if (cachedProperty != EmptyData.INSTANCE)
                        return cachedProperty.get(name, state, null, false);
                }

                if (indexImpl != null) {
                    var parameters = indexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, arg2, 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = TypeCoercions.toJava(state, arg1, clazz);
                                EClassUse<?> ret = indexImpl.returnTypeUse().upperBound();
                                Object out = indexImpl.invoke(instance, jargs);
                                // If out is null, we can assume the index is nil
                                if (out == null) throw new InvalidArgumentException();
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

                if (arg2.isTable()) {
                    LuaTable table = arg2.checkTable();
                    EClass<?>[] typeArgs = new EClass[table.getArrayLength()];

                    for (int i = 0; i < typeArgs.length; i++) {
                        typeArgs[i] = JavaHelpers.asClass(table.rawget(i + 1));
                    }

                    try {
                        return bindClass(clazz.instantiateWith(List.of(typeArgs)));
                    } catch (IllegalArgumentException e) {
                        throw new LuaError(e);
                    }
                }

                return Constants.NIL;
            }
        });

        metatable.rawset("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
                String name = arg2.checkString(); // mapped name

                PropertyData<?> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, false);

                    cachedProperties.put(name, cachedProperty);
                }

                cachedProperty.set(name, state, null, arg3);

                return Constants.NIL;
            }
        });

        metatable.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return createInstance(
                        clazz,
                        state,
                        args.subargs(2)
                );
            }
        });

        return new LuaUserdata(clazz, metatable);
    }

    private static Varargs createInstance(EClass<?> clazz, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.constructors()) {
            if (AnnotationUtils.isHiddenFromLua(constructor)) continue;

            var parameters = constructor.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, args, 1, parameters);

                try { // Get the return type, invoke method, cast returned value, cry.
                    EClassUse<?> ret = (EClassUse<?>) constructor.receiverTypeUse();

                    if (ret == null) ret = clazz.asEmptyUse();

                    Object out = constructor.invoke(jargs);
                    return TypeCoercions.toLuaValue(out, ret);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new LuaError(e);
                }
            } catch (InvalidArgumentException e) {
                paramList.add(ArgumentUtils.paramsToPrettyString(parameters));
            }
        }

        StringBuilder error = new StringBuilder("Could not find parameter match for called constructor " +
                clazz.name() +
                "\nThe following are correct argument types:\n"
        );

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }

    private static class GetClassFunction extends ZeroArgFunction {
        private final EClass<?> clazz;

        public GetClassFunction(EClass<?> clazz) {
            super();
            this.clazz = clazz;
        }

        @Override
        public LuaValue call(LuaState state) {
            return TypeCoercions.toLuaValue(clazz, EClass.fromJava(EClass.class).instantiateWith(List.of(clazz)));
        }
    }
}