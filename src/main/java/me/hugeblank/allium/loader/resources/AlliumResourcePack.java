package me.hugeblank.allium.loader.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.util.FileHelper;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourceNotFoundException;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

// Dangerously similar to DirectoryResourcePack
public class AlliumResourcePack extends DirectoryResourcePack {
    private final List<File> BASES = new ArrayList<>();

    public void register(Path pluginDir) {
        BASES.add(FileHelper.getResourcePackRoot(pluginDir));
    }

    public void drop(Path pluginDir) {
        BASES.remove(FileHelper.getResourcePackRoot(pluginDir));
    }

    public AlliumResourcePack() {
        super(FileHelper.getScriptsDirectory()); // Just use the plugins directory as a base, idk.
    }

    @Override
    protected InputStream openFile(String name) throws IOException {
        if (name.equals("pack.png")) {
            return AlliumResourcePack.class.getResourceAsStream("/assets/allium/icon.png");
        }
        File file = this.getFile(name);
        if (file == null) {
            throw new ResourceNotFoundException(this.base, name);
        }
        return new FileInputStream(file);
    }

    @Override
    protected boolean containsFile(String name) {
        return this.getFile(name) != null;
    }

    @Nullable
    private File getFile(String name) {
        try {
            for (File f : BASES) {
                File file = new File(f, name);
                if (file.isFile() && DirectoryResourcePack.isValidPath(file, name)) {
                    return file;
                }
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        HashSet<String> set = Sets.newHashSet();
        BASES.forEach((f) -> {
            File file = new File(f, type.getDirectory());
            File[] files = file.listFiles((FilenameFilter) DirectoryFileFilter.DIRECTORY);
            if (files != null) {
                for (File file2 : files) {
                    String string = DirectoryResourcePack.relativize(file, file2);
                    if (string.equals(string.toLowerCase(Locale.ROOT))) {
                        set.add(string.substring(0, string.length() - 1));
                        continue;
                    }
                    this.warnNonLowerCaseNamespace(string);
                }
            }
        });
        return set;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
        return AbstractFileResourcePack.parseMetadata(metaReader, AlliumResourcePack.class.getResourceAsStream("/assets/pack.mcmeta"));
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        ArrayList<Identifier> list = Lists.newArrayList();
        BASES.forEach((f) -> {
            File file = new File(f, type.getDirectory() + "/" + namespace + "/" + prefix);
            this.findFiles(file, maxDepth, namespace, list, prefix + "/", pathFilter);
        });
        return list;
    }

    private void findFiles(File file, int maxDepth, String namespace, List<Identifier> found, String prefix, Predicate<String> pathFilter) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File file2 : files) {
                if (file2.isDirectory()) {
                    if (maxDepth <= 0) continue;
                    this.findFiles(file2, maxDepth - 1, namespace, found, prefix + file2.getName() + "/", pathFilter);
                    continue;
                }
                if (file2.getName().endsWith(".mcmeta") || !pathFilter.test(file2.getName())) continue;
                try {
                    found.add(new Identifier(namespace, prefix + file2.getName()));
                }
                catch (InvalidIdentifierException invalidIdentifierException) {
                    Allium.LOGGER.error(invalidIdentifierException.getMessage());
                }
            }
        }
    }

    @Override
    public String getName() {
        return Allium.ID + "_generated";
    }
}
