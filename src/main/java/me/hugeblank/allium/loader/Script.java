package me.hugeblank.allium.loader;

import com.google.gson.Gson;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.event.Event;
import me.hugeblank.allium.util.FileHelper;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static me.hugeblank.allium.Allium.PACK;

public class Script {
    private static final Map<String, Script> SCRIPTS = new HashMap<>();

    private final Manifest manifest;
    private final Logger logger;
    private final ScriptExecutor executor;
    private final boolean loaded;
    private final Path path;

    Script(Path path, Manifest manifest) {
        this.path = path;
        this.manifest = manifest;
        this.executor = new ScriptExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        SCRIPTS.put(manifest.id(), this);
        boolean loaded;
        try {
            getExecutor().initialize(new FileInputStream(path.resolve(manifest.entrypoint()).toFile()));
            loaded = true;
            PACK.register(path);
        } catch (Exception e) {
            getLogger().error("Could not initialize allium script " + getManifest().id(), e);
            unload();
            loaded = false;
        }
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unload() {
        SCRIPTS.remove(manifest.name(), this);
        this.executor.getState().abandon();
        for (Event e : Event.getEvents().values()) {
            List<Pair<Script, LuaFunction>> listeners = e.getListeners();
            listeners.removeIf(pair -> pair.getLeft().equals(this));
        }
        if (path != null) PACK.drop(path);
    }

    public static LuaTable getScriptList() {
        LuaTable out = new LuaTable();
        SCRIPTS.forEach((key, value) -> {
            Manifest man = value.getManifest();
            LuaTable luaMan = new LuaTable();
            luaMan.rawset("id", LuaString.valueOf(man.id()));
            luaMan.rawset("version", LuaString.valueOf(man.version()));
            luaMan.rawset("name", LuaString.valueOf(man.name()));
            out.rawset(key, luaMan);
        });
        return out;
    }

    public Manifest getManifest() {
        return this.manifest;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return manifest.name();
    }

    public ScriptExecutor getExecutor() {
        return this.executor;
    }

    public static void unloadAll() {
        // Unused. Let's think about script reload-ability in the future.
        List<Script> scripts = new ArrayList<>(SCRIPTS.values());
        scripts.forEach(Script::unload);
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

    public static boolean fromDir(Path pluginDir) {
        File dir = pluginDir.toFile();
        if (!checkPath(pluginDir)) return false;
        File manifestJson = FileHelper.getManifestPath(dir.toPath()).toFile();
        try (FileReader reader = new FileReader(manifestJson)) {
            Manifest manifest = new Gson().fromJson(reader, Manifest.class);
            if (SCRIPTS.containsKey(manifest.id())) {
                Allium.LOGGER.error(
                        "Could not load allium script with duplicate ID '" +
                        manifest.id() +
                        "' in directory " +
                        dir.getPath()
                );
            }
            Script script = new Script(pluginDir, manifest);
            return script.isLoaded();
        } catch (IOException e) {
            Allium.LOGGER.warn("Could not read " + manifestJson);
        }
        return false;
    }

    public static boolean fromMod(ModContainer container) { // I have no idea if this works in production.
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
                return false;
            }
            return loadFromContainer(container, man);
        } catch (ClassCastException e) { // Not an object...
            try { // Maybe the value is an array?
                CustomValue.CvArray value = metadata.getCustomValue("allium").getAsArray();
                int i = 0; // Index for developer to debug their mistakes
                for (CustomValue v : value) { // For each array value
                    try { // test for object
                        CustomValue.CvObject obj = v.getAsObject();
                        Manifest man = makeManifest(obj); // No optional arguments here.
                        if (man.isComplete()) {
                            loadFromContainer(container, man);
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
        return false;
    }

    private static boolean loadFromContainer(ModContainer container, Manifest man) {
        for (Path path : container.getRootPaths()) { // Search on all root paths in the mod for this file
            if (path.resolve(man.entrypoint()).toFile().exists()) {
                Script script = new Script(path, man);
                return script.isLoaded();
            }
        }
        return false;
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
}
