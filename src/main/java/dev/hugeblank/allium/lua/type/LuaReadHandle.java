package dev.hugeblank.allium.lua.type;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.lua.type.annotation.OptionalArg;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@LuaWrapped
public class LuaReadHandle extends LuaHandle {
    private static final char EOF = (char)-1;

    protected final InputStream handle;

    public LuaReadHandle(Script script, Path path) throws LuaError {
        super(script);
        try {
            this.handle = Files.newInputStream(path, StandardOpenOption.READ, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public String readLine(@OptionalArg Boolean withTrailing) throws LuaError {
        StringBuilder builder = new StringBuilder();
        char value;
        do {
            try {
                value = (char) handle.read();
                if (value == '\n' && withTrailing != null && withTrailing) {
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
    public LuaValue read(@OptionalArg Integer count) throws LuaError {
        StringBuilder builder = new StringBuilder();
        try {
            if (count != null) {
                for (int i = 0; i < count; i++) {
                    char value = (char) handle.read();
                    if (value == EOF) return ValueFactory.valueOf(builder.toString());
                    builder.append(value);
                }
                return ValueFactory.valueOf(builder.toString());
            } else {
                return ValueFactory.valueOf(handle.read());
            }
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    @Override
    public void close() throws LuaError {
        closeInternal(handle);
    }
}
