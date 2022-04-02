package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@LuaWrapped
public class LuaWriteHandle extends LuaHandle {
    private final OutputStream handle;

    public LuaWriteHandle(Path path, boolean append) throws LuaError {
        super();
        try {
            if (append) {
                this.handle = Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            } else {
                this.handle = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public void write(String out) throws LuaError {
        for (char c : out.toCharArray()) {
            try {
                handle.write(c);
            } catch (IOException e) {
                throw new LuaError(e);
            }
        }
    }

    @LuaWrapped
    public void writeLine(String out) throws LuaError {
        try {
            for (char c : out.toCharArray()) {
                handle.write(c);
            }
            handle.write('\n');
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public void flush() throws LuaError {
        try {
            handle.flush();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    public void close() throws LuaError {
        closeInternal(handle);
    }
}
