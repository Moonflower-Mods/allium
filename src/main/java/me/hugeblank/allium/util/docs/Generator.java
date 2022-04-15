package me.hugeblank.allium.util.docs;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.FileHelper;
import me.hugeblank.allium.util.docs.html.HTMLDocument;
import net.minecraft.SharedConstants;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Generator {
    public static final HTMLDocument INDEX = new HTMLDocument("Minecraft for Lua");
    public static Path ROOT;

    public static void generate() {
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(SharedConstants.class.getName().replace(".", "/") + ".class");
            if (url != null) {
                URI uri = url.toURI();
                Allium.LOGGER.warn("Starting docgen, this may take a while.");
                List<Pair<Path, ClassDocument>> files = new ArrayList<>();
                List<EClass<?>> classes = new ArrayList<>();
                List<Path> packages = new ArrayList<>();
                for (Path rootDirectory : FileSystems.getFileSystem(uri).getRootDirectories()) {
                    ROOT = rootDirectory;
                    packages.add(rootDirectory);
                    rootDirectory = rootDirectory.resolve("net");
                    if (Files.exists(rootDirectory)) {
                        Path finalRootDirectory = rootDirectory;
                        Files.walkFileTree(rootDirectory, new FileVisitor<>() {
                            int fileCount;
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                if (!dir.equals(finalRootDirectory) && dir.getFileName().toString().equals("unused")) return FileVisitResult.SKIP_SUBTREE;
                                packages.add(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (Pattern.compile("\\$\\d\\.class$").matcher(file.getFileName().toString()).find()) return FileVisitResult.CONTINUE;
                                String className = file.toString()
                                        .replace("/", ".")
                                        .replace(".class", "")
                                        .substring(1);
                                try {
                                    Files.createDirectories(FileHelper.DOCS_DIR.resolve(file.getParent().toString().substring(1)));
                                    Path out = FileHelper.DOCS_DIR.resolve(file.toString().replace(".class", ".html").substring(1));
                                    if (!Files.exists(out)) Files.createFile(out);
                                    EClass<?> clazz = EClass.fromJava(Class.forName(className));
                                    ClassDocument doc = new ClassDocument(clazz, file);
                                    files.add(new Pair<>(out, doc));
                                    classes.add(clazz);
                                    if (fileCount%10 == 0) {
                                        System.out.print(".");
                                    }
                                    if (fileCount%1000 == 0) {
                                        System.out.println();
                                    }
                                    fileCount++;
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

                System.out.println();
                Allium.LOGGER.info("Docgen complete");
                int i = 0;
                List<EClass<?>> sortedClasses = classes.stream().sorted(Comparator.comparing(EClass::simpleName)).toList();
                List<Path> sortedPackages = packages.stream().sorted(Comparator.comparing(Path::toString)).toList();
                int size = files.size() + sortedPackages.size();
                for (Path p : sortedPackages) {
                    int percent = (int)(((double)i / size)*100);
                    System.out.print("\r\033[JFinalizing docgen - packages: " + percent + "% " + ("=").repeat(percent));

                    PackageDocument doc = new PackageDocument(p, sortedPackages, sortedClasses);
                    if (!doc.isEmpty()) {
                        Files.writeString(FileHelper.DOCS_DIR.resolve(p.toString().substring(1)).resolve("index.html"), doc.toString());
                    }
                    i++;
                }
                for (Pair<Path, ClassDocument> pair : files) {
                    int percent = (int)(((double)i / size)*100);
                    System.out.print("\r\033[JFinalizing docgen - classes : " + percent + "% " + ("=").repeat(percent));

                    pair.getRight().addClassesToNavbar("Classes", sortedClasses);
                    Files.writeString(pair.getLeft(), pair.getRight().toString());
                    i++;
                }
                System.out.println();
            }
        } catch (URISyntaxException e) {
            Allium.LOGGER.error("URI error", e);
        } catch (IOException e) {
            Allium.LOGGER.error("IO error", e);
        }
    }
}
