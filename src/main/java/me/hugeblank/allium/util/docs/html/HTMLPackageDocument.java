package me.hugeblank.allium.util.docs.html;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.util.docs.Generator;
import me.hugeblank.allium.util.docs.html.base.HTMLDocument;
import me.hugeblank.allium.util.docs.html.base.HTMLElement;
import me.hugeblank.allium.util.docs.html.base.HTMLHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HTMLPackageDocument extends HTMLDocument {
    protected final Path path;
    protected final HTMLElement navigation = new HTMLElement("div").addAttribute("id", "navigation");
    protected final List<Path> packagePaths;
    protected final HTMLElement packages = new HTMLElement("table").addClassAttribute("module_list");
    protected final List<EClass<?>> classPaths;
    protected final HTMLElement classes = new HTMLElement("table").addClassAttribute("module_list");
    private static final String[] categories = {"Classes", "Packages"};

    public HTMLPackageDocument(Path path, List<Path> packagePaths, List<EClass<?>> classPaths) {
        super();
        this.path = path;
        this.classPaths = classPaths // Find classes that are contained in this package
                .stream()
                .filter((eClass) -> path.equals(Path.of(eClass.packageName().replace(".", "/")))
                ).toList();
        // If there's no classes in the package, ignore. Unless it's root.
        if (!this.classPaths.isEmpty() || path.equals(Generator.ROOT)) {
            this.packagePaths = packagePaths // Find packages that are contained in this package
                    .stream()
                    .filter((p) -> {
                        String test = path.relativize(p).toString();
                        return test.length() > 0 && !test.contains("..");
                    }).toList();
            addHeadElement(new HTMLElement("link", true)
                    .addAttribute("rel", "stylesheet")
                    .addAttribute("type", "text/css")
                    .addAttribute("href", relativePathToFile(Path.of("style.css")))
            );
            navigation
                    .addChild(new HTMLElement("br", true))
                    .addChild(HTMLElement.of("a")
                            .addAttribute("href", relativePathToFile(Path.of("index.html")))
                            .addChild(HTMLElement.of("h1", Generator.NAME))
                    );
            HTMLElement container = HTMLElement.of("div").addAttribute("id", "main");
            HTMLElement content = new HTMLElement("div").addAttribute("id", "content");
            container.addChild(navigation);
            container.addChild(content);
            addBodyElement(new HTMLElement("div").addAttribute("id", "container").addChild(container));

            List<HTMLElement> cats = new ArrayList<>();

            packages.addChild(HTMLHelper.toTable(this.packagePaths, (row, p) -> {
                HTMLElement key = HTMLElement.of("td").addClassAttribute("name");
                key.addChild(HTMLElement.of("a", p.toString().replace("/", "."))
                        .addAttribute("href", relativePathToFile(p.resolve("index.html")))
                );
                row.addChild(key);
            }));
            content.addChild(HTMLElement.of("a").addAttribute("name", categories[1]));
            content.addChild(HTMLElement.of("h2").addChild(HTMLElement.of("a", categories[1]).addAttribute("href", "#" + categories[1])));
            content.addChild(packages);
            cats.add(HTMLElement.of("a", categories[1]).addAttribute("href", "#" + categories[1]));

            if (!this.classPaths.isEmpty()) { // Don't add the classes sub-category, in the event this is the root.
                classes.addChild(HTMLHelper.toTable(this.classPaths, (row, eClass) -> {
                    HTMLElement key = HTMLElement.of("td").addClassAttribute("name");
                    HTMLElement value = HTMLElement.of("td").addClassAttribute("summary");
                    key.addChild(HTMLElement.of("a", eClass.toString().replace("/", "."))
                            .addAttribute("href", relativePathToClass(eClass))
                    );
                    row.addChild(key).addChild(value);
                    value.addContent("This description should be content pulled from the first comments of a lua file.");
                }));
                content.addChild(HTMLElement.of("a").addAttribute("name", categories[0]));
                content.addChild(HTMLElement.of("h2").addChild(HTMLElement.of("a", categories[0]).addAttribute("href", "#" + categories[0])));
                content.addChild(classes);
                cats.add(HTMLElement.of("a", categories[0]).addAttribute("href", "#" + categories[0]));
            }

            addCategoriesToNavbar("Categories", cats.toArray(new HTMLElement[1]));
        } else {
            this.packagePaths = null;
        }
    }

    public void addCategoriesToNavbar(String name, HTMLElement... element) {
        navigation.addChild(HTMLElement.of("h2", name)).addChild(HTMLHelper.toUnnamedList(List.of(element)));
    }

    public boolean isEmpty() {
        return classPaths.isEmpty();
    }

    private String relativePathToClass(EClass<?> reference) {
        reference = reference.raw().isArray() ? reference.arrayComponent() : reference;
        return path
                .relativize(HTMLHelper.classToPath(reference))
                .toString()
                .replace(".class", ".html");
    }

    private String relativePathToFile(Path reference) {
        return path.relativize(reference).toString();
    }
}
