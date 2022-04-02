package me.hugeblank.allium.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.Entrypoint;
import me.hugeblank.allium.loader.Manifest;
import me.hugeblank.allium.loader.Script;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class FileHelper {
    /* Allium Script directory spec
      /allium
        /<unique dir name> | unique file name, bonus point if using the namespace ID
          /<libs and stuff>
          manifest.json |  File containing key information about the script. ID, Name, Version, Entrypoint file
    */

    public static final Path SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID);
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Allium.ID);
    public static final Path MAPPINGS_CFG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Allium.ID + "_mappings");
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    public static Path getScriptsDirectory() {
        if (!Files.exists(SCRIPT_DIR)) {
            Allium.LOGGER.warn("Missing allium directory, creating one for you");
            try {
                Files.createDirectory(SCRIPT_DIR);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create allium directory", new FileSystemException(SCRIPT_DIR.toAbsolutePath().toString()));
            }
        }
        return SCRIPT_DIR;
    }

    public static Set<Script> getValidDirScripts() {
        Set<Script> out = new HashSet<>();
        try {
            Stream<Path> files = Files.list(FileHelper.getScriptsDirectory());
            files.forEach((scriptDir) -> {
                Path path;
                if (Files.isDirectory(scriptDir)) {
                    path = scriptDir;
                } else {
                    try {
                        FileSystem fs = FileSystems.newFileSystem(scriptDir); // zip, tarball, whatever has a provider.
                        path = fs.getPath("/");
                    } catch (IOException | ProviderNotFoundException e) {
                        return; // Just a file we can't read, ignore it and move on.
                    }
                }
                try {
                    BufferedReader reader = Files.newBufferedReader(path.resolve(MANIFEST_FILE_NAME));
                    Manifest manifest = new Gson().fromJson(reader, Manifest.class);
                    out.add(new Script(manifest, path));
                } catch (IOException e) {
                    Allium.LOGGER.error("Could not find " + MANIFEST_FILE_NAME  + " file on path " + scriptDir, e);
                }
            });
        } catch (IOException e) {
            Allium.LOGGER.error("Could not read from scripts directory", e);
        }
        return out;
    }

    public static Set<Script> getValidModScripts() { // I have no idea if this works in production.
        Set<Script> out = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach((container) -> {
            if (container.getMetadata().getCustomValue("allium") != null) {
                ModMetadata metadata = container.getMetadata();
                try { // Let's see if the value we're after is an object
                    CustomValue.CvObject value = metadata.getCustomValue("allium").getAsObject();
                    Manifest man = makeManifest( // Make a manifest using the default values, use optional args otherwise.
                            value,
                            metadata.getId(),
                            metadata.getVersion().getFriendlyString(),
                            metadata.getName()
                    );
                    if (man == null || man.entrypoints().valid()) { // Make sure the manifest exists and has an entrypoint
                        Allium.LOGGER.error("Could not read manifest from script with ID " + metadata.getId());
                    } else {
                        Script script = scriptFromContainer(man, container);
                        if (script != null) {
                            out.add(script);
                        } else {
                            Allium.LOGGER.error("Could not find entrypoint(s) for script with ID " + metadata.getId());
                        }
                    }
                } catch (ClassCastException e) { // Not an object...
                    try { // Maybe the value is an array?
                        CustomValue.CvArray value = metadata.getCustomValue("allium").getAsArray();
                        int i = 0; // Index for developer to debug their mistakes
                        for (CustomValue v : value) { // For each array value
                            try { // test for object
                                CustomValue.CvObject obj = v.getAsObject();
                                Manifest man = makeManifest(obj); // No optional arguments here.
                                if (man.isComplete()) {
                                    Script script = scriptFromContainer(man, container);
                                    if (script != null) {
                                        out.add(script);
                                    }
                                } else { // a value was missing. Be forgiving, and continue parsing
                                    Allium.LOGGER.warn("Malformed manifest at index " + i + " of allium array block in " +
                                            "fabric.mod.json of mod '" + metadata.getId() + "'");
                                }
                                i++;
                            } catch (ClassCastException g) { // was not object. Be forgiving, and continue parsing
                                Allium.LOGGER.warn("Expected object at index " + i + " of allium array block in " +
                                        "fabric.mod.json of mod '" + metadata.getId() + "'");
                            }
                        }
                    } catch (ClassCastException f) { // Not an array...!? Someone messed up.
                        Allium.LOGGER.error("allium block for mod '" + metadata.getId() + "' not of type JSON Object or Array");
                    }
                }
            }
        });
        return out;
    }

    private static Script scriptFromContainer( Manifest man, ModContainer container) {
        final Script[] out = new Script[1];
        container.getRootPaths().forEach((path) -> {
            if (path.resolve(man.entrypoints().getStatic()).toFile().exists()) {
                // This has an incidental safeguard in the event that if multiple root paths have the same script
                // the most recent script loaded will just *overwrite* previous ones.
                out[0] = new Script(man, path);
            }
        });
        return out[0];
    }

    public static JsonElement getConfig(Script script) throws IOException {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getId() + ".json");
        if (Files.exists(path)) {
            return JsonParser.parseReader(Files.newBufferedReader(path));
        }
        return null;
    }

    public static void saveConfig(Script script, JsonElement element) throws IOException {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getId() + ".json");
        Files.deleteIfExists(path);
        OutputStream outputStream = Files.newOutputStream(path);
        String jstr = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element);
        Allium.LOGGER.info(jstr);
        outputStream.write(jstr.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }

    private static Manifest makeManifest(CustomValue.CvObject value) {
        return makeManifest(value, null, null, null);
    }

    private static Manifest makeManifest(CustomValue.CvObject value, String optId, String optVersion, String optName) {
        String id; String version; String name; CustomValue.CvObject entrypoint;

        id = value.get("id") == null ? optId : value.get("id").getAsString();
        version = value.get("version") == null ? optVersion : value.get("version").getAsString();
        name = value.get("name") == null ? optName : value.get("name").getAsString();
        entrypoint = value.get("entrypoints") == null ? null : value.get("entrypoints").getAsObject();
        if (entrypoint != null && entrypoint.containsKey("static")) {
            String eStatic = entrypoint.containsKey("static") ? entrypoint.get("static").getAsString() : null;
            String eDynamic = entrypoint.containsKey("dynamic") ? entrypoint.get("dynamic").getAsString() : null;
            return new Manifest(id, version, name, new Entrypoint(eStatic, eDynamic));
        } else {
            return null;
        }
    }
}
