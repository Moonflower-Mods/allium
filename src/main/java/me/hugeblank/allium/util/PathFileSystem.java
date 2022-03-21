package me.hugeblank.allium.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

public class PathFileSystem extends FileSystem {
    private final FileSystem sys;
    private final Path path;

    public PathFileSystem(FileSystem sys, Path path) {
        this.sys = sys;
        this.path = path;
    }

    @Override
    public FileSystemProvider provider() {
        return sys.provider();
    }

    @Override
    public void close() throws IOException {
        sys.close();
    }

    @Override
    public boolean isOpen() {
        return sys.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return sys.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return sys.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        Set<Path> roots = new HashSet<>();
        sys.getRootDirectories().forEach((path) -> roots.add(path.resolve(this.path)));
        return roots;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return sys.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return sys.supportedFileAttributeViews();
    }

    @NotNull
    @Override
    public Path getPath(@NotNull String first, @NotNull String... more) {
        Path path = this.path.resolve(first);
        for (String s : more) {
            path = path.resolve(s);
        }
        return sys.getPath(path.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return sys.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return sys.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return sys.newWatchService();
    }
}
