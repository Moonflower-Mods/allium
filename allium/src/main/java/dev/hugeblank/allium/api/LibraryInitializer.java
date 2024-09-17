package dev.hugeblank.allium.api;

import dev.hugeblank.allium.loader.Script;

/**
 * Functional interface for libraries that depend on the script to function.
 * Passing the constructor of a class that extends <code>WrappedLuaLibrary</code> is the ideal use case.
 *
 * @see dev.hugeblank.allium.loader.EnvironmentManager#registerLibrary(LibraryInitializer)
  */
@FunctionalInterface
public interface LibraryInitializer {
    WrappedLuaLibrary init(Script script);
}
