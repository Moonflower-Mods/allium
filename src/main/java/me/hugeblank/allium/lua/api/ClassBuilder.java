package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.UserdataFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ClassBuilder {
    public static int id = 0;

    private final Class<?> superClass;
    private DynamicType.Builder<?> builder;
    private List<Method> methods = new ArrayList<>();

    public ClassBuilder(Class<?> superClass, Class<?>[] interfaces) {
        this.builder = new ByteBuddy().subclass(superClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS).implement(interfaces).name("allium.GeneratedClass_" + id);
        this.superClass = superClass;
        this.methods.addAll(List.of(this.superClass.getMethods()));
        for (var inrf : interfaces) {
            this.methods.addAll(List.of(inrf.getMethods()));
        }
        id++;
    }

    public void method(String methodName, Class<?>[] parameters, LuaState state, LuaFunction func) {
        var methods = new ArrayList<Method>();

        UserdataFactory.collectMethods(this.superClass, this.methods, methodName, methods::add);

        for (var method : methods) {
            var methParams = method.getParameterTypes();

            if (methParams.length == parameters.length) {
                boolean match = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (methParams[i] != parameters[i]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    this.builder = this.builder.defineMethod(method.getName(), method.getReturnType(), method.getModifiers() ^ Modifier.ABSTRACT)
                            .withParameters(methParams).intercept(InvocationHandlerAdapter.of((p, m, params) -> {
                        var args = new LuaValue[params.length + 1];
                        args[0] = UserdataFactory.toLuaValue(p);
                        for (int i = 1; i < params.length; i++) {
                            args[i] = UserdataFactory.toLuaValue(params[i]);
                        }

                        return UserdataFactory.toJava(state, func.invoke(state, ValueFactory.varargsOf(args)).first(), method.getReturnType());
                    }));
                    return;
                }
            }
        }
    }

    public Class<?> build() {
        return this.builder.make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
    }

    public static LuaTable createLua(Class<?> superClass, Class<?>[] interfaces) {
        var builder = new ClassBuilder(superClass, interfaces);


        return LibBuilder.create("javaclass")
                .add("withMethod", (state, args) -> {
                    try {
                        var methodName =  args.arg(2).checkString();
                        var paramsTable = args.arg(3).checkTable().checkTable();
                        var function = args.arg(4).checkFunction();

                        var params = new Class[paramsTable.length()];

                        for (int i = 0; i < paramsTable.length(); i++) {
                            var val = paramsTable.rawget(i + 1);
                            params[i] = val.isUserdata(Class.class) ? val.checkUserdata(Class.class) : JavaLib.getClassOf(val.checkString());
                        }

                        builder.method(methodName, params, state, function);
                        return Constants.NIL;
                    } catch (Exception e) {
                        if (e instanceof LuaError le) {
                            throw le;
                        } else {
                            throw new LuaError(e);
                        }
                    }
                })
                .add("build", (state, args) -> {
                    try {
                        return UserdataFactory.toLuaValue(builder.build());
                    } catch (Exception e) {
                        if (e instanceof LuaError le) {
                            throw le;
                        } else {
                            throw new LuaError(e);
                        }
                    }
                })
                .buildTable();
    }
}
