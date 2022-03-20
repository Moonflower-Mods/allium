package me.hugeblank.allium.loader;

import me.hugeblank.allium.lua.event.Event;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.hugeblank.allium.Allium.PACK;

public abstract class Script {
    private static final Map<String, Script> SCRIPTS = new HashMap<>();

    // The Man(ifest) who can't be moved
    private final Manifest manifest;
    private final Logger logger;
    private final ScriptExecutor executor;
    private final boolean loaded;
    private boolean initialized = false;
    protected LuaValue module;

    public Script(Manifest manifest) {
        this.manifest = manifest;
        this.executor = new ScriptExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        boolean loaded;
        try {
            if (SCRIPTS.containsKey(manifest.id()))
                throw new Exception("Script with ID is already loaded!");
            SCRIPTS.put(manifest.id(), this);
                PACK.register(this);
            loaded = true;
        } catch (Exception e) {
            getLogger().error("Could not load allium script " + getManifest().id(), e);
            unload();
            loaded = false;
        }
        this.loaded = loaded;
    }

    public abstract Path getRootPath();

    protected abstract InputStream loadEntrypoint() throws Throwable;

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
        PACK.drop(this);
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

    public static void initializeAll() {
        SCRIPTS.forEach((id, script) -> {
            if (!script.isInitialized()) script.initialize();
        });
    }

    // return null if file isn't contained within Scripts path, or if it doesn't exist.
    public abstract LuaValue loadLibrary(LuaState state, File mod) throws UnwindThrowable, LuaError;

    public LuaValue getModule() {
        return module;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Logger getLogger() {
        return logger;
    }

    public ScriptExecutor getExecutor() {
        return this.executor;
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
