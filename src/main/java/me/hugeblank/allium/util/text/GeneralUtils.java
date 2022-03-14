package me.hugeblank.allium.util.text;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class GeneralUtils {
    public static String textToString(Text text) {
        StringBuffer string = new StringBuffer(text.asString());
        recursiveParsing(string, text.getSiblings());
        return string.toString();
    }

    private static void recursiveParsing(StringBuffer string, List<Text> textList) {
        for (Text text : textList) {
            string.append(text.asString());

            List<Text> siblings = text.getSiblings();
            if (siblings.size() != 0) {
                recursiveParsing(string, siblings);
            }
        }
    }

    public static Text removeHoverAndClick(Text input) {
        var output = cloneText(input);
        removeHoverAndClick(output);
        return output;
    }

    private static void removeHoverAndClick(MutableText input) {
        if (input.getStyle() != null) {
            input.setStyle(input.getStyle().withHoverEvent(null).withClickEvent(null));
        }

        if (input instanceof TranslatableText text) {
            for (int i = 0; i < text.getArgs().length; i++) {
                var arg = text.getArgs()[i];
                if (arg instanceof MutableText argText) {
                    removeHoverAndClick(argText);
                }
            }
        }

        for (var sibling : input.getSiblings()) {
            removeHoverAndClick((MutableText) sibling);
        }

    }

    public static MutableText cloneText(Text input) {
        MutableText baseText;
        if (input instanceof TranslatableText translatable) {
            var obj = new ArrayList<>();

            for (var arg : translatable.getArgs()) {
                if (arg instanceof Text argText) {
                    obj.add(cloneText(argText));
                } else {
                    obj.add(arg);
                }
            }

            baseText = new TranslatableText(translatable.getKey(), obj.toArray());
        } else {
           baseText = input.copy();
        }

        for (var sibling : input.getSiblings()) {
            baseText.append(cloneText(sibling));
        }

        baseText.setStyle(input.getStyle());
        return baseText;
    }

    public static record TextLengthPair(MutableText text, int length) {
        public static final TextLengthPair EMPTY = new TextLengthPair(null, 0);
    }

    public static record Pair<L, R>(L left, R right) {}
}
