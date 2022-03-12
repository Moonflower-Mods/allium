// Derived from:
// https://github.com/natanfudge/Not-Enough-Crashes/blob/b5119154d9b7ba8f08656b82e87158860f0e5489/fabric/src/main/java/fudge/notenoughcrashes/fabric/YarnVersion.java
//  and https://github.com/natanfudge/Not-Enough-Crashes/blob/b5119154d9b7ba8f08656b82e87158860f0e5489/fabric/src/main/java/fudge/notenoughcrashes/fabric/StacktraceDeobfuscator.java
// Nathanfudge has the entirety of my heart and I owe them my life for this
package me.hugeblank.allium.util;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import me.hugeblank.allium.Allium;
import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;
import net.minecraft.MinecraftVersion;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class YarnLoader {

    private static final String MAPPINGS_JAR_LOCATION = "mappings/mappings.tiny";
    private static final Path CACHED_MAPPINGS = Allium.CONFIG_DIR.resolve("mappings-" +  MinecraftVersion.create().getName() + ".tiny");
    private static final Path VERSION_FILE = Allium.CONFIG_DIR.resolve("yarn-version.txt");

    private static final String NAMESPACE_FROM = "intermediary";
    private static final String NAMESPACE_TO = "named";

    public static Map<String, String> init() {
        try {
            return loadOrCreateMappings();
        } catch (Exception e) {
            Allium.LOGGER.error("Failed to load mappings!");
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> loadOrCreateMappings() throws IOException {
        // Unlike NEC, it's imperative that allium has these mappings otherwise all methods
        // will be intermediary names. not good.
        if (!Files.exists(CACHED_MAPPINGS)) {
            String yarnVersion = YarnVersion.getLatestBuildForCurrentVersion();

            Allium.LOGGER.info("Downloading deobfuscation mappings: " + yarnVersion + " for the first launch");

            String encodedYarnVersion = UrlEscapers.urlFragmentEscaper().escape(yarnVersion);
            // Download V2 jar
            String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/yarn/" + encodedYarnVersion + "/yarn-" + encodedYarnVersion + "-v2.jar";

            try {
                Files.createDirectories(Allium.CONFIG_DIR);
            } catch (IOException e) {
                Allium.LOGGER.error("Could not create " + Allium.ID + " directory!");
                throw e;
            }

            File jarFile = Allium.CONFIG_DIR.resolve("yarn-mappings.jar").toFile();
            jarFile.deleteOnExit();
            try {
                FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
            } catch (IOException e) {
                Allium.LOGGER.error("Failed to downloads mappings!");
                throw e;
            }

            try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
                ensureDirectoryExists();
                Files.copy(jar.getPath(MAPPINGS_JAR_LOCATION), CACHED_MAPPINGS, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Allium.LOGGER.error("Failed to extract mappings!");
                throw e;
            }
        }

        Map<String, String> mappings = new HashMap<>();

        try (BufferedReader mappingReader = Files.newBufferedReader(CACHED_MAPPINGS)) {
            TinyV2Factory.visit(mappingReader, new TinyVisitor() {
                private final Map<String, Integer> namespaceStringToColumn = new HashMap<>();

                private void addMappings(MappingGetter name) {
                    mappings.put(name.get(namespaceStringToColumn.get(NAMESPACE_FROM)).replace('/', '.'),
                            name.get(namespaceStringToColumn.get(NAMESPACE_TO)).replace('/', '.'));
                }

                @Override
                public void start(TinyMetadata metadata) {
                    namespaceStringToColumn.put(NAMESPACE_FROM, metadata.index(NAMESPACE_FROM));
                    namespaceStringToColumn.put(NAMESPACE_TO, metadata.index(NAMESPACE_TO));
                }

                @Override
                public void pushClass(MappingGetter name) {
                    addMappings(name);
                }

                @Override
                public void pushMethod(MappingGetter name, String descriptor) {
                    addMappings(name);
                }

                @Override
                public void pushField(MappingGetter name, String descriptor) {
                    // Allium doesn't use fields... yet... maybe.
                    // addMappings(name);
                }
            });

        } catch (IOException e) {
            Allium.LOGGER.error("Could not load mappings");
            throw e;
        }

        return mappings;
    }

    private static class YarnVersion {
        private static final String YARN_API_ENTRYPOINT = "https://meta.fabricmc.net/v2/versions/yarn/" + MinecraftVersion.create().getName();
        private static String versionMemCache = null;
        public int build;
        public String version;


        public static String getLatestBuildForCurrentVersion() throws IOException {
            if (versionMemCache == null) {
                if (!Files.exists(VERSION_FILE)) {
                    URL url = new URL(YARN_API_ENTRYPOINT);
                    URLConnection request = url.openConnection();
                    request.connect();

                    InputStream response = (InputStream) request.getContent();
                    YarnVersion[] versions = new Gson().fromJson(new InputStreamReader(response), YarnVersion[].class);
                    if (versions.length == 0) {
                        throw new IllegalStateException("No yarn versions were received at the API endpoint. Received json: " + getString(response));
                    }
                    String version = Arrays.stream(versions).max(Comparator.comparingInt(v -> v.build)).get().version;
                    ensureDirectoryExists();
                    Files.write(VERSION_FILE, version.getBytes());
                    versionMemCache = version;
                } else {
                    versionMemCache = new String(Files.readAllBytes(VERSION_FILE));
                }
            }

            return versionMemCache;
        }

        private static String getString(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void ensureDirectoryExists() throws FileSystemException {
        if (!Allium.CONFIG_DIR.toFile().exists() && !Allium.CONFIG_DIR.toFile().mkdir())
            throw new FileSystemException("Could not create allium config directory");
    }
}
