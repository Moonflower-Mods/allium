package dev.hugeblank.allium.util.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class GeneralUtils {
    public static String textToString(Text text) {
        StringBuffer string = new StringBuffer(text.copyContentOnly().toString());
        recursiveParsing(string, text.getSiblings());
        return string.toString();
    }

    private static void recursiveParsing(StringBuffer string, List<Text> textList) {
        for (Text text : textList) {
            string.append(text.getContent().toString());

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

        if (input.getContent() instanceof TranslatableTextContent text) {
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
        if (input.getContent() instanceof TranslatableTextContent translatable) {
            var obj = new ArrayList<>();

            for (var arg : translatable.getArgs()) {
                if (arg instanceof Text argText) {
                    obj.add(cloneText(argText));
                } else {
                    obj.add(arg);
                }
            }

            baseText = Text.translatable(translatable.getKey(), obj.toArray());
        } else {
            baseText = input.copyContentOnly();
        }

        for (var sibling : input.getSiblings()) {
            baseText.append(cloneText(sibling));
        }

        baseText.setStyle(input.getStyle());
        return baseText;
    }

    public record TextLengthPair(MutableText text, int length) {
        public static final TextLengthPair EMPTY = new TextLengthPair(null, 0);
    }

    public record Pair<L, R>(L left, R right) {}
}
