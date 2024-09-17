package dev.hugeblank.bouquet.api.lib.fs;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.api.ScriptResource;
import org.squiddev.cobalt.LuaError;

import java.io.Closeable;
import java.io.IOException;

public abstract class LuaHandle implements ScriptResource {

    public LuaHandle(Script script) {
        script.registerResource(this);
    }

    public abstract void close() throws LuaError;

    public void closeInternal(Closeable handle) throws LuaError {
        try {
            handle.close();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    };
}
