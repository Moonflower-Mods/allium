package dev.hugeblank.allium.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Map;

// This class was directly inspired by Fabric-ASM. Thank you Chocohead for paving this path for me to walk down with my goofy Lua mod.
// https://github.com/Chocohead/Fabric-ASM/blob/master/src/com/chocohead/mm/CasualStreamHandler.java
public class EldritchURLStreamHandler extends URLStreamHandler {
    private final Map<String, byte[]> providers;

    public EldritchURLStreamHandler(Map<String, byte[]> providers) {
        this.providers = providers;
    }

    public static URL create(String name, byte[] provider) {
        try {
            return new URL("allium-mixin", null, -1, "/", new EldritchURLStreamHandler(Collections.singletonMap(name, provider)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL create(Map<String, byte[]> providers) {
        try {
            return new URL("allium-mixin", null, -1, "/", new EldritchURLStreamHandler(providers));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected URLConnection openConnection(URL url) {
        return providers.containsKey(url.getPath()) ? new EldritchConnection(url, providers.get(url.getPath())) : null;
    }

    // Someone please name a game "Eldritch Connection"
    private static final class EldritchConnection extends URLConnection {
        private final byte[] bytes;

        public EldritchConnection(URL url, byte[] bytes) {
            super(url);
            this.bytes = bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void connect() {
            throw new UnsupportedOperationException();
        }
    }
}
