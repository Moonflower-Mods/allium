package dev.hugeblank.allium.util.docs.html.base;

public class HTMLDocument {
    protected final HTMLElement html = new HTMLElement("html");
    protected final HTMLElement head = new HTMLElement("head");
    protected final HTMLElement body = new HTMLElement("body");

    public HTMLDocument() {
        html.addChild(head);
        html.addChild(body);
    }

    public HTMLDocument(String title) {
        this();
        head.addChild(HTMLContent.of(title));
    }

    public void addHeadElement(HTMLElement element) {
        head.addChild(element);
    }

    public void addBodyElement(HTMLElement element) {
        body.addChild(element);
    }

    @Override
    public String toString() {
        return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" + html;
    }
}
