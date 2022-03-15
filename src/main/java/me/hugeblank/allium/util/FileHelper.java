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

    public static final File PLUGIN_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID).toFile();

    public static final String MAIN_FILE_NAME = "main.lua";
    public static final String MANIFEST_FILE_NAME = "manifest.json";


    public static File getPluginsDirectory() {
        if (!PLUGIN_DIR.exists()) {
            Allium.LOGGER.warn("Missing allium directory, creating one for you");
            if (!PLUGIN_DIR.mkdir()) {
                Allium.LOGGER.error("Could not create allium directory, something is seriously wrong!");
                throw new RuntimeException("Failed to create allium directory", new FileSystemException(PLUGIN_DIR.toPath().toAbsolutePath().toString()));
            }
        }
        return PLUGIN_DIR;
    }

    public static boolean hasMainFile(Path pluginPath) {
        return pluginPath.resolve(MAIN_FILE_NAME).toFile().exists();
    }

    public static boolean hasManifestFile(Path pluginPath) {
        return pluginPath.resolve(MANIFEST_FILE_NAME).toFile().exists();
    }

    public static Path getMainPath(Path pluginPath) {
        return pluginPath.resolve(MAIN_FILE_NAME);
    }

    public static Path getManifestPath(Path pluginPath) { return pluginPath.resolve(MANIFEST_FILE_NAME); }
}
