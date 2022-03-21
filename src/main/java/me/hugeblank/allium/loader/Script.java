package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.resources.AlliumResourcePack;
import me.hugeblank.allium.lua.event.Event;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Script {
    private static final Map<String, Script> SCRIPTS = new HashMap<>();

    // The Man(ifest) who can't be moved
    private final Manifest manifest;
    private final Logger logger;
    private final ScriptExecutor executor;
    private final boolean loaded;
    private boolean initialized = false;
    protected LuaValue module;
    private final Path path;

    public Script(Manifest manifest, Path path, boolean registerResources) {
        this.manifest = manifest;
        this.path = path;
        this.executor = new ScriptExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        boolean loaded;
        try {
            if (SCRIPTS.containsKey(manifest.id()))
                throw new Exception("Script with ID is already loaded!");
            SCRIPTS.put(manifest.id(), this);
            if (registerResources) AlliumResourcePack.register(this);
            loaded = true;
        } catch (Exception e) {
            getLogger().error("Could not load allium script " + getManifest().id(), e);
            unload();
            loaded = false;
        }
        this.loaded = loaded;
    }

    public Script (Manifest manifest, Path path) {
        this(manifest, path, true);
    }

    protected InputStream loadEntrypoint() throws Throwable {
        return Files.newInputStream(path.resolve(manifest.entrypoint()));
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
        AlliumResourcePack.drop(this);
    }

    public void initialize() {
        if (isInitialized()) return;
        try {
            this.module = getExecutor().initialize(loadEntrypoint());
            this.initialized = true;
        } catch (Throwable e) {
            getLogger().error("Could not initialize allium script " + getManifest().id(), e);
            unload();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // return null if file isn't contained within Scripts path, or if it doesn't exist.
    public LuaValue loadLibrary(LuaState state, Path mod) throws UnwindThrowable, LuaError {
        // Ensure the modules parent path is the root path, and that the module exists before loading
        try {
            LuaFunction loadValue = getExecutor().load(Files.newInputStream(mod), mod.getFileName().toString());
            return loadValue.call(state);
        } catch (FileNotFoundException e) {
            // This should never happen, but if it does, boy do I want to know.
            Allium.LOGGER.warn("File claimed to exist but threw a not found exception...", e);
            return null;
        } catch (CompileException | IOException e) {
            throw new LuaError(e);
        }
    }

    public LuaValue getModule() {
        return module;
    }

    public Path getPath() {
        return path;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Logger getLogger() {
        return logger;
    }

    public ScriptExecutor getExecutor() {
        return executor;
    }

    public static Script getFromID(String id) {
        return SCRIPTS.get(id);
    }

    public static LuaTable getScriptTable() {
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

    public static void unloadAll() {
        // Unused. Let's think about script reload-ability in the future.
        List<Script> scripts = new ArrayList<>(SCRIPTS.values());
        scripts.forEach(Script::unload);
    }

    @Override
    public String toString() {
        return manifest.name();
    }

    //  if ( i % 2 == 0) break;
}
