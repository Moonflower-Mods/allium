package me.hugeblank.allium.util.docs;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.util.docs.html.HTMLDocument;
import me.hugeblank.allium.util.docs.html.HTMLElement;
import me.hugeblank.allium.util.docs.html.HTMLHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PackageDocument extends HTMLDocument {
    protected final Path path;
    protected final HTMLElement navigation = new HTMLElement("div").addAttribute("id", "navigation");
    protected final List<Path> packagePaths;
    protected final HTMLElement packages = new HTMLElement("table").addClassAttribute("module_list");
    protected final List<EClass<?>> classPaths;
    protected final HTMLElement classes = new HTMLElement("table").addClassAttribute("module_list");
    private static final String[] categories = {"Classes", "Packages"};

    public PackageDocument(Path path, List<Path> packagePaths, List<EClass<?>> classPaths) {
        super();
        this.path = path;
        this.packagePaths = packagePaths;
        this.classPaths = classPaths;
        addHeadElement(new HTMLElement("link", true)
                .addAttribute("rel", "stylesheet")
                .addAttribute("type", "text/css")
                .addAttribute("href", relativePathToFile(Generator.ROOT.resolve("style.css")))
        );
        navigation
                .addChild(new HTMLElement("br", true))
                .addChild(HTMLElement.of("h1", "MCLua"));
        HTMLElement container = HTMLElement.of("div").addAttribute("id", "main");
        HTMLElement content = new HTMLElement("div").addAttribute("id", "content");
        container.addChild(navigation);
        container.addChild(content);
        addBodyElement(new HTMLElement("div").addAttribute("id", "container").addChild(container));
        List<HTMLElement> cats = new ArrayList<>();

        final boolean[] hasPackages = {false};
        packages.addChild(HTMLHelper.toTable(packagePaths, (row, p) -> {
            HTMLElement key = HTMLElement.of("td").addClassAttribute("name");
            String test = path.relativize(p).toString();
            if (Files.isDirectory(p) && test.length() > 0 && !test.contains("..")) {
                hasPackages[0] = true;
                key.addChild(HTMLElement.of("a", p.toString().substring(1).replace("/", "."))
                        .addAttribute("href", relativePathToFile(p.resolve("index.html")))
                );
                row.addChild(key);
            }
        }));
        if (hasPackages[0]) {
            content.addChild(HTMLElement.of("a").addAttribute("name", categories[1]));
            content.addChild(HTMLElement.of("h2").addChild(HTMLElement.of("a", categories[1]).addAttribute("href", "#" + categories[1])));
            content.addChild(packages);
            cats.add(HTMLElement.of("a", categories[1]).addAttribute("href", "#" + categories[1]));
        }

        final boolean[] hasClasses = {false};
        classes.addChild(HTMLHelper.toTable(classPaths, (row, eClass) -> {
            if (path.equals(path.getFileSystem().getPath("/" + eClass.packageName().replace(".", "/")))) {
                hasClasses[0] = true;
                HTMLElement key = HTMLElement.of("td").addClassAttribute("name");
                HTMLElement value = HTMLElement.of("td").addClassAttribute("summary");
                key.addChild(HTMLElement.of("a", eClass.toString().replace("/", "."))
                        .addAttribute("href", relativePathToClass(eClass))
                );
                row.addChild(key).addChild(value);
                value.addContent("This description should be content pulled from the first comments of a lua file.");
            }
        }));
        if (hasClasses[0]) {
            content.addChild(HTMLElement.of("a").addAttribute("name", categories[0]));
            content.addChild(HTMLElement.of("h2").addChild(HTMLElement.of("a", categories[0]).addAttribute("href", "#" + categories[0])));
            content.addChild(classes);
            cats.add(HTMLElement.of("a", categories[0]).addAttribute("href", "#" + categories[0]));
        }

        addCategoriesToNavbar("Categories", cats.toArray(new HTMLElement[1]));
    }

    public void addCategoriesToNavbar(String name, HTMLElement... element) {
        navigation.addChild(HTMLElement.of("h2", name)).addChild(HTMLHelper.toUnnamedList(List.of(element)));
    }

    public boolean isEmpty() {
        return classPaths.isEmpty() && packagePaths.isEmpty();
    }

    private String relativePathToClass(EClass<?> reference) {
        reference = reference.raw().isArray() ? reference.arrayComponent() : reference;
        //noinspection ConstantConditions
        return path.relativize(HTMLHelper.classToPath(reference, path.getFileSystem())).toString().replace(".class", ".html");
    }

    private String relativePathToFile(Path reference) {
        return path.relativize(reference).toString();
    }
}
