package me.hugeblank.allium.lua.type;

import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@LuaWrapped
public class LuaReadHandle extends LuaHandle {
    private static final char EOF = (char)-1;

    protected final InputStream handle;

    public LuaReadHandle(Path path) throws LuaError {
        super();
        try {
            this.handle = Files.newInputStream(path, StandardOpenOption.READ, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public String readLine(boolean withTrailing) throws LuaError {
        StringBuilder builder = new StringBuilder();
        char value;
        do {
            try {
                value = (char) handle.read();
                if (value == '\n' && withTrailing) {
                    builder.append(value);
                } else if (value != '\n') {
                    builder.append(value);
                }
            } catch (IOException e) {
                throw new LuaError(e);
            }
        } while(value != EOF && value != '\n');
        return builder.toString();
    }

    @LuaWrapped
    public String readAll() throws LuaError {
        StringBuilder builder = new StringBuilder();
        try {
            for (byte b : handle.readAllBytes()) {
                builder.append((char)b);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public String read(int count) throws LuaError {
        StringBuilder builder = new StringBuilder();
        try {
            for (int i = 0; i < count; i++) {
                char value = (char) handle.read();
                if (value == EOF) return builder.toString();
                builder.append(value);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    public void close() throws LuaError {
        closeInternal(handle);
    }
}
