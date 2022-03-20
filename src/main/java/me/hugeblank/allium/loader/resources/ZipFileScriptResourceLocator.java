package me.hugeblank.allium.loader.resources;

import com.google.common.collect.Lists;
import me.hugeblank.allium.loader.ZipFileScript;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileScriptResourceLocator implements ScriptResourceLocator {
    ZipFileScript script;

    public ZipFileScriptResourceLocator(ZipFileScript script) {
        this.script = script;
    }

    @Override
    public InputStream getFile(String name) {
        String path = name.replace(script.getRootPath().toString(), "");
        ZipFile zip = script.getZip();
        try {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) return null;
            if (!entry.isDirectory()) return zip.getInputStream(entry);
        } catch (IOException e) {
            // empty catch block
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> set = new HashSet<>();
        ZipFile zip = script.getZip();
        // This is ugly and probably not the smartest.
        zip.stream().forEach((entry) -> {
            String name = entry.getName();
            if (name.startsWith(type.getDirectory())) {
                String subPath = name.replace(type.getDirectory() + "/", "");
                String[] strs = subPath.split("/");
                if (strs.length > 0) {
                    set.add(strs[0]);
                }
            }
        });
        return set;
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        ArrayList<Identifier> list = Lists.newArrayList();
        ZipEntry entry = script.getZip().getEntry(type.getDirectory() + "/" + namespace + "/" + prefix);
        if (entry != null) {
            ZipEntryFile file = new ZipEntryFile(script.getZip(), entry);
            findFiles(file, maxDepth, namespace, list, prefix + "/", pathFilter);
        }
        return list;
    }

    private void findFiles(ZipEntryFile file, int maxDepth, String namespace, List<Identifier> found, String prefix, Predicate<String> pathFilter) {
        ZipEntryFile[] files = file.listFiles();
            for (ZipEntryFile file2 : files) {
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
                    LOGGER.error(invalidIdentifierException.getMessage());
                }
            }
    }

    private static class ZipEntryFile {
        // Cursed, in a cute way.
        private final ZipFile zip;
        private final ZipEntry entry;
        private final String[] path;

        ZipEntryFile(ZipFile zip, ZipEntry entry) {
            this.zip = zip;
            this.entry = entry;
            this.path = entry.getName().split("/");
        }

        public ZipEntryFile[] listFiles() {
            List<ZipEntryFile> files = new ArrayList<>();
            zip.stream().forEach((entry) -> {
                String[] path2 = entry.getName().split("/");
                if (path.length+1 == path2.length) {
                    boolean valid = true;
                    for (int i = 0; i < path.length; i++) {
                        if (!path[i].equals(path2[i])) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) files.add(new ZipEntryFile(zip, entry));
                }
            });
            return files.toArray(new ZipEntryFile[0]);
        }

        public boolean isDirectory() {
            return entry.isDirectory();
        }

        public String getName() {
            return path[path.length-1];
        }
    }
}
