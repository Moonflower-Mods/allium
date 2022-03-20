package me.hugeblank.allium.loader.resources;

import com.google.common.collect.Lists;
import me.hugeblank.allium.loader.Script;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

public class DirectScriptResourceLocator implements ScriptResourceLocator {
    private final Script script;
    DirectScriptResourceLocator(Script script) {
        this.script = script;
    }

    @Override
    public InputStream getFile(String name) {
        try {
                File file = new File(script.getRootPath().toFile(), name);
                if (file.isFile() && DirectoryResourcePack.isValidPath(file, name)) {
                    return new FileInputStream(file);
                }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> set = new HashSet<>();
        File file = new File(script.getRootPath().toFile(), type.getDirectory());
        File[] files = file.listFiles((FilenameFilter) DirectoryFileFilter.DIRECTORY);
        if (files != null) {
            for (File file2 : files) {
                String string = relativize(file, file2);
                if (string.equals(string.toLowerCase(Locale.ROOT))) {
                    set.add(string.substring(0, string.length() - 1));
                    continue;
                }
                this.warnNonLowerCaseNamespace(string, script.getManifest().id());
            }
        }
        return set;
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        ArrayList<Identifier> list = Lists.newArrayList();
        File file = new File(script.getRootPath().toFile(), type.getDirectory() + "/" + namespace + "/" + prefix);
        findFiles(file, maxDepth, namespace, list, prefix + "/", pathFilter);
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
                    LOGGER.error(invalidIdentifierException.getMessage());
                }
            }
        }
    }
}
