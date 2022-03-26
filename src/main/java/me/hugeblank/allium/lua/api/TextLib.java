package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.util.text.TextParserUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

@LuaWrapped(name = "texts")
public class TextLib implements WrappedLuaLibrary {
    @LuaWrapped(name = "empty")
    public Text EMPTY = LiteralText.EMPTY;

    @LuaWrapped
    public Text parse(String input) {
        return TextParserUtils.parse(input);
    }

    @LuaWrapped
    public Text parseSafe(String input) {
        return TextParserUtils.parseSafe(input);
    }

    @LuaWrapped
    public Text fromJson(String input) {
        return Text.Serializer.fromLenientJson(input);
    }

    @LuaWrapped
    public String toJson(Text text) {
        return Text.Serializer.toJson(text);
    }

    @LuaWrapped
    public String escapeForParser(String string) {
        return TextParserUtils.escapeCharacters(string);
    }

    @LuaWrapped
    public String unescape(String string) {
        return TextParserUtils.removeEscaping(string);
    }
}
