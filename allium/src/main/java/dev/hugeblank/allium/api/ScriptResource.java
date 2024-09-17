package dev.hugeblank.allium.api;

/**
 * Temporary resource to be provided to a dynamic script.
 * On script reload, the <code>close()</code> method gets invoked.
 */
public interface ScriptResource extends AutoCloseable {
    @Override
    void close() throws Exception;
}
