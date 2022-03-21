package me.hugeblank.allium.util;

import com.google.gson.Gson;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class FileHelper {
    /* Allium Script directory spec
      /allium
        /<unique dir name> | unique file name, bonus point if using the namespace ID
          /<libs and stuff>
          manifest.json |  File containing key information about the script. ID, Name, Version, Entrypoint file
    */

    public static final Path SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID);
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
                    try {
                        FileSystem fs;
                        try {
                            if (Files.isDirectory(scriptDir)) {
                                fs = new PathFileSystem(FileSystems.getDefault(), scriptDir);
                            } else {
                                fs = FileSystems.newFileSystem(scriptDir);
                            }
                            if (Files.exists(fs.getPath(MANIFEST_FILE_NAME))) {
                                BufferedReader reader = Files.newBufferedReader(fs.getPath(MANIFEST_FILE_NAME));
                                Manifest manifest = new Gson().fromJson(reader, Manifest.class);
                                out.add(new Script(manifest, fs));
                            } else {
                                Allium.LOGGER.error("Could not find " + MANIFEST_FILE_NAME  + " file on path " + scriptDir);
                            }
                        } catch (ProviderNotFoundException e) {
                            // probably just .DS_Store or some dumb OS specific equivalent.
                        }
                    } catch (IOException e) {
                        Allium.LOGGER.error("Could not read " + MANIFEST_FILE_NAME + " on path " + scriptDir, e);
                    }
            });
        } catch (IOException e) {
            // silencio.
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

                    if (man == null || man.entrypoint() == null) { // Make sure the manifest exists and has an entrypoint
                        Allium.LOGGER.error("Could not read entrypoint from mod with ID " + metadata.getId());
                    } else {
                        Script script = scriptFromContainer(man, container);
                        if (script != null) {
                            out.add(script);
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
            try {
                FileSystem fs = FileSystems.newFileSystem(path);
                if (Files.exists(fs.getPath(man.entrypoint()))) {
                    // This has an incidental safeguard in the event that multiple plugins with the same
                    // ID, the most recent script loaded will just *overwrite* previous ones.
                    out[0] = new Script(man, fs);
                }
            } catch (IOException e) {
                // hush
            }
        });
        return out[0];
    }

    private static Manifest makeManifest(CustomValue.CvObject value) {
        return makeManifest(value, null, null, null);
    }

    private static Manifest makeManifest(CustomValue.CvObject value, String optId, String optVersion, String optName) {
        String id; String version; String name; String entrypoint;

        id = value.get("id") == null ? optId : value.get("id").getAsString();
        version = value.get("version") == null ? optVersion : value.get("version").getAsString();
        name = value.get("name") == null ? optName : value.get("name").getAsString();
        entrypoint = value.get("entrypoint") == null ? null : value.get("entrypoint").getAsString();

        return entrypoint == null ? null : new Manifest(id, version, name, entrypoint);
    }

    public static Path getResourcePackRoot(Path pluginPath) {
        return pluginPath;
    }

    public static boolean hasManifestFile(Path pluginPath) {
        return Files.exists(pluginPath.resolve(MANIFEST_FILE_NAME));
    }
}
