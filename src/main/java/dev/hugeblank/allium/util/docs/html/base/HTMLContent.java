package dev.hugeblank.allium.util.docs.html.base;

public class HTMLContent extends HTMLElement {

    HTMLContent(String content) {
        super(content, true);
    }

    public static HTMLContent of(String content) {
        return new HTMLContent(content);
    }

    @Override
    public HTMLElement addAttribute(String key, String value) { return null;}

    @Override
    public HTMLElement addChild(HTMLElement element) { return null;}

    @Override
    public String toString() {
        return tag;
    }
}
