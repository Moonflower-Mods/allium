package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Plugin {
    protected String id;
    protected String version; // TODO: SemVer parser in java
    protected String name;
    protected Logger logger;
    protected PluginExecutor executor;

    public Plugin(String id, String version, String name, PluginExecutor executor) {
        this.id = id; this.version = version; this.name = name; this.executor = executor;
        this.logger = LoggerFactory.getLogger('@' + this.id);
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
        };
        PluginExecutor executor = new PluginExecutor();
        return executor.initialize(FileHelper.getMainPath(dir.toPath()).toFile());
    }
}
