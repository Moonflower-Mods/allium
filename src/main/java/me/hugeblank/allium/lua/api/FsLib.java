package me.hugeblank.allium.lua.api;

import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.type.*;
import me.hugeblank.allium.util.FileHelper;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@LuaWrapped(name = "fs")
public class FsLib implements WrappedLuaLibrary {
    private final Script script;
    private final Path root;

    // Creates a persistent file storage outside of the script, since the scripts path could be in a mod or zip
    // Files cannot be created in mods/zips from what I can tell.
    public FsLib(Script script) {
        this(script, FileHelper.PERSISTENCE_DIR.resolve(script.getId()));
    }

    public FsLib(Script script, Path root) {
        this.script = script;
        this.root = root;
    }

    private Path sanitize(String str) {
        if (str.charAt(0) == '/') {
            Path p = root.resolve("." + str);
             if (p.compareTo(root) < 0) {
                 return root;
             }
            return root.resolve("." + str);
        } else {
            return root.resolve(str);
        }
    }

    @LuaWrapped
    public FsLib create(String path) {
        return new FsLib(this.script, Path.of(path));
    }

    @LuaWrapped
    public LuaTable list(String path) throws LuaError {
        Path contents = sanitize(path);
        LuaTable out = new LuaTable();
        try {
            int i = 1;
            for (Path p : Files.list(contents).toList()) {
                out.rawset(i++, ValueFactory.valueOf(String.valueOf(p.getFileName())));
            }
            return out;
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public String combine(String path, String... strs) {
        return Path.of(path, strs).toString();
    }

    @LuaWrapped
    public String getName(String path) {
        return Path.of(path).getFileName().toString();
    }

    @LuaWrapped
    public String getDir(String path) {
        return Path.of(path).getParent().toString();
    }

    @LuaWrapped
    public long getSize(String path) throws LuaError {
        try {
            return Files.size(sanitize(path));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public boolean exists(String path) {
        return Files.exists(sanitize(path));
    }

    @LuaWrapped
    public boolean isDir(String path) {
        return exists(path) && Files.isDirectory(sanitize(path));
    }

    @LuaWrapped
    public boolean isReadOnly(String path) {
        return exists(path) && !Files.isWritable(sanitize(path));
    }

    @LuaWrapped
    public void makeDir(String path) throws LuaError {
        try {
            Files.createDirectories(sanitize(path));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public void move(String source, String dest) throws LuaError {
        try {
            Files.move(sanitize(source), sanitize(dest));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public void copy(String source, String dest) throws LuaError {
        try {
            Files.copy(sanitize(source), sanitize(dest));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public void delete(String path) throws LuaError {
        try {
            deleteInternal(sanitize(path));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    private void deleteInternal(Path path) throws IOException, LuaError {
        try {
            Files.delete(path);
        } catch (DirectoryNotEmptyException e) {
            try {
                Stream<Path> list = Files.list(path);
                for (Path p : list.toList()) {
                    deleteInternal(p);
                }
            } catch (IOException ex) {
                throw new LuaError(e);
            }
        }
    }

    @LuaWrapped
    public LuaHandle open(String path, String mode) throws LuaError {
        if (mode.length() == 1) {
            Path p = sanitize(path);
            try {
                Files.createDirectories(p.getParent());
            } catch (IOException e) {
                throw new LuaError(e);
            }
            return switch (mode.charAt(0)) {
                case 'r' -> new LuaReadHandle(script, p);
                case 'w' -> new LuaWriteHandle(script, p, false);
                case 'a' -> new LuaWriteHandle(script, p, true);
                default -> null;
            };
        } else if (mode.length() == 2) {
            // TODO binary handles
            return null;
        } else {
            throw new LuaError("Invalid mode " + mode);
        }
    }

    // Differs from cct documentation
    // just returns the free space of drive holding root
    @LuaWrapped
    public long getFreeSpace() throws LuaError {
        try {
            return Files.getFileStore(root).getUsableSpace();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public @CoerceToNative List<String> find(String query) throws LuaError {
        List<String> out = new ArrayList<>();
        findInternal(root, query, out);
        return out;
    }

    private void findInternal(Path base, String query, List<String> out) throws LuaError {
        try {
            String[] routes = query.split("/");
            if (routes.length == 1 && routes[0].isEmpty()) {
                out.add(base.toString().replace(root + "/", ""));
                return;
            }
            String route = routes[0];
            String expr = route.replace("*", "(|.*)");
            Stream<Path> list = Files.list(base);
            for (Path path : list.toList()) {
                if (path.getFileName().toString().matches(expr))
                    findInternal(base.resolve(path), subPath(routes), out);
            }

        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    private String subPath(String[] routes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < routes.length; i++) {
            builder.append(routes[i]).append("/");
        }
        return builder.toString();
    }

    // Differs from cct documentation
    // just returns the capacity of drive holding root
    @LuaWrapped
    public long getCapacity() throws LuaError {
        try {
            return Files.getFileStore(root).getTotalSpace();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    // TODO: Move from map to lua table
    @LuaWrapped
    public @CoerceToNative Map<String, LuaValue> attributes(String p) throws LuaError {
        Path path = sanitize(p);
        Map<String, LuaValue> out = new HashMap<>();
        out.put("isReadOnly", ValueFactory.valueOf(isReadOnly(p)));
        try {
            BasicFileAttributes attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
            out.put("size", ValueFactory.valueOf(attributes.size()));
            out.put("isDir", ValueFactory.valueOf(attributes.isDirectory()));
            out.put("modified", ValueFactory.valueOf(attributes.lastModifiedTime().toMillis()));
            out.put("created", ValueFactory.valueOf(attributes.creationTime().toMillis()));
        } catch (IOException e) {
            throw new LuaError(e);
        }
        return out;
    }


}
