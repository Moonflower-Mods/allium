package me.hugeblank.allium.util.docs;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.FileHelper;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Generator {
    // Path to class, Pair with Path to html file and Enhanced Class
    public static final Map<Path, Pair<Path, EClass<?>>> CLASSES = new HashMap<>();
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
                                    return FileVisitResult.CONTINUE;
                                if (Pattern.compile("\\$\\d*\\.class$").matcher(file.getFileName().toString()).find())
                                    return FileVisitResult.CONTINUE;
                                String className = file.toString()
                                        .replace("/", ".")
                                        .replaceFirst("\\.class$", "");
                                try {
                                    if (file.getParent() != null) {
                                        Files.createDirectories(FileHelper.DOCS_DIR.resolve(file.getParent().toString()));
                                        Path out = FileHelper.DOCS_DIR.resolve(file.toString().replace(".class", ".html"));
                                        if (!Files.exists(out)) Files.createFile(out);
                                        EClass<?> clazz = EClass.fromJava(Class.forName(className));
                                        CLASSES.put(file, new Pair<>(out, clazz));

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
        } catch (URISyntaxException e) {
            Allium.LOGGER.error("URI error", e);
        } catch (IOException e) {
            Allium.LOGGER.error("IO error - generate", e);
        }
        complete();
    }

    private static void complete() {
        int i = 0;
        List<EClass<?>> sortedClasses = CLASSES.values()
                .stream()
                .<EClass<?>>map(Pair::getRight)
                .sorted(Comparator.comparing(EClass::simpleName))
                .toList();
        List<Path> sortedPackages = PACKAGES.stream().sorted(Comparator.comparing(Path::toString)).toList();
        int size = CLASSES.size() + sortedPackages.size();
        try {
            for (Path p : sortedPackages) {
                int percent = (int) (((double) i / size) * 100);
                System.out.print("\r\033[JFinalizing docgen - packages: " + percent + "% ");

                PackageDocument doc = new PackageDocument(p, sortedPackages, sortedClasses);
                if (!doc.isEmpty() || p.equals(ROOT)) {
                    Files.writeString(FileHelper.DOCS_DIR.resolve(p.toString()).resolve("index.html"), doc.toString());
                }
                i++;
            }
            for (Map.Entry<Path, Pair<Path, EClass<?>>> entry : CLASSES.entrySet()) {
                Pair<Path, EClass<?>> pair = entry.getValue();
                ClassDocument doc = new ClassDocument(pair.getRight(), entry.getKey());
                int percent = (int) (((double) i / size) * 100);
                if (i % 50 == 0) System.out.print("\r\033[JFinalizing docgen - classes : " + percent + "% ");

                doc.addClassesToNavbar("Classes", sortedClasses);
                Files.writeString(pair.getLeft(), doc.toString());
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
}
