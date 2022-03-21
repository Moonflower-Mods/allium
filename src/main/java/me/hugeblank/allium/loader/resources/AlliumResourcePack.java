/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Heavily stripped/modified version of FabricAPI's AlliumResourcePack
package me.hugeblank.allium.loader.resources;

import me.hugeblank.allium.loader.Script;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class AlliumResourcePack extends AbstractFileResourcePack {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlliumResourcePack.class);
    private static final Pattern RESOURCE_PACK_PATH = Pattern.compile("[a-z0-9-_.]+");
    private static final List<Script> BASES = new ArrayList<>();

    public static void register(Script script) {
        BASES.add(script);
    }

    public static void drop(Script script) {
        BASES.remove(script);
    }

    private final String name;
    private final Map<Script, Path> basePaths;
    private final AutoCloseable closer;
    private final Map<ResourceType, Set<String>> namespaces;

    public static AlliumResourcePack create(String name) {
        Map<Script, Path> paths = new HashMap<>();

        BASES.forEach((script) -> paths.put(script, script.getPath()));

        return new AlliumResourcePack(name, paths, null);
    }

    private AlliumResourcePack(String name, Map<Script, Path> paths, AutoCloseable closer) {
        super(null);

        this.name = name;
        this.basePaths = paths;
        this.closer = closer;
        this.namespaces = readNamespaces(paths);
    }

    private static Map<ResourceType, Set<String>> readNamespaces(Map<Script, Path> paths) {
        Map<ResourceType, Set<String>> ret = new EnumMap<>(ResourceType.class);

        for (ResourceType type : ResourceType.values()) {
            Set<String> namespaces = new HashSet<>();
            paths.forEach((script, path) -> {
                Path dir = path.resolve(type.getDirectory());
                if (!Files.isDirectory(dir)) return;

                String separator = path.getFileSystem().getSeparator();

                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for (Path p : ds) {
                        if (!Files.isDirectory(p)) continue;

                        String s = p.getFileName().toString();
                        // s may contain trailing slashes, remove them
                        s = s.replace(separator, "");

                        if (!RESOURCE_PACK_PATH.matcher(s).matches()) {
                            LOGGER.warn("NioResourcePack: ignored invalid namespace: {} in script ID {}", s, script.getManifest().id());
                            continue;
                        }

                        namespaces.add(s);
                    }
                } catch (IOException e) {
                    LOGGER.warn("getNamespaces on script " + script.getManifest().id() + " failed!", e);
                }
            });

            if (!namespaces.isEmpty()) ret.put(type, namespaces);
        }

        return ret;
    }

    private Map.Entry<Script, Path> getPath(String filename) {
        if (hasAbsentNs(filename)) return null;

        for (Map.Entry<Script, Path> entry : basePaths.entrySet()) {
            Path childPath = entry.getKey().getPath().resolve(filename.replace("/", entry.getValue().getFileSystem().getSeparator()));
            if (Files.exists(childPath)) {
                // This is bad, YUCK! Don't do this! I get to because I'm running on no sleep. I'm suffering from a
                // crippling addiction to working on this project. You are not.
                return new Map.Entry<>() {
                    @Override
                    public Script getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public Path getValue() {
                        return entry.getValue().resolve(childPath);
                    }

                    @Override
                    public Path setValue(Path value) {
                        return null;
                    }
                };
            }
        }

        return null;
    }

    private static final String resPrefix = ResourceType.CLIENT_RESOURCES.getDirectory()+"/";
    private static final String dataPrefix = ResourceType.SERVER_DATA.getDirectory()+"/";

    private boolean hasAbsentNs(String filename) {
        int prefixLen;
        ResourceType type;

        if (filename.startsWith(resPrefix)) {
            prefixLen = resPrefix.length();
            type = ResourceType.CLIENT_RESOURCES;
        } else if (filename.startsWith(dataPrefix)) {
            prefixLen = dataPrefix.length();
            type = ResourceType.SERVER_DATA;
        } else {
            return false;
        }

        int nsEnd = filename.indexOf('/', prefixLen);
        if (nsEnd < 0) return false;

        return !namespaces.get(type).contains(filename.substring(prefixLen, nsEnd));
    }

    @Override
    protected InputStream openFile(String filename) throws IOException {
        if (filename.equals("pack.mcmeta")) {
            return AlliumResourcePack.class.getResourceAsStream("/assets/pack.mcmeta");
        } else if (filename.equals("pack.png")) {
            return AlliumResourcePack.class.getResourceAsStream("/assets/allium/icon.png");
        }

        Map.Entry<Script, Path> entry = getPath(filename);

        if (entry != null) {
            return Files.newInputStream(entry.getValue());
        }
        throw new FileNotFoundException("\"" + filename + "\" in Allium resource pack");
    }

    @Override
    protected boolean containsFile(String filename) {
        Map.Entry<Script, Path> entry = getPath(filename);
        return entry != null;
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, String path, int depth, Predicate<String> predicate) {
        if (!namespaces.getOrDefault(type, Collections.emptySet()).contains(namespace)) {
            return Collections.emptyList();
        }

        List<Identifier> ids = new ArrayList<>();

        for (Map.Entry<Script, Path> basePath : basePaths.entrySet()) {
            String separator = basePath.getValue().getFileSystem().getSeparator();
            Path nsPath = basePath.getValue().resolve(type.getDirectory()).resolve(namespace);
            Path searchPath = nsPath.resolve(path.replace("/", separator)).normalize();
            if (!Files.exists(searchPath)) continue;

            try {
                Files.walkFileTree(searchPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String fileName = file.getFileName().toString();

                        if (!fileName.endsWith(".mcmeta")
                                && predicate.test(fileName)) {
                            try {
                                ids.add(new Identifier(namespace, nsPath.relativize(file).toString().replace(separator, "/")));
                            } catch (InvalidIdentifierException e) {
                                LOGGER.error(e.getMessage());
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOGGER.warn("findResources at " + path + " in namespace " + namespace + ", script " + basePath.getKey().getManifest().id() + " failed!", e);
            }
        }

        return ids;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return namespaces.getOrDefault(type, Collections.emptySet());
    }

    @Override
    public void close() {
        if (closer != null) {
            try {
                closer.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }
}

