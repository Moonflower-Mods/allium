package me.hugeblank.allium.util;

import com.google.gson.Gson;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.commons.io.input.ReaderInputStream;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileHelper {
    /* Allium Script directory spec
      /allium
        /<unique dir name> | unique file name, bonus point if using the namespace ID
          /<libs and stuff>
          manifest.json |  File containing key information about the script. ID, Name, Version, Entrypoint file
    */

    public static final File SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID).toFile();
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    public static File getScriptsDirectory() {
        if (!SCRIPT_DIR.exists()) {
            Allium.LOGGER.warn("Missing allium directory, creating one for you");
            if (!SCRIPT_DIR.mkdir()) {
                Allium.LOGGER.error("Could not create allium directory, something is seriously wrong!");
                throw new RuntimeException("Failed to create allium directory", new FileSystemException(SCRIPT_DIR.toPath().toAbsolutePath().toString()));
            }
        }
        return SCRIPT_DIR;
    }

    public static Set<ScriptCandidate<Path>> getScriptDirCandidates() {
        Set<ScriptCandidate<Path>> out = new HashSet<>();
        File[] files = Objects.requireNonNull(FileHelper.getScriptsDirectory().listFiles());
        for (File pluginDir : files) {
            if (pluginDir.isDirectory() && FileHelper.hasManifestFile(pluginDir.toPath())) {
                File manifestJson = FileHelper.getManifestPath(pluginDir.toPath()).toFile();
                try (FileReader reader = new FileReader(manifestJson)) {
                    Manifest manifest = new Gson().fromJson(reader, Manifest.class);
                    out.add(new ScriptCandidate<>(manifest, pluginDir.toPath(), DirectoryScript::new));
                } catch (IOException e) {
                    Allium.LOGGER.error("Could not read " + manifestJson, e);
                }
            }
        }
        return out;
    }

    public static Set<ScriptCandidate<ZipFile>> getZipDirCandidates() {
        Set<ScriptCandidate<ZipFile>> out = new HashSet<>();
        File[] files = Objects.requireNonNull(FileHelper.getScriptsDirectory().listFiles());
        for (File pluginDir : files) {
            if (pluginDir.isFile()) {
                try {
                    ZipFile zip = new ZipFile(pluginDir);
                    InputStreamReader reader = new InputStreamReader(
                            zip.getInputStream(zip.getEntry(MANIFEST_FILE_NAME))
                    );
                    Manifest manifest = new Gson().fromJson(reader, Manifest.class);
                    out.add(new ScriptCandidate<>(manifest, zip, ZipFileScript::new));
                } catch (IOException e) {
                    // hush
                }
            }
        }
        return out;
    }

    public static Set<ScriptCandidate<ModContainer>> getModContainerCandidates() { // I have no idea if this works in production.
        Set<ScriptCandidate<ModContainer>> out = new HashSet<>();
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
                        out.add(new ScriptCandidate<>(man, container, ModContainerScript::new));
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
                                    out.add(new ScriptCandidate<>(man, container, ModContainerScript::new));
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

    public static File getResourcePackRoot(Path pluginPath) {
        return pluginPath.toFile();
    }

    public static boolean hasManifestFile(Path pluginPath) {
        return pluginPath.resolve(MANIFEST_FILE_NAME).toFile().exists();
    }

    public static Path getManifestPath(Path pluginPath) { return pluginPath.resolve(MANIFEST_FILE_NAME); }
}
