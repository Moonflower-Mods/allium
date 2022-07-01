package me.hugeblank.allium.lua.api;

import eu.pb4.placeholders.api.TextParserUtils;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.text.Text;

@LuaWrapped(name = "texts")
public class TextLib implements WrappedLuaLibrary {
    @LuaWrapped(name = "empty")
    public Text EMPTY = Text.empty();

    @LuaWrapped
    @Deprecated
    public Text parse(String input) {
        return format(input);
    }

    @LuaWrapped
    @Deprecated
    public Text parseSafe(String input) {
        return formatSafe(input);
    }

    @LuaWrapped
    public Text format(String input) {
        return TextParserUtils.formatText(input);
    }

    @LuaWrapped
    public Text formatSafe(String input) {
        return TextParserUtils.formatTextSafe(input);
    }

    @LuaWrapped
    public Text fromJson(String input) {
        return Text.Serializer.fromLenientJson(input);
    }

    @LuaWrapped
    public String toJson(Text text) {
        return Text.Serializer.toJson(text);
    }

    /*@LuaWrapped
    public String escapeForParser(String string) {
        return TextParserImpl.escapeCharacters(string);
    }

    @LuaWrapped
    public String unescape(String string) {
        return TextParserImpl.removeEscaping(string);
    }*/
}
