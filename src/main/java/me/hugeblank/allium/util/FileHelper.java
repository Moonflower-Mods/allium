package me.hugeblank.allium.util;

import me.hugeblank.allium.Allium;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

public class FileHelper {
    /* Allium Script directory spec
      /allium
        /<unique dir name> | unique file name, bonus point if using the namespace ID
          /<libs and stuff>
          manifest.json |  File containing key information about the script. ID, Name, Version, Entrypoint file
    */

    public static final File SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID).toFile();
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    public static File getPluginsDirectory() {
        if (!SCRIPT_DIR.exists()) {
            Allium.LOGGER.warn("Missing allium directory, creating one for you");
            if (!SCRIPT_DIR.mkdir()) {
                Allium.LOGGER.error("Could not create allium directory, something is seriously wrong!");
                throw new RuntimeException("Failed to create allium directory", new FileSystemException(SCRIPT_DIR.toPath().toAbsolutePath().toString()));
            }
        }
        return SCRIPT_DIR;
    }

    public static File getResourcePackRoot(Path pluginPath) {
        return pluginPath.toFile();
    }

    public static boolean hasManifestFile(Path pluginPath) {
        return pluginPath.resolve(MANIFEST_FILE_NAME).toFile().exists();
    }

    public static Path getManifestPath(Path pluginPath) { return pluginPath.resolve(MANIFEST_FILE_NAME); }
}
