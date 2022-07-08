package me.hugeblank.allium.util.docs.html;

import me.basiqueevangelist.enhancedreflection.api.*;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import me.hugeblank.allium.util.docs.Generator;
import me.hugeblank.allium.util.docs.html.base.HTMLDocument;
import me.hugeblank.allium.util.docs.html.base.HTMLElement;
import me.hugeblank.allium.util.docs.html.base.HTMLHelper;
import net.minecraft.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HTMLClassDocument extends HTMLDocument {
    protected final Path path;
    protected final HTMLElement content = new HTMLElement("div").addAttribute("id", "content");
    protected final HTMLElement navigation = new HTMLElement("div").addAttribute("id", "navigation");
    private final List<HTMLElement> navList = new ArrayList<>();
    private final EClass<?> clazz;
    // Cached class list for *efficiency*
    private static final List<? extends EClass<?>> classes = Generator.CLASS_DATA.stream().map(Generator.ClassData::clazz).toList();

    public HTMLClassDocument(EClass<?> clazz, Path path) {
        super();
        addHeadElement(new HTMLElement("link", true)
                .addAttribute("rel", "stylesheet")
                .addAttribute("type", "text/css")
                .addAttribute("href", path.getParent().normalize().relativize(Path.of("style.css")).toString())
        );
        this.path = path;
        this.clazz = clazz;
        HTMLElement container = HTMLElement.of("div").addAttribute("id", "main");
        container.addChild(navigation);
        container.addChild(content);
        addBodyElement(new HTMLElement("div").addAttribute("id", "container").addChild(container));
        content.addChild(HTMLElement.of("h1", "Class ")
                .addChild(HTMLElement.of("code", clazz.name()))
        ).addChild(HTMLElement.of("p", "This description should be content pulled from the first comments of a lua file."));
        List<Pair<List<HTMLElement>, List<HTMLElement>>> categories = List.of(listStaticFields(), listStaticMethods(), listConstructors(), listInstanceFields(), listInstanceMethods());
        categories.forEach((pair) -> pair.getLeft().forEach(content::addChild));
        content.addChild(HTMLElement.of("br"));
        categories.forEach((pair) -> pair.getRight().forEach(content::addChild));
        navigation
                .addChild(new HTMLElement("br", true))
                .addChild(HTMLElement.of("a")
                        .addAttribute("href", relativePathToFile(Path.of("index.html")))
                        .addChild(HTMLElement.of("h1", Generator.NAME))
                );
        if (navList.isEmpty()) {
            addCategoriesToNavbar("Content", HTMLElement.of("br"));
        } else {
            addCategoriesToNavbar("Content", navList.toArray(new HTMLElement[1]));
        }
    }

    public void addCategoriesToNavbar(String name, HTMLElement... element) {
        navigation.addChild(HTMLElement.of("h2", name)).addChild(HTMLHelper.toUnnamedList(List.of(element)));
    }

    public void addClassesToNavbar(String name, List<EClass<?>> elements) {
        navigation.addChild(HTMLElement.of("h2", name)).addChild(HTMLHelper.toUnnamedList(elements, ((li, eClass) -> {
            if (eClass.equals(clazz)) {
                li.addChild(HTMLElement.of("strong", getName(eClass)));
                return;
            }
            if (eClass.packageName().equals(clazz.packageName()))
                li.addChild(HTMLElement.of("a").addAttribute("href", relativePathToClass(eClass)).addContent(getName(eClass)));
        })));
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listStaticFields() {
        List<EField> fields = clazz.fields()
                .stream()
                .filter((eField) -> eField.isStatic() && eField.isPublic())
                .toList();
        return listFields("Static Fields", fields);
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listInstanceFields() {
        List<EField> fields = clazz.fields()
                .stream()
                .filter((eField) -> !eField.isStatic() && eField.isPublic())
                .toList();
        return listFields("Instance Fields", fields);
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listConstructors() {
        List<? extends EConstructor<?>> constructors = clazz.constructors()
                .stream()
                .filter(ModifierHolder::isPublic)
                .toList();
        return listCallable("Constructors", constructors);
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listInstanceMethods() {
        List<EMethod> methods = clazz.methods()
                .stream()
                .filter((eMethod) -> !eMethod.isStatic() && isPublic(eMethod))
                .toList();
        return listCallable("Instance Methods", methods);
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listStaticMethods() {
        List<EMethod> methods = clazz.methods()
                .stream()
                .filter((eMethod) -> eMethod.isStatic() && isPublic(eMethod))
                .toList();
        return listCallable("Static Methods", methods);
    }

    private static boolean isPublic(EMethod method) {
        return method.isPublic() && !method.name().contains("allium_private$");
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listFields(String category, List<EField> fields) {
        //noinspection DuplicatedCode
        if (fields.size() == 0) {
            // Add nothing
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }
        navList.add(HTMLElement.of("a", category).addAttribute("href", "#" + category));
        HTMLElement summary = HTMLElement.of("tbody");
        fields.stream().sorted(Comparator.comparing(EMember::name)).forEach((field) -> {
            String fieldDetails = "This description should be content pulled from the comments above a method in a lua file. Format: \"--- @method m_id DESCRIPTION\"";
            String header;
            if (field.isStatic()) {
                header = getClassName() + "." + field.name();
            } else {
                header = "instance." + field.name();
            }
            summary.addChild(HTMLElement.of("tr")
                    .addChild(HTMLElement.of("td")
                            .addClassAttribute("field-type")
                            .addAttribute("nowrap", "")
                            .addChild(createTypesTag(field.rawFieldType()))
                    )
                    .addChild(HTMLElement.of("td")
                            .addClassAttribute("name")
                            .addAttribute("nowrap", "")
                            .addChild(HTMLElement.of("strong", header))
                    )
                    .addChild(HTMLElement.of("td")
                            .addClassAttribute("summary")
                            .addContent(fieldDetails)
                    )
            );
        });

        return new Pair<>(List.of(
                HTMLElement.of("a").addAttribute("name", category),
                HTMLElement.of("h2")
                        .addChild(HTMLElement.of("a", category)
                                .addAttribute("href", "#" + category)
                        ),
                HTMLElement.of("table")
                        .addClassAttribute("function_list")
                        .addChild(summary)
        ), new ArrayList<>()); // Return no details
    }

    private Pair<List<HTMLElement>, List<HTMLElement>> listCallable(String category, List<? extends EExecutable> executables) {
        //noinspection DuplicatedCode
        if (executables.size() == 0) {
            // Add nothing
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }
        navList.add(HTMLElement.of("a", category).addAttribute("href", "#" + category));
        HTMLElement summary = HTMLElement.of("tbody");
        List<HTMLElement> details = new ArrayList<>();
        details.add(HTMLElement.of("a").addAttribute("name", category));
        details.add(HTMLElement.of("h2", category)
                .addClassAttribute("section-header")
        );

        executables.stream()
                .filter((executable) ->
                        clazz.hasAnnotation(LuaWrapped.class) && executable.hasAnnotation(LuaWrapped.class)
                        || !clazz.hasAnnotation(LuaWrapped.class)
                )
                .sorted(Comparator.comparing(EMember::name))
                .forEach((executable) -> {
                    String methodInfo = "This description should be content pulled from the comments above a method in a lua file. Format: \"--- @method m_id DESCRIPTION\"";
                    String header = getCallableHeader(executable);
                    summary.addChild(HTMLElement.of("tr")
                            .addChild(HTMLElement.of("td")
                                    .addClassAttribute("name")
                                    .addAttribute("nowrap", "")
                                    .addChild(HTMLElement.of("a")
                                            .addAttribute("href", "#" + header)
                                            .addContent(header)
                                    )
                            )
                            .addChild(HTMLElement.of("td")
                                    .addClassAttribute("summary")
                                    .addContent(methodInfo)
                            )
                    );
                    HTMLElement inoutDetails = HTMLElement.of("dd") // DETAILS
                            .addContent(methodInfo);
                    if (executable.parameters().size() > 0) {
                        inoutDetails.addChild(HTMLElement.of("h3", "Parameters:"));
                    }
                    inoutDetails.addChild(HTMLHelper.toUnnamedList(executable.parameters(), (li, param) -> { // PARAMETERS
                        li // PARAM DETAILS
                                .addChild(HTMLElement.of("span")
                                        .addClassAttribute("parameter")
                                        .addContent(param.name())
                                )
                                .addChild(createTypesTag(param.rawParameterType()))
                                .addContent("This description should be content pulled from the comments above a method in a lua file. Format: \"-- @param varName DESCRIPTION\"");
                    }));

                    if (executable instanceof EMethod eMethod && !eMethod.rawReturnType().raw().equals(void.class)) {
                        inoutDetails
                                .addChild(HTMLElement.of("h3", "Returns:"))
                                .addChild(createTypesTag(eMethod.rawReturnType()))
                                .addContent("This description should be content pulled from the comments above a method in a lua file. Format: \"-- @return DESCRIPTION\"");
                    }

                    details.add(HTMLElement.of("dl")
                            .addClassAttribute("function")
                            .addChild(HTMLElement.of("dt")
                                    .addChild(HTMLElement.of("a").addAttribute("name", header))
                                    .addChild(HTMLElement.of("strong", header))
                            ).addChild(inoutDetails)
                    );
                });

        return new Pair<>(List.of(
                HTMLElement.of("a").addAttribute("name", category),
                HTMLElement.of("h2")
                        .addChild(HTMLElement.of("a", category)
                                .addAttribute("href", "#" + category)
                        ),
                HTMLElement.of("table")
                        .addClassAttribute("function_list")
                        .addChild(summary)
        ), details);
    }

    private String getCallableHeader(EExecutable executable) {
        StringBuilder header = new StringBuilder();
        if (executable instanceof EMethod) {

            header.append(getCallableName(executable)).append("(");
        } else if (executable instanceof EConstructor<?>) {
            header.append(getClassName()).append("(");
        }
        List<EParameter> params = executable.parameters();
        for (int i = 0; i < params.size(); i++) {
            header.append(params.get(i).name());
            if (i < params.size() - 1) {
                header.append(", ");
            }
        }
        return header.append(")").toString();
    }

    private String getCallableName(EExecutable executable) {
        String defName = executable.name().replace("allium$", "");
        String out = "";
        if (executable.isStatic()) {
            out += getClassName() + ".";
        } else {
            out += "instance:";
        }
        if (executable.hasAnnotation(LuaWrapped.class)) {
            LuaWrapped luaWrapped = executable.annotation(LuaWrapped.class);
            if (luaWrapped != null && luaWrapped.name().length > 0) {
                out += luaWrapped.name()[0];
            } else {
                out += defName;
            }
        } else {
            out += defName;
        }
        return out;
    }

    private String getClassName() {
        String defName = clazz.simpleName().replace("allium$", "");
        if (clazz.hasAnnotation(LuaWrapped.class)) {
            LuaWrapped luaWrapped = clazz.annotation(LuaWrapped.class);
            if (luaWrapped != null && luaWrapped.name().length > 0) {
                return luaWrapped.name()[0];
            } else {
                return defName;
            }
        } else {
            return defName;
        }
    }

    private HTMLElement createTypesTag(EClass<?> eClass) { // Parameter and return type generation
        HTMLElement ptype = HTMLElement.of("span").addClassAttribute("type");

        if (classes.contains(eClass)) {
            ptype
                    .addContent("userdata, ")
                    .addChild(
                            HTMLElement.of("a")
                                    .addAttribute("href", relativePathToClass(eClass))
                                    .addContent(getName(eClass))
                    );
        } else { // Class is outside the scope of the game, or is primitive
            StringBuilder builder = new StringBuilder();
            Class<?> rawType = eClass.raw();
            boolean found = false; // Check if this is a Lua Primitive
            if (
                    rawType.equals(byte.class) || rawType.equals(short.class) || rawType.equals(long.class) ||
                            rawType.equals(Byte.class) || rawType.equals(Short.class) || rawType.equals(Long.class)
            ) {
                builder.append("int, ");
                found = true;
            } else if (
                    rawType.equals(float.class) || rawType.equals(double.class) ||
                            rawType.equals(Float.class) || rawType.equals(Double.class)
            ) {
                builder.append("number, ");
                found = true;
            } else if (rawType.equals(int.class) || rawType.equals(Integer.class) || rawType.equals(boolean.class) || rawType.equals(Boolean.class) || rawType.equals(String.class)) {
                found = true; // No additional information, the java and lua types are essentially the same.
            } else { // It's not a primitive, so it's a class not in the scope of the project. (java.*, lwjgl, gson, etc.)
                builder.append("userdata, ");
            }
            String name = rawType.getSimpleName();
            if (rawType.equals(Integer.class)) { // Why not Int.class??? >:I
                name = "int";
            } else {
                if (found) {
                    name = name.toLowerCase();
                }
            }
            ptype.addContent(builder.append(name).toString());
        }

        return HTMLElement.of("span")
                .addClassAttribute("types")
                .addChild(ptype);
    }

    private String relativePathToClass(EClass<?> reference) {
        reference = reference.raw().isArray() ? reference.arrayComponent() : reference;
        return path.getParent()
                .relativize(HTMLHelper.classToPath(reference))
                .toString()
                .replace(".class", ".html");
    }

    private String relativePathToFile(Path reference) {
        return path.getParent().relativize(reference).toString();
    }

    private String getName(EClass<?> eClass) {
        if (eClass.raw().getDeclaringClass() == null) return eClass.simpleName();
        return getName(EClass.fromJava(eClass.raw().getDeclaringClass())) + "$" + eClass.simpleName();
    }
}
