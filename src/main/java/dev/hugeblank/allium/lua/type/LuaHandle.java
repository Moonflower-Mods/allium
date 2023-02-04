package dev.hugeblank.allium.lua.type;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptResource;
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
