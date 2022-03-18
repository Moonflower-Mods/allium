package me.hugeblank.allium.loader;

import com.google.gson.Gson;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.resources.AlliumResourcePack;
import me.hugeblank.allium.lua.event.Event;
import me.hugeblank.allium.util.FileHelper;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.hugeblank.allium.Allium.PACK;

public class Plugin {
    private static final Map<String, Plugin> PLUGINS = new HashMap<>();

    private final Manifest manifest;
    private final Logger logger;
    private final PluginExecutor executor;
    private final Path path;
    private final boolean loaded;


    private Plugin(Path dir, Manifest manifest) {
        this.path = dir;
        this.manifest = manifest;
        this.executor = new PluginExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        PLUGINS.put(manifest.id(), this);
        PACK.register(path);
        boolean loaded;
        try {
            File entrypoint = dir.resolve(manifest.entrypoint()).toFile();
            executor.initialize(entrypoint);
            loaded = true;
        } catch (Exception e) {
            logger.error("Could not initialize allium script " + manifest.id(), e);
            unload();
            loaded = false;
        }
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unload() {
        PLUGINS.remove(getId(), this);
        this.executor.getState().abandon();
        for (Event e : Event.getEvents().values()) {
            List<Pair<Plugin, LuaFunction>> listeners = e.getListeners();
            listeners.removeIf(pair -> pair.getLeft().equals(this));
            PACK.drop(path);
        }
    }

    public String getId() {
        return manifest.id();
    }

    public String getVersion() {
        return manifest.version();
    }

    public String getName() {
        return manifest.name();
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return getName();
    }

    public PluginExecutor getExecutor() {
        return this.executor;
    }

    public static void unloadAll() {
        // Unused. Let's think about script reload-ability in the future.
        List<Plugin> plugins = new ArrayList<>(PLUGINS.values());
        plugins.forEach(Plugin::unload);
    }

    private static boolean checkPath(Path pluginDir) {
        String name = pluginDir.toFile().getPath();
        if (!pluginDir.toFile().isDirectory()) {
            Allium.LOGGER.warn("Attempted to load allium script from a file: " + name);
            return false;
        } else if (!FileHelper.hasManifestFile(pluginDir)) {
            Allium.LOGGER.warn("Missing " + FileHelper.MANIFEST_FILE_NAME + " in directory " + name);
            return false;
        }
        return true;
    }

    public static boolean loadFromDir(Path pluginDir) {
        File dir = pluginDir.toFile();
        if (!checkPath(pluginDir)) return false;
        File manifestJson = FileHelper.getManifestPath(dir.toPath()).toFile();
        try (FileReader reader = new FileReader(manifestJson)) {
            Manifest manifest = new Gson().fromJson(reader, Manifest.class);
            if (PLUGINS.containsKey(manifest.id())) {
                Allium.LOGGER.error(
                        "Could not load allium script with duplicate ID '" +
                        manifest.id() +
                        "' in directory " +
                        dir.getPath()
                );
            }
            Plugin plugin = new Plugin(pluginDir, manifest);
            return plugin.isLoaded();
        } catch (IOException e) {
            Allium.LOGGER.warn("Could not read " + manifestJson);
        }
        return false;
    }
}
