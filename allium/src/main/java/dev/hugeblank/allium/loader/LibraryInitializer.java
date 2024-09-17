package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.loader.type.WrappedLuaLibrary;

public interface LibraryInitializer {
    WrappedLuaLibrary init(Script script);
}
