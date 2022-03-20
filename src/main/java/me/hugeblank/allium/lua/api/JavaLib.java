package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.*;
import me.hugeblank.allium.util.Mappings;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaLib {
    private static final String[] AUTO_COMPLETE = new String[]{
            "",
            "java.util.",
            "java.lang.",
            "net.minecraft.",
            "net.minecraft.item.",
            "net.minecraft.block.",
            "net.minecraft.entity.",
            "net.minecraft.entity.player.",
            "net.minecraft.inventory.",
            "net.minecraft.nbt.",
            "net.minecraft.potion.",
            "net.minecraft.sound.",
            "net.minecraft.text.",
            "net.minecraft.tag.",
            "net.minecraft.village.",
            "net.minecraft.world.",
            "net.minecraft.util.",
            "net.minecraft.util.registry.",
            "net.minecraft.server.",
            "net.minecraft.server.world.",
            "net.minecraft.server.network.",
            "com.mojang."
    };

    private static final Map<String, String> CACHED_AUTO_COMPLETE = new HashMap<>();

    // TODO: Optionally provide userdata as the first argument for the class in most methods.
    @HideFromLua
    public static LuaLibrary create() {
        return (state, env) -> {
            LuaValue lib = JavaLib.importClass(EClass.fromJava(JavaLib.class));

            env.rawset("java", lib);
            state.loadedPackages.rawset("java", lib);

            return lib;
        };
    }

    @LuaName("import")
    public static LuaValue importClass(EClass<?> clazz) {
        List<EMethod> staticMethods = new ArrayList<>();
        List<EField> staticFields = new ArrayList<>();

        for (EMethod declaredMethod : clazz.declaredMethods()) {
            if (declaredMethod.isPublic() && declaredMethod.isStatic() && !declaredMethod.isAbstract() && !declaredMethod.hasAnnotation(HideFromLua.class)) {
                staticMethods.add(declaredMethod);
            }
        }

        for (EField declaredField : clazz.declaredFields()) {
            if (declaredField.isPublic() && declaredField.isStatic() && !declaredField.isAbstract() && !declaredField.hasAnnotation(HideFromLua.class)) {
                staticFields.add(declaredField);
            }
        }

        LuaTable mt = new LuaTable();
        mt.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return JavaLib.createInstance(
                        state,
                        ValueFactory.varargsOf(UserdataFactory.toLuaValue(clazz), args.subargs(2))
                );
            }
        });
        mt.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                LuaTable table = arg2.checkTable();
                EClass<?>[] typeArgs = new EClass[table.getArrayLength()];

                for (int i = 0; i < typeArgs.length; i++) {
                    typeArgs[i] = asClass(table.rawget(i + 1));
                }

                try {
                    return importClass(clazz.instantiateWith(List.of(typeArgs)));
                } catch (IllegalArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        LuaTable impord = new StaticMethods(clazz, staticMethods, staticFields).create();
        impord.setMetatable(mt);

        return impord;
    }

    private static Varargs invokeStatic(EClass<?> clazz, String name, EMethod[] methods, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
        StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
                name + "\" for \"" + clazz.name() + "\"" +
                "\nThe following are correct argument types:\n"
        );

        for (var method : methods) {
            var parameters = method.parameters();
            try {
                var jargs = UserdataFactory.toJavaArguments(state, args, 1, parameters);
                if (jargs.length == parameters.size()) { // Found a match!
                    try { // Get the return type, invoke method, cast returned value, cry.
                        EClass<?> ret = method.returnType().upperBound();
//                        method.setAccessible(true); // throws InaccessibleObjectException | SecurityException
                        Object out = method.invoke(null, jargs);
                        return UserdataFactory.toLuaValue(out, ret);
                    } catch (InaccessibleObjectException | SecurityException | IllegalAccessException e) {
                        throw new LuaError(e);
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof LuaError err)
                            throw err;

                        throw new LuaError(e);
                    }
                }
            } catch (UserdataFactory.InvalidArgumentException e) {
                paramList.add(UserdataFactory.paramsToPrettyString(parameters));
            }
        }

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }

    // TODO: merge this with the Lua string library
    public static String[] split(String strToSplit, String delimiter) throws LuaError {
        return strToSplit.split(delimiter);
    }

    public static String toYarn(String string) {
        return Allium.MAPPINGS.getYarn(string);
    }

    public static List<String> fromYarn(String string) {
        return Allium.MAPPINGS.getIntermediary(string);
    }

    private static Varargs createInstance(LuaState state, Varargs args) throws LuaError {
        EClass<?> clazz = asClass(args.arg(1));

        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.constructors()) {
            if (constructor.hasAnnotation(HideFromLua.class)) continue;

            var parameters = constructor.parameters();
            if (args.count() - 1 == parameters.size()) {
                try {
                    var jargs = UserdataFactory.toJavaArguments(state, args, 2, parameters);

                    if (jargs.length == parameters.size()) { // Found a match!
                        try { // Get the return type, invoke method, cast returned value, cry.
                            Object out = constructor.invoke(jargs);
                            return UserdataFactory.toLuaValue(out, clazz);
                        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                            throw new LuaError(e);
                        }
                    }
                } catch (UserdataFactory.InvalidArgumentException e) {
                    paramList.add(UserdataFactory.paramsToPrettyString(parameters));
                }
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

    public static LuaValue cast(@LuaStateArg LuaState state, EClass<?> klass, LuaUserdata object) throws LuaError {
        try {
            return UserdataFactory.toLuaValue(UserdataFactory.toJava(state, object, klass));
        } catch (UserdataFactory.InvalidArgumentException e) {
            e.printStackTrace();
            return Constants.NIL;
        }
    }

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

    public static LuaTable extendClass(@LuaStateArg LuaState state, EClass<?> superclass, List<EClass<?>> interfaces) {
        return ClassBuilder.createLua(superclass, interfaces.toArray(new EClass[0]), state);
    }

    public static EClass<?> getRawClass(String className) throws LuaError {
        var cachedClassName = CACHED_AUTO_COMPLETE.get(className);

        if (cachedClassName != null) {
            try {
                return EClass.fromJava(Class.forName(cachedClassName));
            } catch (ClassNotFoundException e1) {

            }
        }

        for (var auto : AUTO_COMPLETE) {
            try {
                cachedClassName = Allium.MAPPINGS.getIntermediary(auto + className).get(0);
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException e1) {

            }

            try {
                cachedClassName = auto + className;
                var clazz = EClass.fromJava(Class.forName(cachedClassName));
                CACHED_AUTO_COMPLETE.put(className, cachedClassName);
                return clazz;
            } catch (ClassNotFoundException e) {

            }
        }

        throw new LuaError("Couldn't find class \"" + className + "\"");

    }

    @SuppressWarnings("unchecked")
    @HideFromLua
    public static EClass<?> asClass(LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(value.checkString());
        } else if (value.isUserdata(EClass.class)) {
            return value.checkUserdata(EClass.class);
        } else if (value.isUserdata(Class.class)) {
            return EClass.fromJava(value.checkUserdata(Class.class));
        } else if (value.isUserdata()) {
            return EClass.fromJava(value.checkUserdata().getClass());
        } else if (value.isTable() && value.checkTable().rawget("allium_java_class") != Constants.NIL) {
            return value.checkTable().rawget("allium_java_class").checkUserdata(EClass.class);
        } else if (value.isNil()) {
            return null;
        }

        throw new LuaError(new ClassNotFoundException());
    }

    private static class StaticMethods {
        private final EClass<?> clazz;
        private final EMethod[][] methods;
        private final String[] methodNames;
        private final EField[] fields;

        public StaticMethods(EClass<?> clazz, List<EMethod> methods, List<EField> fields) {
            this.clazz = clazz;
            var methodMap = new HashMap<String, List<EMethod>>();

            for (var method : methods) {
                methodMap.computeIfAbsent(
                        Allium.MAPPINGS.getYarn(Mappings.asMethod(this.clazz.name(), method.name())).split("#")[1],
                        (s) -> new ArrayList<>()
                ).add(method);

                LuaName luaName = method.annotation(LuaName.class);
                if (luaName != null) {
                    for (String altName : luaName.value()) {
                        methodMap.computeIfAbsent(altName, unused -> new ArrayList<>()).add(method);
                    }
                }
            }
            this.methods = new EMethod[methodMap.size()][];
            this.methodNames = new String[methodMap.size()];

            int i = 0;
            for (var entry : methodMap.entrySet()) {
                this.methodNames[i] = entry.getKey();
                this.methods[i] = entry.getValue().toArray(new EMethod[0]);
                i++;
            }

            this.fields = fields.toArray(new EField[0]);
        }

        public LuaTable create() {
            LuaTable tbl = new LuaTable();
            String[] arr = new String[this.methodNames.length + 1];

            System.arraycopy(this.methodNames, 0, arr, 0, this.methodNames.length);

            arr[this.methods.length] = "getClass";

            LibFunction.bind(tbl, FunctionImpl::new, arr);

            for (var field : fields) {
                try {
                    LuaName luaName = field.annotation(LuaName.class);
                    if (luaName != null) {
                        for (String altName : luaName.value()) {
                            if (tbl.rawget(altName) == Constants.NIL) {
                                tbl.rawset(altName, UserdataFactory.toLuaValue(field.get(null), field.fieldType().upperBound()));
                            } else {
                                tbl.rawset("f_" + altName, UserdataFactory.toLuaValue(field.get(null), field.fieldType().upperBound()));
                            }
                        }
                    } else {
                        var fieldName = Allium.MAPPINGS.getYarn(Mappings.asMethod(this.clazz.name(), field.name())).split("#")[1];

                        if (tbl.rawget(fieldName) == Constants.NIL) {
                            tbl.rawset(fieldName, UserdataFactory.toLuaValue(field.get(null), field.fieldType().upperBound()));
                        } else {
                            tbl.rawset("f_" + fieldName, UserdataFactory.toLuaValue(field.get(null), field.fieldType().upperBound()));
                        }
                    }


                } catch (Exception e) {
                }
            }

            tbl.rawset("allium_java_class", UserdataFactory.toLuaValue(this.clazz));
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
