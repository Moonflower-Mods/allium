package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.UserdataFactory;
import me.hugeblank.allium.util.Mappings;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JavaLib {
    // TODO: Optionally provide userdata as the first argument for the class in most methods.
    public static LuaLibrary create() {
        return LibBuilder.create("java")
                .add("import", JavaLib::importClass)
                .add("create", JavaLib::createInstance)
                .add("getRawClass", JavaLib::getClassObject)
                .add("exists", JavaLib::checkIfExists)
                .add("cast", JavaLib::cast)
                .add("toYarn", JavaLib::toYarn)
                .add("fromYarn", JavaLib::fromYarn)
                .add("split", JavaLib::split)
                .add("classBuilder", JavaLib::classBuilder)
                .build();
    }

    private static Varargs importClass(LuaState state, Varargs args) throws LuaError {
        Class<?> clazz = getClassOf(args.arg(1).checkString());
        List<Method> staticMethods = new ArrayList<>();
        List<Field> staticFields = new ArrayList<>();

        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            int mods = declaredMethod.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && !Modifier.isAbstract(mods)) {
                staticMethods.add(declaredMethod);
            }
        }

        for (Field declaredField : clazz.getDeclaredFields()) {
            int mods = declaredField.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && !Modifier.isAbstract(mods)) {
                staticFields.add(declaredField);
            }
        }

        LuaTable mt = new LuaTable();
        mt.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return JavaLib.createInstance(
                        state,
                        ValueFactory.varargsOf( LuaString.valueOf(clazz.getName()), args.subargs(2))
                );
            }
        });

        LuaTable impord = new StaticMethods(clazz, staticMethods, staticFields).create();
        impord.setMetatable(mt);

        return ValueFactory.varargsOf(impord);
    }

    private static Varargs invokeStatic(Class<?> clazz, String name, Method[] methods, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
        StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                name + "\" for \"" + clazz.getName() + "\"" +
                "\nThe following are correct argument types:\n"
        );
        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        for (var method : methods) {
            var parameters = method.getParameterTypes();
            try {
                var jargs = UserdataFactory.toJavaArguments(state, args, 1, parameters);
                if (jargs.length == parameters.length) { // Found a match!
                    try { // Get the return type, invoke method, cast returned value, cry.
                        Class<?> ret = method.getReturnType();
                        method.setAccessible(true); // throws InaccessibleObjectException | SecurityException
                        Object out = method.invoke(null, jargs);
                        return UserdataFactory.toLuaValue(out, ret);
                    } catch (InaccessibleObjectException | SecurityException | IllegalAccessException | InvocationTargetException e) {
                        throw new LuaError(e);
                    }
                }
            } catch (UserdataFactory.InvalidArgumentException e) {
                var params = new StringBuilder();

                for (var clatz : parameters) {
                    params.append(clatz.getName()).append(", ");
                }

                paramList.add(params.toString());
            }
        }

        throw new LuaError(error.toString());
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
        Class<?> clazz = args.arg(1).isUserdata(Class.class) ? args.arg(1).checkUserdata(Class.class) : getClassOf(args.arg(1).checkString());

        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.getConstructors()) {
            var parameters = constructor.getParameterTypes();
            if (args.count() - 1 == parameters.length) {
                try {
                    var jargs = UserdataFactory.toJavaArguments(state, args, 2, parameters);

                    if (jargs.length == parameters.length) { // Found a match!
                        try { // Get the return type, invoke method, cast returned value, cry.
                            Object out = constructor.newInstance(jargs);
                            return UserdataFactory.toLuaValue(out);
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
            return UserdataFactory.toLuaValue(UserdataFactory.toJava(state, args.arg(2), getClassOf(args.arg(1).checkString())));
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.NIL;
        }
    }

    private static Varargs getClassObject(LuaState state, Varargs args) throws LuaError {
        return UserdataFactory.toLuaValue(getClassOf(args.arg(1).checkString()));
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


    private static Varargs classBuilder(LuaState state, Varargs args) throws LuaError {
        var clazz = args.arg(1).isUserdata(Class.class) ? args.arg(1).checkUserdata(Class.class) : getClassOf(args.arg(1).checkString());
        var table = args.arg(2).checkTable();
        var interfaces = new ArrayList<Class>();
        for (int i = 1; i <= table.length(); i++) {
            var luaValue = table.rawget(i);
            interfaces.add(luaValue.isUserdata(Class.class) ? args.arg(1).checkUserdata(Class.class) : getClassOf(luaValue.checkString()));
        }

        return ClassBuilder.createLua(clazz, interfaces.toArray(new Class[0]));
    }

    public static Class<?> getClassOf(String className) throws LuaError {
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

    private static class StaticMethods {
        private final Class<?> clazz;
        private final Method[][] methods;
        private final String[] methodNames;
        private final Field[] fields;

        public StaticMethods(Class<?> clazz, List<Method> methods, List<Field> fields) {
            this.clazz = clazz;
            var methodMap = new HashMap<String, List<Method>>();

            for (var method : methods) {
                methodMap.computeIfAbsent(
                        Allium.MAPPINGS.getYarn(Mappings.asMethod(this.clazz.getName(), method.getName())).split("#")[1],
                        (s) -> new ArrayList<>()
                ).add(method);
            }
            this.methods = new Method[methodMap.size()][];
            this.methodNames = new String[methodMap.size()];

            int i = 0;
            for (var entry : methodMap.entrySet()) {
                this.methodNames[i] = entry.getKey();
                this.methods[i] = entry.getValue().toArray(new Method[0]);
                i++;
            }

            this.fields = fields.toArray(new Field[0]);
        }

        public LuaTable create() {
            LuaTable tbl = new LuaTable();
            String[] arr = new String[this.methodNames.length + 1];

            for (int i = 0; i < this.methodNames.length; i++) {
                arr[i] = this.methodNames[i];
            }

            arr[this.methods.length] = "getClass";

            LibFunction.bind(tbl, FunctionImpl::new, arr);

            for (var field : fields) {
                try {
                    tbl.rawset(Mappings.asMethod(this.clazz.getName(), field.getName()).split("#")[1], UserdataFactory.toLuaValue(field.get(null)));
                } catch (Exception e) {}
            }
            return tbl;
        }

        private final class FunctionImpl extends VarArgFunction {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                if (methods.length == opcode) {
                    return UserdataFactory.toLuaValue(clazz);
                } else {
                    return invokeStatic(clazz, this.name, methods[opcode], state, args);
                }
            }
        }
    }


}
