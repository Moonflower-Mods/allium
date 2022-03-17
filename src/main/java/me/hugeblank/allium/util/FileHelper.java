package me.hugeblank.allium.util;

import me.hugeblank.allium.Allium;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

public class FileHelper {
    /* Allium Plugin directory spec
        /allium
            /<unique dir name> | unique file name, bonus point if using the namespace ID
                /<libs and stuff>
                main.lua | file loaded by Allium
     */

    public static final File SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID).toFile();

    public static final String RESOURCE_DIR_NAME = "resources";
    public static final String MANIFEST_FILE_NAME = "allium.script.json";
    public static final String MANIFEST_FILE_PATH = RESOURCE_DIR_NAME + "/" + MANIFEST_FILE_NAME;


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
        return pluginPath.resolve(RESOURCE_DIR_NAME).toFile();
    }

    public static boolean hasManifestFile(Path pluginPath) {
        return pluginPath.resolve(MANIFEST_FILE_PATH).toFile().exists();
    }

    public static Path getManifestPath(Path pluginPath) { return pluginPath.resolve(MANIFEST_FILE_PATH); }
}
