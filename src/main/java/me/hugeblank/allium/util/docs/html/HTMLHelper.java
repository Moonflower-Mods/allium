package me.hugeblank.allium.util.docs.html;

import me.basiqueevangelist.enhancedreflection.api.EClass;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public class HTMLHelper {
    public static HTMLElement toUnnamedList(List<HTMLElement> elements) {
        HTMLElement out = HTMLElement.of("ul").addAttribute("class", "nowrap");
        elements.forEach((element) -> out.addChild(HTMLElement.of("li").addChild(element)));
        return out;
    }

    public static <T> HTMLElement toUnnamedList(List<T> elements, BiConsumer<HTMLElement, T> handler) {
        HTMLElement out = HTMLElement.of("ul").addAttribute("class", "nowrap");
        elements.forEach((element) -> {
            HTMLElement li = HTMLElement.of("li");
            handler.accept(li, element);
            out.addChild(li);
        });
        return out;
    }

    public static <T> HTMLElement toTable(List<T> list, BiConsumer<HTMLElement, T> handler) {
        HTMLElement body = HTMLElement.of("tbody");
        list.forEach((element) -> {
            HTMLElement row = HTMLElement.of("tr");
            handler.accept(row, element);
            body.addChild(row);
        });
        return body;
    }

    public static Path classToPath(EClass<?> eClass) {
        return Path.of(eClass.name()
                .replace(".class", "")
                .replace(".", "/")
                + ".class");
    }

    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}
