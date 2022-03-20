package me.hugeblank.allium.loader.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.loader.ZipFileScript;
import me.hugeblank.allium.util.FileHelper;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceNotFoundException;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;

// Dangerously similar to DirectoryResourcePack
public class AlliumResourcePack extends AbstractFileResourcePack {
    private final Map<Script, ScriptResourceLocator> BASES = new HashMap<>();

    public void register(Script script) {
        if (script instanceof ZipFileScript) {
            BASES.put(script, new ZipFileScriptResourceLocator((ZipFileScript) script));
        } else {
            BASES.put(script, new DirectScriptResourceLocator(script));
        }
    }

    public void drop(Script script) {
        BASES.remove(script);
    }

    public AlliumResourcePack() {
        super(FileHelper.getScriptsDirectory()); // Just use the plugins directory as a base, idk.
    }

    @Override
    protected InputStream openFile(String name) throws IOException {
        if (name.equals("pack.png")) {
            return AlliumResourcePack.class.getResourceAsStream("/assets/allium/icon.png");
        }
        InputStream stream = this.getFile(name);
        if (stream == null) {
            throw new ResourceNotFoundException(this.base, name);
        }
        return stream;
    }

    @Override
    protected boolean containsFile(String name) {
        return this.getFile(name) != null;
    }

    @Nullable
    private InputStream getFile(String name) {
        for (ScriptResourceLocator srl : BASES.values()) {
            InputStream stream = srl.getFile(name);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        HashSet<String> set = Sets.newHashSet();
        BASES.values().forEach((srl) -> set.addAll(srl.getNamespaces(type)));
        return set;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
        return AbstractFileResourcePack.parseMetadata(metaReader, AlliumResourcePack.class.getResourceAsStream("/assets/pack.mcmeta"));
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        ArrayList<Identifier> list = Lists.newArrayList();
        BASES.forEach((s, srl) -> list.addAll(srl.findResources(type, namespace, prefix, maxDepth, pathFilter)));
        return list;
    }

    @Override
    public String getName() {
        return Allium.ID + "_generated";
    }

    @Override
    public void close() {}
}
