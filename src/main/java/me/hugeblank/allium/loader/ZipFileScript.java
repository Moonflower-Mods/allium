package me.hugeblank.allium.loader;

import me.hugeblank.allium.Allium;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileScript extends Script {
    private final ZipFile zip;

    public ZipFileScript(Manifest manifest, ZipFile zip) {
        super(manifest);
        this.zip = zip;
    }

    public ZipFile getZip() {
        return zip;
    }

    @Override
    public Path getRootPath() {
        return Path.of(zip.getName());
    }

    @Override
    protected InputStream loadEntrypoint() throws Throwable {
        return zip.getInputStream(zip.getEntry(getManifest().entrypoint()));
    }

    @Override
    public LuaValue loadLibrary(LuaState state, File mod) throws UnwindThrowable, LuaError {
        if (mod.toPath().startsWith(getRootPath())) {
            try {
                // Snip off the zip file path, leaving just the sub-path
                String subPath = mod.toString().replace(getRootPath().toString() + "/", "");
                ZipEntry entry = zip.getEntry(subPath);
                if (entry == null) return null;
                InputStream stream = zip.getInputStream(entry);
                return getExecutor().load(stream, subPath).call(state);
            } catch (CompileException e) {
                throw new LuaError(e);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
