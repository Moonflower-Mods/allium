package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.*;
import java.nio.file.Path;

import static me.hugeblank.allium.Allium.PACK;

public class DirectoryScript extends Script {
    private final Path path;

    public DirectoryScript(Manifest manifest, Path path) {
        super(manifest);
        this.path = path;
    }

    @Override
    public Path getRootPath() {
        return path;
    }

    @Override
    public InputStream loadEntrypoint() throws Throwable {
        return new FileInputStream(path.resolve(getManifest().entrypoint()).toFile());
    }

    public LuaValue loadLibrary(LuaState state, File mod) throws UnwindThrowable, LuaError {
        // Ensure the modules parent path is the root path, and that the module exists before loading
        if (!(mod.toPath().startsWith(this.path) && mod.exists())) return null;
        try {
            LuaFunction loadValue = getExecutor().load(new FileInputStream(mod), mod.getName());
            return loadValue.call(state);
        } catch (FileNotFoundException e) {
            // This should never happen, but if it does, boy do I want to know.
            Allium.LOGGER.warn("File claimed to exist but threw a not found exception...", e);
            return null;
        } catch (CompileException | IOException e) {
            throw new LuaError(e);
        }
    }
}
