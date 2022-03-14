package me.hugeblank.allium.util.text;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.util.internal.UnstableApi;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import static me.hugeblank.allium.util.text.GeneralUtils.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Modified version of TextPlaceholderAPI's (https://github.com/Patbox/TextPlaceholderAPI) TextParser implementation
 */
public class TextParserUtils {
    // Based on minimessage's regex, modified to fit more parsers needs
    public static final Pattern STARTING_PATTERN = Pattern.compile("<(?<id>[^<>/]+)(?<data>(:([']?([^'](\\\\\\\\['])?)+[']?))*)>");
    public static final List<Pair<String, String>> ESCAPED_CHARS = new ArrayList<>();
    public static final List<Pair<String, String>> UNESCAPED_CHARS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().registerTypeHierarchyAdapter(Style.class, new Style.Serializer()).create();
    private static boolean IS_REGISTERED = false;

    private static final HashMap<String, TextFormatterHandler> TAGS = new HashMap<>();
    private static final HashMap<String, TextFormatterHandler> SAFE_TAGS = new HashMap<>();

    public static Text parse(String input) {
        return parse(input, TAGS);
    }

    public static Text parseSafe(String input) {
        return parse(input, SAFE_TAGS);
    }

    public static void register(String identifier, TextFormatterHandler handler) {
        register(identifier, handler, true);
    }

    public static void register(String identifier, TextFormatterHandler handler, boolean safe) {
        if (safe) {
            SAFE_TAGS.put(identifier, handler);
        }
        TAGS.put(identifier, handler);
    }

    public static ImmutableMap<String, TextFormatterHandler> getRegisteredTags() {
        return ImmutableMap.copyOf(TAGS);
    }

    public static ImmutableMap<String, TextFormatterHandler> getRegisteredSafeTags() {
        return ImmutableMap.copyOf(SAFE_TAGS);
    }

    @FunctionalInterface
    public interface TextFormatterHandler {
        GeneralUtils.TextLengthPair parse(String tag, String data, String input, Map<String, TextFormatterHandler> handlers, String endAt);
    }


    public static Text parse(String string, Map<String, TextFormatterHandler> handlers) {
        if (!IS_REGISTERED) {
            register();
        }
        return recursiveParsing(escapeCharacters(string), handlers, null).text();
    }

    public static String escapeCharacters(String string) {
        for (Pair<String, String> entry : ESCAPED_CHARS) {
            string = string.replaceAll(Matcher.quoteReplacement(entry.left()), entry.right());
        }
        return string;
    }

    public static String removeEscaping(String string) {
        for (Pair<String, String> entry : UNESCAPED_CHARS) {
            try {
                string = string.replaceAll(entry.right(), entry.left());
            } catch (Exception e) {
                // Silence!
            }
        }
        return string;
    }

    public static String cleanArgument(String string) {
        if (string.length() >= 2 && string.startsWith("'") && string.endsWith("'")) {
            return string.substring(1, string.length() - 1);
        } else {
            return string;
        }
    }

