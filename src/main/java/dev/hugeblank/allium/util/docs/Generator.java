package dev.hugeblank.allium.util.docs;

import dev.hugeblank.allium.Allium;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.docs.html.HTMLClassDocument;
import dev.hugeblank.allium.util.docs.html.HTMLPackageDocument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Generator {
    // Path to class, Pair with Path to html file and Enhanced Class
    public static final List<ClassData> CLASS_DATA = new ArrayList<>();
    public static final List<Path> PACKAGES = new ArrayList<>();
    public static final Path ROOT = Path.of("");
    public static final String NAME = "Allium Docs";

    public static void generate(Class<?>... clazzes) {
        Allium.LOGGER.warn("Starting docgen, this may take a while.");
        PACKAGES.add(ROOT);
        try {
            for (Class<?> clazz : clazzes) {
                URL url = ClassLoader.getSystemClassLoader().getResource(clazz.getName().replace(".", "/") + ".class");
                if (url != null) {
                    Path trueRoot = getPath(url.toURI(), EClass.fromJava(clazz));
                    if (Files.exists(trueRoot)) {
                        Files.walkFileTree(trueRoot, new FileVisitor<>() {
                            int fileCount;

                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                Path fakeDir = Path.of(trueRoot.relativize(dir).toString());
                                if (!fakeDir.equals(ROOT) && (fakeDir.getFileName().toString().equals("unused") || fakeDir.getFileName().toString().equals("mixin")))
                                    return FileVisitResult.SKIP_SUBTREE;
                                Stream<Path> files = Files.list(dir);
                                if (files
                                        .filter(Files::isRegularFile)
                                        .anyMatch((p) -> p.getFileName().toString().matches(".*\\.class$"))
                                ) {
                                    PACKAGES.add(fakeDir);
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                file = Path.of(trueRoot.relativize(file).normalize().toString());
                                if (!Pattern.compile("\\.class$").matcher(file.getFileName().toString()).find())
                                    return FileVisitResult.CONTINUE; // We're only looking for .class files...
                                if (Pattern.compile("\\$\\d*\\.class$").matcher(file.getFileName().toString()).find())
                                    return FileVisitResult.CONTINUE; // ...That aren't generated
                                String className = file.toString()
                                        .replace("/", ".")
                                        .replaceFirst("\\.class$", "");
                                try {
                                    if (file.getParent() != null) {
                                        EClass<?> clazz = EClass.fromJava(Class.forName(className));
                                        CLASS_DATA.add(
                                                new ClassData(
                                                        file,
                                                        getPath(FileHelper.HTML_DOCS_DIR, file, ".html"),
                                                        getPath(FileHelper.LUA_DOCS_DIR, file, ".lua"),
                                                        clazz
                                                )
                                        );

                                        if (fileCount % 10 == 0) {
                                            System.out.print(".");
                                        }
                                        if (fileCount % 1000 == 0) {
                                            System.out.println();
                                        }
                                        fileCount++;
                                    }
                                } catch (ClassNotFoundException e) {
                                    Allium.LOGGER.warn("Could not document class " + className);
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            // Create necessary files on the lua and HTML side of things
                            @NotNull
                            private Path getPath(Path docFile, Path classFile, String extension) throws IOException {
                                Files.createDirectories(docFile.resolve(classFile.getParent().toString()));
                                Path out = docFile.resolve(classFile.toString().replace(".class", extension));
                                if (!Files.exists(out)) Files.createFile(out);
                                return out;
                            }


                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                String className = file.toString()
                                        .replace("/", ".")
                                        .replace(".class", "")
                                        .substring(1);

                                Allium.LOGGER.warn("Failed to load class " + className);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                }
            }
            System.out.println();
            Allium.LOGGER.info("Docgen complete, saving...");
            complete();
        } catch (URISyntaxException e) {
            Allium.LOGGER.error("URI error", e);
        } catch (IOException e) {
            Allium.LOGGER.error("IO error - generate", e);
        }
    }

    private static void complete() {
        int i = 0;
        List<EClass<?>> sortedClasses = CLASS_DATA
                .stream()
                .<EClass<?>>map(ClassData::clazz)
                .sorted(Comparator.comparing(EClass::simpleName))
                .toList();
        List<Path> sortedPackages = PACKAGES.stream().sorted(Comparator.comparing(Path::toString)).toList();
        int size = CLASS_DATA.size() + sortedPackages.size();
        try {
            Path stylesheet = FileHelper.HTML_DOCS_DIR.resolve("style.css");
            Files.createFile(stylesheet); // Copy stylesheet
            Files.write(stylesheet,
                    Objects.requireNonNull(Generator.class.getResourceAsStream("/docs/style.css"))
                            .readAllBytes()
            );
            for (Path p : sortedPackages) {
                int percent = (int) (((double) i / size) * 100);
                System.out.print("\r\033[JFinalizing docgen - packages: " + percent + "% ");

                HTMLPackageDocument doc = new HTMLPackageDocument(p, sortedPackages, sortedClasses);
                if (!doc.isEmpty() || p.equals(ROOT)) {
                    Files.writeString(FileHelper.HTML_DOCS_DIR.resolve(p.toString()).resolve("index.html"), doc.toString());
                }
                i++;
            }
            for (ClassData entry : CLASS_DATA) {
                HTMLClassDocument doc = new HTMLClassDocument(entry.clazz, entry.classPath);
                int percent = (int) (((double) i / size) * 100);
                if (i % 50 == 0) System.out.print("\r\033[JFinalizing docgen - classes : " + percent + "% ");

                doc.addClassesToNavbar("Classes", sortedClasses);
                Files.writeString(entry.htmlPath, doc.toString());
                i++;
            }
            System.out.println();
        } catch (IOException e) {
            Allium.LOGGER.error("IO error - complete", e);
        }
    }

    private static Path getPath(URI uri, EClass<?> eClass) {
        try { // jar file?
            return FileSystems.getFileSystem(uri).getRootDirectories().iterator().next();
        } catch(Throwable ignored) { // nope.
            return Path.of(Path.of(uri).toString().replace(eClass.name().replace(".", "/") + ".class", ""));
        }
    }

    public record ClassData(Path classPath, Path htmlPath, Path luaPath, EClass<?> clazz) {}

}
