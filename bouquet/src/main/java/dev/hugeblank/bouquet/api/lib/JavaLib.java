package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.*;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.util.AnnotationUtils;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@LuaWrapped(name = "java")
public class JavaLib implements WrappedLuaLibrary {

    @LuaWrapped(name = "import")
    public static LuaValue importClass(EClass<?> clazz) {
        return StaticBinder.bindClass(clazz);
    }

    // TODO: Is this used anywhere?
    private static Varargs invokeStatic(EClass<?> clazz, String name, EMethod[] methods, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
        StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                name + "\" for \"" + clazz.name() + "\"" +
                "\nThe following are correct argument types:\n"
        );

        for (var method : methods) {
            var parameters = method.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, args, 1, parameters);
                if (jargs.length == parameters.size()) { // Found a match!
                    try { // Get the return type, invoke method, cast returned value, cry.
                        EClassUse<?> ret = method.returnTypeUse().upperBound();
//                        method.setAccessible(true); // throws InaccessibleObjectException | SecurityException
                        Object out = method.invoke(null, jargs);
                        return TypeCoercions.toLuaValue(out, ret);
                    } catch (InaccessibleObjectException | SecurityException | IllegalAccessException e) {
                        throw new LuaError(e);
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof LuaError err)
                            throw err;

                        throw new LuaError(e);
                    }
                }
            } catch (InvalidArgumentException e) {
                paramList.add(ArgumentUtils.paramsToPrettyString(parameters));
            }
        }

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }

    // TODO: consider merging this with the Lua string library
    @LuaWrapped
    public static String[] split(String strToSplit, String delimiter) {
        return strToSplit.split(delimiter);
    }

    @LuaWrapped
    public static String toYarn(String string) {
        return Allium.MAPPINGS.getYarn(string);
    }

    @LuaWrapped
    public static @CoerceToNative List<String> fromYarn(String string) {
        return Allium.MAPPINGS.getIntermediary(string);
    }

    // TODO: Is this used anywhere?
    private static Varargs createInstance(LuaState state, EClass<?> clazz, Varargs args) throws LuaError {
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

    @LuaWrapped
    public static LuaValue cast(@LuaStateArg LuaState state, EClass<?> klass, LuaUserdata object) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, object, klass), klass);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
            return Constants.NIL;
        }
    }

    @LuaWrapped
    public static boolean exists(String string, @OptionalArg Class<?>[] value) {
        try {
            var parts = string.split("#");
            var clazz = getRawClass(parts[0]);

            if (parts.length != 2) {
                return true;
            }

            if (value != null) {
                return clazz.method(parts[1], value) != null;
            } else {
                for (var method : clazz.methods()) {
                    if (method.name().equals(parts[1])) {
                        return true;
                    }
                }

                return clazz.field(parts[1]) != null;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @LuaWrapped
    public static ClassBuilder extendClass(@LuaStateArg LuaState state, EClass<?> superclass, List<EClass<?>> interfaces) {
        return new ClassBuilder(superclass, interfaces, state);
    }

    @LuaWrapped
    public static EClass<?> getRawClass(String className) throws LuaError {
        return JavaHelpers.getRawClass(className);

    }

}