    public static GeneralUtils.TextLengthPair recursiveParsing(String input, Map<String, TextFormatterHandler> handlers, String endAt) {
        if (input.isEmpty()) {
            return new GeneralUtils.TextLengthPair(new LiteralText(""), 0);
        }

        MutableText text = null;

        Matcher matcher = STARTING_PATTERN.matcher(input);
        Matcher matcherEnd = endAt != null ? Pattern.compile(endAt).matcher(input) : null;
        int currentPos = 0;
        int offset = 0;
        boolean hasEndTag = endAt != null && matcherEnd.find();
        int currentEnd = hasEndTag ? matcherEnd.start() : input.length();

        while (matcher.find()) {
            if (currentEnd <= matcher.start()) {
                break;
            }

            String[] entireTag = (matcher.group("id") + matcher.group("data")).split(":", 2);
            String tag = entireTag[0].toLowerCase(Locale.ROOT);
            String data = "";
            if (entireTag.length == 2) {
                data = entireTag[1];
            }

            // Special reset handling for <reset> tag
            if (tag.equals("reset") || tag.equals("r")) {
                if (endAt != null) {
                    currentEnd = matcher.start();
                    if (currentPos < currentEnd) {
                        String restOfText = removeEscaping(input.substring(currentPos, currentEnd));
                        if (restOfText.length() != 0) {
                            if (text == null) {
                                text = new LiteralText(restOfText);
                            } else {
                                text.append(restOfText);
                            }
                        }
                    }

                    return new GeneralUtils.TextLengthPair(text, currentEnd);
                } else {
                    String betweenText = input.substring(currentPos, matcher.start());

                    if (betweenText.length() != 0) {
                        if (text == null) {
                            text = new LiteralText(removeEscaping(betweenText));
                        } else {
                            text.append(removeEscaping(betweenText));
                        }
                    }
                    currentPos = matcher.end();
                }
            } else {

                if (tag.startsWith("#")) {
                    data = tag;
                    tag = "color";
                }

                String end = "</" + tag + ">";

                TextFormatterHandler handler = handlers.get(tag);
                if (handler != null) {
                    String betweenText = input.substring(currentPos, matcher.start());

                    if (betweenText.length() != 0) {
                        if (text == null) {
                            text = new LiteralText(removeEscaping(betweenText));
                        } else {
                            text.append(removeEscaping(betweenText));
                        }
                    }
                    currentPos = matcher.end();
                    try {
                        GeneralUtils.TextLengthPair pair = handler.parse(tag, data, input.substring(currentPos), handlers, end);
                        if (pair.text() != null) {
                            if (text == null) {
                                text = new LiteralText("");
                            }
                            text.append(pair.text());
                        }
                        currentPos += pair.length();

                        if (currentPos >= input.length()) {
                            currentEnd = input.length();
                            break;
                        }
                        matcher.region(currentPos, input.length());
                        if (matcherEnd != null) {
                            matcherEnd.region(currentPos, input.length());
                            if (matcherEnd.find()) {
                                hasEndTag = true;
                                currentEnd = matcherEnd.start();
                            } else {
                                hasEndTag = false;
                                currentEnd = input.length();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (currentPos < currentEnd) {
            String restOfText = removeEscaping(input.substring(currentPos, currentEnd));
            if (restOfText.length() != 0) {
                if (text == null) {
                    text = new LiteralText(restOfText);
                } else {
                    text.append(restOfText);
                }
            }
        }

        if (hasEndTag) {
            currentEnd += endAt.length();
        } else {
            currentEnd = input.length();
        }
        return new GeneralUtils.TextLengthPair(text, currentEnd);
    }

    public static void register() {
        if (IS_REGISTERED) {
            return;
        } else {
            IS_REGISTERED = true;
        }

        {
            for (Formatting formatting : Formatting.values()) {
                register(formatting.getName(), (tag, data, input, handlers, endAt) -> {
                    GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                    out.text().formatted(formatting);
                    return out;
                });
            }

            var reg = getRegisteredTags();

            register("orange", reg.get("gold"));
            register("grey", reg.get("gray"));
            register("dark_grey", reg.get("dark_gray"));
            register("st", reg.get("strikethrough"));
            register("obf", reg.get("obfuscated"));
            register("em", reg.get("italic"));
            register("i", reg.get("italic"));
            register("b", reg.get("bold"));
            register("underlined", reg.get("underline"));
        }

        {
            TextFormatterHandler color = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                out.text().fillStyle(Style.EMPTY.withColor(TextColor.parse(cleanArgument(data))));
                return out;
            };

            register("color", color);
            register("colour", color);
            register("c", color);
        }

        register("font", (tag, data, input, handlers, endAt) -> {
            GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
            out.text().fillStyle(Style.EMPTY.withFont(Identifier.tryParse(cleanArgument(data))));
            return out;
        });

        register("lang", (tag, data, input, handlers, endAt) -> {
            String[] lines = data.split(":");
            if (lines.length > 0) {
                List<Text> textList = new ArrayList<>();
                boolean skipped = false;
                for (String part : lines) {
                    if (!skipped) {
                        skipped = true;
                        continue;
                    }
                    textList.add(parse(removeEscaping(cleanArgument(part)), handlers));
                }

                MutableText out = new TranslatableText(cleanArgument(lines[0]), textList.toArray());
                return new GeneralUtils.TextLengthPair(out, 0);
            }
            return GeneralUtils.TextLengthPair.EMPTY;
        });

        register("key", (tag, data, input, handlers, endAt) -> {
            if (!data.isEmpty()) {
                MutableText out = new KeybindText(cleanArgument(data));
                return new GeneralUtils.TextLengthPair(out, 0);
            }
            return GeneralUtils.TextLengthPair.EMPTY;
        });

        register("click", (tag, data, input, handlers, endAt) -> {
            String[] lines = data.split(":", 2);
            GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
            if (lines.length > 1) {
                ClickEvent.Action action = ClickEvent.Action.byName(cleanArgument(lines[0]));
                if (action != null) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(action, removeEscaping(cleanArgument(lines[1])))));
                }
            }
            return out;
        }, false);

        {
            TextFormatterHandler formatter = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                if (!data.isEmpty()) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeEscaping(cleanArgument(data)))));
                }
                return out;
            };

            register("run_command", formatter, false);
            register("run_cmd", formatter, false);
        }

        {
            TextFormatterHandler formatter = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                if (!data.isEmpty()) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, removeEscaping(cleanArgument(data)))));
                }
                return out;
            };

            register("suggest_command", formatter, false);
            register("cmd", formatter, false);
        }

        {
            TextFormatterHandler formatter = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                if (!data.isEmpty()) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, removeEscaping(cleanArgument(data)))));
                }
                return out;
            };

            register("open_url", formatter, false);
            register("url", formatter, false);
        }

        {
            TextFormatterHandler formatter = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                if (!data.isEmpty()) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, removeEscaping(cleanArgument(data)))));
                }
                return out;
            };

            register("copy_to_clipboard", formatter, false);
            register("copy", formatter, false);
        }

        {
            TextFormatterHandler formatter = (tag, data, input, handlers, endAt) -> {
                GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
                if (!data.isEmpty()) {
                    out.text().setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, removeEscaping(cleanArgument(data)))));
                }
                return out;
            };

            register("change_page", formatter);
            register("page", formatter);
        }

        register("hover", (tag, data, input, handlers, endAt) -> {
            String[] lines = data.split(":", 2);
            GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);

            try {
                if (lines.length > 1) {
                    HoverEvent.Action<?> action = HoverEvent.Action.byName(cleanArgument(lines[0].toLowerCase(Locale.ROOT)));

                    if (action == HoverEvent.Action.SHOW_TEXT) {
                        out.text().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, parse(removeEscaping(cleanArgument(lines[1])), handlers))));
                    } else if (action == HoverEvent.Action.SHOW_ENTITY) {
                        lines = lines[1].split(":", 3);
                        if (lines.length == 3) {
                            out.text().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                                    new HoverEvent.EntityContent(
                                            EntityType.get(removeEscaping(removeEscaping(cleanArgument(lines[0])))).orElse(EntityType.PIG),
                                            UUID.fromString(cleanArgument(lines[1])),
                                            parse(removeEscaping(removeEscaping(cleanArgument(lines[2]))), handlers))
                            )));
                        }
                    } else if (action == HoverEvent.Action.SHOW_ITEM) {
                        out.text().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new HoverEvent.ItemStackContent(ItemStack.fromNbt(StringNbtReader.parse(removeEscaping(cleanArgument(lines[1])))))
                        )));
                    } else {
                        out.text().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, parse(removeEscaping(cleanArgument(data)), handlers))));
                    }
                } else {
                    out.text().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, parse(removeEscaping(cleanArgument(data)), handlers))));
                }
            } catch (Exception e) {
                // Shut
            }
            return out;
        });

        register("insert", (tag, data, input, handlers, endAt) -> {
            GeneralUtils.TextLengthPair out = recursiveParsing(input, handlers, endAt);
            out.text().setStyle(Style.EMPTY.withInsertion(removeEscaping(cleanArgument(data))));
            return out;
        }, false);

        register("score", (tag, data, input, handlers, endAt) -> {
            String[] lines = data.split(":");
            if (lines.length == 2) {
                MutableText out = new ScoreText(removeEscaping(cleanArgument(lines[0])), removeEscaping(cleanArgument(lines[1])));
                return new GeneralUtils.TextLengthPair(out, 0);
            }
            return GeneralUtils.TextLengthPair.EMPTY;
        }, false);

        ESCAPED_CHARS.add(new Pair<>("\\\\", "&slsh;"));
        ESCAPED_CHARS.add(new Pair<>("\\<", "&lt;"));
        ESCAPED_CHARS.add(new Pair<>("\\>", "&gt;"));
        ESCAPED_CHARS.add(new Pair<>("\\\"", "&quot;"));
        ESCAPED_CHARS.add(new Pair<>("\\'", "&pos;"));
        ESCAPED_CHARS.add(new Pair<>("\\:", "&colon;"));

        UNESCAPED_CHARS.add(new Pair<>("\\", "&slsh;"));
        UNESCAPED_CHARS.add(new Pair<>("<", "&lt;"));
        UNESCAPED_CHARS.add(new Pair<>(">", "&gt;"));
        UNESCAPED_CHARS.add(new Pair<>("\"", "&quot;"));
        UNESCAPED_CHARS.add(new Pair<>("'", "&pos;"));
        UNESCAPED_CHARS.add(new Pair<>(":", "&colon;"));
    }

    // Cursed don't touch this
    @ApiStatus.Experimental
    @UnstableApi
    public static String convertToString(Text text) {
        StringBuilder builder = new StringBuilder();
        String style = GSON.toJson(text.getStyle());
        if (style != null && !style.equals("null")) {
            builder.append("<style:").append(style).append(">");
        }
        if (text instanceof LiteralText literalText) {
            builder.append(escapeCharacters(literalText.asString()));
        } else if (text instanceof TranslatableText translatableText) {
            List<String> stringList = new ArrayList<>();

            for (Object arg : translatableText.getArgs()) {
                if (arg instanceof Text text1) {
                    stringList.add("'" + escapeCharacters(convertToString(text1)) + "'");
                } else {
                    stringList.add("'" + escapeCharacters(arg.toString()) + "'");
                }
            }

            if (stringList.size() > 0) {
                stringList.add(0, "");
            }

            String additional = String.join(":", stringList);

            builder.append("<lang:'").append(translatableText.getKey()).append("'").append(additional).append(">");
        } else if (text instanceof KeybindText keybindText) {
            builder.append("<key:'").append(keybindText.getKey()).append("'>");
        } else {
            builder.append("<raw:'").append(escapeCharacters(Text.Serializer.toJson(text.copy()))).append("'>");
        }

        for (Text text1 : text.getSiblings()) {
            builder.append(convertToString(text1));
        }

        if (style != null && !style.equals("null")) {
            builder.append("</style>");
        }
        return builder.toString();
    }

}