package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.lua.type.HideFromLua;
import me.hugeblank.allium.lua.type.LuaName;
import me.hugeblank.allium.lua.type.UserdataFactory;
import me.hugeblank.allium.util.text.TextParserUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.LuaLibrary;

public class TextLib {
    @HideFromLua
    public static LuaLibrary create() {
        return (state, env) -> {
            LuaValue lib = JavaLib.importClass(EClass.fromJava(TextLib.class));

            env.rawset("texts", lib);
            state.loadedPackages.rawset("texts", lib);

            return lib;
        };
    }

    @LuaName("empty")
    public static Text EMPTY = LiteralText.EMPTY;

    public static Text parse(String input) {
        return TextParserUtils.parse(input);
    }

    public static Text parseSafe(String input) {
        return TextParserUtils.parseSafe(input);
    }

    public static Text fromJson(String input) {
        return Text.Serializer.fromLenientJson(input);
    }

    public static String toJson(Text text) {
        return Text.Serializer.toJson(text);
    }

    public static String escapeForParser(String string) {
        return TextParserUtils.escapeCharacters(string);
    }

    public static String unescape(String string) {
        return TextParserUtils.removeEscaping(string);
    }
}
