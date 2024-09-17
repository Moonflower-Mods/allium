package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.text.Text;

@LuaWrapped(name = "texts")
public class TextLib implements WrappedLuaLibrary {
    @LuaWrapped(name = "empty")
    public Text EMPTY = Text.empty();

    @LuaWrapped
    public Text format(String input) {
        return ParserBuilder.of().legacyAll().build().parseNode(input).toText();
    }

    @LuaWrapped
    public Text formatSafe(String input) {
        return ParserBuilder.of().legacyAll().requireSafe().build().parseNode(input).toText();
    }

    @LuaWrapped
    public Text fromJson(String input) {
        return Text.Serialization.fromLenientJson(input, BuiltinRegistries.createWrapperLookup());
    }

    @LuaWrapped
    public String toJson(Text text) {
        return Text.Serialization.toJsonString(text, BuiltinRegistries.createWrapperLookup());
    }
}
