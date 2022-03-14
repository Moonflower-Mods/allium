package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.UserdataFactory;
import me.hugeblank.allium.util.text.TextParserUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.lib.LuaLibrary;

public class TextLib {
    public static LuaLibrary create() {
        return LibBuilder.create("texts")
                .set("empty", UserdataFactory.toLuaValue(LiteralText.EMPTY, Text.class))
                .set("parse", TextLib::parse)
                .set("parseSafe", TextLib::parseSafe)
                .set("fromJson", TextLib::fromJson)
                .set("toJson", TextLib::toJson)
                .set("escape", TextLib::escapeForParser)
                .set("unescape", TextLib::unescape)
                .build();
    }

    private static Varargs parseSafe(LuaState state, Varargs args) throws LuaError {
        return UserdataFactory.toLuaValue(TextParserUtils.parseSafe(args.arg(1).checkString()), Text.class);
    }

    private static Varargs parse(LuaState state, Varargs args) throws LuaError {
        return UserdataFactory.toLuaValue(TextParserUtils.parse(args.arg(1).checkString()), Text.class);
    }

    private static Varargs fromJson(LuaState state, Varargs args) throws LuaError {
        return UserdataFactory.toLuaValue(Text.Serializer.fromLenientJson(args.arg(1).checkString()), Text.class);
    }

    private static Varargs toJson(LuaState state, Varargs args) throws LuaError {
        try {
            return LuaString.valueOf(Text.Serializer.toJson((Text) UserdataFactory.toJava(state, args.arg(1), Text.class)));
        } catch (UserdataFactory.InvalidArgumentException e) {
            throw new LuaError("Arg1 is not a Text!");
        }
    }


    private static Varargs escapeForParser(LuaState state, Varargs args) throws LuaError {
        return LuaString.valueOf(TextParserUtils.escapeCharacters(args.arg(1).checkString()));
    }

    private static Varargs unescape(LuaState state, Varargs args) throws LuaError {
        return LuaString.valueOf(TextParserUtils.removeEscaping(args.arg(1).checkString()));
    }
}
