package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.resources.AlliumResourcePack;
import me.hugeblank.allium.lua.event.Event;
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
import java.util.HashMap;
import java.util.Map;

public class Script {
    private static final Map<String, Script> SCRIPTS = new HashMap<>();

    // The Man(ifest) who can't be moved
    private final Manifest manifest;
    private final Logger logger;
    private ScriptExecutor executor;
    // Whether this script was able to register itself
    private boolean initialized = false; // Whether this scripts Lua side (static and dynamic) was able to execute
    protected LuaValue module;
    private final Path path;

    public Script (Manifest manifest, Path path) {
        this.manifest = manifest;
        this.path = path;
        this.executor = new ScriptExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        try {
            if (SCRIPTS.containsKey(manifest.id()))
                throw new Exception("Script with ID is already loaded!");
            SCRIPTS.put(manifest.id(), this);
            AlliumResourcePack.register(this);
        } catch (Exception e) {
            getLogger().error("Could not load allium script " + getManifest().id(), e);
            unload();
        }
    }

    public void reload() {
        // Remove listeners
        for (Event e : Event.getEvents().values()) {
            e.removeAllListeners(this);
        }
        // Re-run dynamic entrypoint again
        try {
            InputStream dynamicEntrypoint = manifest.entrypoints().containsDynamic() ?
                    Files.newInputStream(path.resolve(manifest.entrypoints().getDynamic())) :
                    null;
            // Reload and set the module if all that's provided is a dynamic script
            this.module = manifest.entrypoints().getType().equals(Entrypoint.Type.DYNAMIC) ?
                    executor.reload(dynamicEntrypoint).arg(1) :
                    this.module;
        } catch (Throwable e) {
            getLogger().error("Could not reload allium script " + getManifest().id(), e);
            unload();
        }

    }

    public void unload() {
        SCRIPTS.remove(manifest.name(), this);
        this.executor.getState().abandon();
        for (Event e : Event.getEvents().values()) {
            e.removeAllListeners(this);
        }
        AlliumResourcePack.drop(this);
    }

    public void initialize() {
        if (isInitialized()) return;
        try {
            // Create InputStreams for each entrypoint, if it exists
            InputStream staticEntrypoint = manifest.entrypoints().containsStatic() ?
                    Files.newInputStream(path.resolve(manifest.entrypoints().getStatic())) :
                    null;
            InputStream dynamicEntrypoint = manifest.entrypoints().containsDynamic() ?
                    Files.newInputStream(path.resolve(manifest.entrypoints().getDynamic())) :
                    null;
            // Initialize and set module used by require
            this.module = getExecutor().initialize(staticEntrypoint, dynamicEntrypoint).arg(1);
            this.initialized = true; // If all these steps are successful, we can set initialized to true
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

    @Override
    public String toString() {
        return manifest.name();
    }

    //  if ( i % 2 == 0) break;
}
