package me.hugeblank.allium.util.docs.html.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLElement {
    protected final boolean singlet;
    protected final String tag;
    protected final Map<String, String> attributes = new HashMap<>();
    protected final List<HTMLElement> children = new ArrayList<>();

    public HTMLElement(String tag, boolean singlet) {
        this.singlet = singlet;
        this.tag = tag;
    }

    public HTMLElement(String tag) {
        this(tag, false);
    }

    public static HTMLElement of(String tag, String content) {
        HTMLElement out = new HTMLElement(tag);
        out.addChild(new HTMLContent(content));
        return out;
    }

    public static HTMLElement of(String tag) {
        return new HTMLElement(tag);
    }

    public HTMLElement addAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    public HTMLElement addClassAttribute(String value) {
        attributes.put("class", value);
        return this;
    }

    public HTMLElement addContent(String content) {
        children.add(HTMLContent.of(content));
        return this;
    }

    public HTMLElement addChild(HTMLElement element) {
        if (element == null) return this;
        children.add(element);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(tag);
        // Apply attributes
        attributes.entrySet().forEach(
                (entry) -> // ( attribute="value")
                builder.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"")
        );

        builder.append(">\n");
        if (singlet) return builder.toString();
        children.forEach((e) -> builder.append("\t").append(e));
        builder.append("</").append(tag).append(">\n");
        return builder.toString();
    }


}