package dev.hugeblank.allium.loader;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.util.FileHelper;

import java.util.Set;

public class ScriptLoader {
    public static final Set<Script> SCRIPTS;

    static {
        SCRIPTS = new ImmutableSet.Builder<Script>()
                .addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()))
                .addAll(FileHelper.getValidModScripts())
                .build();
    }
}
