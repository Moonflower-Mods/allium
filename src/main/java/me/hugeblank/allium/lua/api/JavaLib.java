package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JavaLib {
    public static LuaLibrary create() {
        return LibBuilder.create("java")
                .add("create", JavaLib::createInstance)
                .add("getRawClass", JavaLib::getClassObject)
                .add("cast", JavaLib::cast)
                .build();
    }

    private static Varargs createInstance(LuaState state, Varargs args) throws LuaError {
        Class<?> clazz;
        try {
            clazz = Class.forName(args.arg(1).checkString());
        } catch (ClassNotFoundException e) {
            throw new LuaError(e.toString());
        }

        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.getConstructors()) {
            var parameters = constructor.getParameterTypes();
            if (args.count() - 1 == parameters.length) {
                try {
                    var jargs = UserdataFactory.toJavaArguments(state, args, 2, parameters);

                    if (jargs.length == parameters.length) { // Found a match!
                        try { // Get the return type, invoke method, cast returned value, cry.
                            Object out = constructor.newInstance(jargs);
                            return UserdataFactory.toLuaValue(out, clazz);
                        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                            throw new LuaError(e);
                        }
                    }
                } catch (UserdataFactory.InvalidArgumentException e) {
                    var params = new StringBuilder();

                    for (var clazzz : parameters) {
                        params.append(clazzz.getName()).append(", ");
                    }

                    paramList.add(params.toString());
                }
            }
        }

        StringBuilder error = new StringBuilder("Could not find parameter match for called constructor " +
                clazz.getName() +
                "\nThe following are correct argument types:\n"
        );

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }

    private static Varargs cast(LuaState state, Varargs args) throws LuaError {
        Class<?> clazz;
        try {
            clazz = Class.forName(args.arg(1).checkString());
        } catch (ClassNotFoundException e) {
            throw new LuaError(e.toString());
        }

        try {
            return UserdataFactory.toLuaValue(UserdataFactory.toJava(state, args.arg(2), clazz), Object.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.NIL;
        }
    }

    private static Varargs getClassObject(LuaState state, Varargs args) throws LuaError {
        Class<?> clazz;
        try {
            clazz = Class.forName(args.arg(1).checkString());
        } catch (ClassNotFoundException e) {
            throw new LuaError(e.toString());
        }

        return UserdataFactory.toLuaValue(clazz, Class.class);
    }
}
