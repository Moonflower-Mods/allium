package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaError;

import java.io.Closeable;
import java.io.IOException;

@LuaWrapped
public abstract class LuaHandle {

    public LuaHandle() {}

    @LuaWrapped
    public abstract void close() throws LuaError;

    public void closeInternal(Closeable handle) throws LuaError {
        try {
            handle.close();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    };
}
