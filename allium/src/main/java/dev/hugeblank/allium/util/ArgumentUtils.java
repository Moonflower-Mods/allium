package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.annotation.LuaArgs;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.squiddev.cobalt.*;

import java.util.List;

public class ArgumentUtils {
    public static Object[] toJavaArguments(LuaState state, Varargs args, final int offset, List<EParameter> parameters) throws LuaError, InvalidArgumentException {
        Object[] arguments = new Object[parameters.size()];

        int filledJavaArguments = 0;
        int luaOffset = offset;
        for (EParameter param : parameters) { // For each parameter in the matched call
            if (param.hasAnnotation(LuaStateArg.class)) {
                if (!param.parameterType().upperBound().raw().equals(LuaState.class))
                    throw new InvalidArgumentException("@LuaStateArg parameter must take LuaState!");

                arguments[filledJavaArguments] = state;
            } else if (param.hasAnnotation(LuaArgs.class)) {
                if (!param.parameterType().upperBound().raw().equals(Varargs.class))
                    throw new InvalidArgumentException("@LuaArgs parameter must take Varargs!");

                arguments[filledJavaArguments] = args.subargs(luaOffset);
                luaOffset = args.count() + 1;
            } else if (param.isVarArgs()) {
                Varargs sub = args.subargs(luaOffset);
                LuaTable table = new LuaTable();

                for (int i = 0; i < sub.count(); i++) {
                    table.rawset(i + 1, sub.arg(i + 1));
                }

                arguments[filledJavaArguments] = TypeCoercions.toJava(state, table, param.parameterType().upperBound());
                luaOffset = args.count() + 1;
            } else {
                Object arg;

                if (luaOffset > args.count()) {
                    if (param.hasAnnotation(OptionalArg.class)) {
                        arg = null;
                    } else {
                        throw new InvalidArgumentException("Not enough arguments!");
                    }
                } else {
                    arg = TypeCoercions.toJava(state, args.arg(luaOffset), param.parameterType().upperBound());
                    luaOffset++;
                }

                arguments[filledJavaArguments] = arg;
            }

            filledJavaArguments++;
        }

        if (luaOffset != args.count() + 1)
            throw new InvalidArgumentException("Too many arguments!");

        return arguments;
    }

    public static String paramsToPrettyString(List<EParameter> parameters) {
        var sb = new StringBuilder();
        boolean isFirst = true;
        boolean optionalsStarted = false;

        for (var param : parameters) {
            if (param.hasAnnotation(LuaStateArg.class)) {
                continue;
            }

            if (!isFirst) sb.append(", ");
            isFirst = false;

            if (param.hasAnnotation(LuaArgs.class)) {
                sb.append("...");
            } else {
                if (param.hasAnnotation(OptionalArg.class)) {
                    if (!optionalsStarted)
                        sb.append("[");

                    optionalsStarted = true;
                }

                sb.append(Allium.DEVELOPMENT ? param : Allium.MAPPINGS.getYarn(Mappings.asClass(param.rawParameterType())));
            }

        }

        if (optionalsStarted)
            sb.append("]");

        return sb.toString();
    }
}
