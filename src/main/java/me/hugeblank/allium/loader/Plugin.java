package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.event.Event;
import me.hugeblank.allium.util.FileHelper;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plugin {
    private static final Map<String, Plugin> PLUGINS = new HashMap<>();

    private String id;
    private String version; // TODO: SemVer parser in java
    private String name;
    private Logger logger;
    private PluginExecutor executor;

    public Plugin(String id, String version, String name, PluginExecutor executor) {
        this.register(id, version, name, executor);
    }

    public Plugin() {}

    public boolean register(String id, String version, String name, PluginExecutor executor) {
        // There exists a duplicate, remove everything registered by this plugin
        if (this.id == null && this.version == null && this.name == null && this.executor == null && !PLUGINS.containsKey(id)) {
            this.id = id;
            this.version = version;
            this.name = name;
            this.executor = executor;
            this.logger = LoggerFactory.getLogger('@' + this.id);
            PLUGINS.put(this.id, this);
            return true;
        } else {
            this.cleanup();
        }
        return false;
    }

    protected void cleanup() {
        PLUGINS.remove(this.id, this);
        for (Event e : Event.getEvents().values()) {
            List<Pair<Plugin, LuaFunction>> listeners = e.getListeners();
            listeners.removeIf(pair -> pair.getLeft().equals(this));
        }
    }

    public String getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public String getName() {
        return this.name;
    }

    public Logger getLogger() {
        // While loading plugin and on initial pass through main, plugin metadata is null
        if (logger == null) return Allium.LOGGER;
        return logger;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public PluginExecutor getExecutor() {
        return this.executor;
    }

    public static Plugin loadFromDir(File dir) {
        if (!FileHelper.isDirectoryPlugin(dir)) {
            Allium.LOGGER.warn("Could not load plugin in directory " + dir.getPath());
            return null;
        }
        Plugin plugin = new Plugin();
        PluginExecutor executor = new PluginExecutor(plugin);
        if (executor.initialize(plugin, FileHelper.getMainPath(dir.toPath()).toFile())) {
            return plugin;
        }
        return null;
    }
}
