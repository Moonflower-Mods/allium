package me.hugeblank.allium.loader.resources;

import com.mojang.logging.LogUtils;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public interface ScriptResourceLocator {
    Logger LOGGER = LogUtils.getLogger();
    InputStream getFile(String name);
    Set<String> getNamespaces(ResourceType type);
    Collection<Identifier> findResources(ResourceType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter);

    default String relativize(File base, File target) {
        return base.toURI().relativize(target.toURI()).getPath();
    }

    default void warnNonLowerCaseNamespace(String namespace, String id) {
        LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", namespace, id);
    }
}
