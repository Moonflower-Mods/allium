package dev.hugeblank.allium.api;

/**
 * Entrypoint interface for allium extensions.
 * Use this interface to provide additional libraries to scripts
  */
public interface AlliumExtension {
    void onInitialize();
}
