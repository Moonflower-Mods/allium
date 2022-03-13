package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JavaLib {
    public static LuaLibrary create() {
        return LibBuilder.create("java")
                .add("create", JavaLib::createInstance)
                .add("getRawClass", JavaLib::getClassObject)
                .add("exists", JavaLib::checkIfExists)
                .add("cast", JavaLib::cast)
                .add("toYarn", JavaLib::toYarn)
                .add("fromYarn", JavaLib::fromYarn)
                .add("split", JavaLib::split)
                .build();
    }

    private static Varargs split(LuaState state, Varargs args) throws LuaError {
        var array = args.arg(1).checkString().split(args.arg(2).checkString());

        var table = new LuaTable();

        for (int i = 0; i < array.length; i++) {
            table.rawset(i + 1, LuaString.valueOf(array[i]));
        }
        return table;
    }

    private static Varargs toYarn(LuaState state, Varargs args) throws LuaError {
        var string = args.arg(1).checkString();

        return LuaString.valueOf(Allium.MAPPINGS.getYarn(string));
    }

    private static Varargs fromYarn(LuaState state, Varargs args) throws LuaError {
        var string = args.arg(1).checkString();
        var mappings = Allium.MAPPINGS.getIntermediary(string);
        int size = mappings.size();
        var array = new LuaTable();

        for (int i = 0; i < size; i++) {
            array.rawset(i + 1, LuaString.valueOf(mappings.get(i)));
        }
        return array;
    }

    private static Varargs createInstance(LuaState state, Varargs args) throws LuaError {
        Class<?> clazz = getClassOf(args.arg(1).checkString());

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
        try {
            return UserdataFactory.toLuaValue(UserdataFactory.toJava(state, args.arg(2), getClassOf(args.arg(1).checkString())), Object.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.NIL;
        }
    }

    private static Varargs getClassObject(LuaState state, Varargs args) throws LuaError {
        return UserdataFactory.toLuaValue(getClassOf(args.arg(1).checkString()), Class.class);
    }

    private static Varargs checkIfExists(LuaState state, Varargs args) {
        try {
            var string = args.arg(1).checkString().split("#");
            var clazz = getClassOf(string[0]);

            if (string.length == 2) {
                if (args.count() == 2) {
                    clazz.getMethod(string[1], (Class[]) UserdataFactory.toJava(state, args.arg(2), Class[].class));
                } else {
                    for (var method : clazz.getMethods()) {
                        if (method.getName().equals(string[1])) {
                            return Constants.TRUE;
                        }
                    }

                    clazz.getField(string[1]);
                }
            }

            return Constants.TRUE;
        } catch (Throwable t) {
            return Constants.FALSE;
        }
    }

    private static Class<?> getClassOf(String className) throws LuaError {
        try {
            return Class.forName(Allium.MAPPINGS.getIntermediary(className).get(0));
        } catch (ClassNotFoundException e1) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new LuaError(e.toString());
            }
        }
    }


}
