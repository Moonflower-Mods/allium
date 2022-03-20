package me.hugeblank.allium.loader;

import net.fabricmc.loader.api.ModContainer;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.UnwindThrowable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class ModContainerScript extends Script {
    private Path path;
    private File entry;


    public ModContainerScript(Manifest manifest, ModContainer container) {
        super(manifest);
        for (Path path : container.getRootPaths()) { // Search on all root paths in the mod for the manifest
            File entry = path.resolve(getManifest().entrypoint()).toFile();
            if (entry.exists()) {
                this.path = path;
                this.entry = entry;
            }
        }
    }

    @Override
    public Path getRootPath() {
        return path;
    }

    @Override
    public InputStream loadEntrypoint() throws Exception {
        if (entry == null) throw new FileNotFoundException(
                "Could not find entrypoint in script '" +
                getManifest().id() + "'."
        );
        return new FileInputStream(entry);
    }

    @Override
    public LuaValue loadLibrary(LuaState state, File mod) throws UnwindThrowable, LuaError {
        return null;
    }
}
