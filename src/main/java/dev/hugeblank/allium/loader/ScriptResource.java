package dev.hugeblank.allium.loader;

public interface ScriptResource extends AutoCloseable {
    @Override
    void close() throws Exception;
}
