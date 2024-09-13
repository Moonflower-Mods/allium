package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.ScriptExecutor;

import java.util.function.Consumer;

@SuppressWarnings("ALL")
public interface AlliumExtension {
    void onInitialize(Consumer<ScriptExecutor.LibraryInitializer> register);
}
