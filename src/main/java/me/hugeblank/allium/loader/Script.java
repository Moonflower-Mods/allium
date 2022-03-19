package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.event.Event;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.hugeblank.allium.Allium.PACK;

public class Script {
    private static final Map<String, Script> SCRIPTS = new HashMap<>();

    private final Manifest manifest;
    private final Logger logger;
    private final ScriptExecutor executor;
    private final boolean loaded;
    private final Path path;
    private LuaValue module;

    public Script(Manifest manifest, Path path) {
        this.path = path;
        this.manifest = manifest;
        this.executor = new ScriptExecutor(this);
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
        boolean loaded;
        try {
            if (SCRIPTS.containsKey(manifest.id()))
                throw new Exception("Could not load allium script on path " + path + " with duplicate ID");
            SCRIPTS.put(manifest.id(), this);
            this.module = getExecutor().initialize(new FileInputStream(path.resolve(manifest.entrypoint()).toFile()));
            loaded = true;
            PACK.register(path);
        } catch (UnwindThrowable | Exception e) {
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

    public static Script getFromID(String id) {
        return SCRIPTS.get(id);
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getRootPath() {
        return path;
    }

    public LuaValue getModule() {
        return module;
    }

    @Override
    public String toString() {
        return manifest.name();
    }

    public ScriptExecutor getExecutor() {
        return this.executor;
    }

    public LuaValue loadLibrary(LuaState state, File module) throws UnwindThrowable, LuaError {
        // Ensure the modules parent path is the root path, and that the module exists before loading
        if (!(module.toPath().startsWith(getRootPath()) && module.exists())) return null;
        try {
            LuaFunction loadValue = getExecutor().load(new FileInputStream(module), module.getName());
            return loadValue.call(state);
        } catch (FileNotFoundException e) {
            // This should never happen, but if it does, boy do I want to know.
            Allium.LOGGER.warn("File claimed to exist but threw a not found exception...", e);
            return null;
        } catch (CompileException | IOException e) {
            throw new LuaError(e);
        }
    }

    public static void unloadAll() {
        // Unused. Let's think about script reload-ability in the future.
        List<Script> scripts = new ArrayList<>(SCRIPTS.values());
        scripts.forEach(Script::unload);
    }

    public static Script fromContainerSafe(Manifest man, ModContainer container) {
        for (Path path : container.getRootPaths()) { // Search on all root paths in the mod for this file
            if (path.resolve(man.entrypoint()).toFile().exists()) {
                return createSafe(man, path);
            }
        }
        return null;
    }

    public static Script createSafe(Manifest man, Path path) {
        if (SCRIPTS.containsKey(man.id())) return null;
        return new Script(man, path);
    }
}
