package dev.hugeblank.allium.lua.api.http;

import dev.hugeblank.allium.lua.api.WrappedLuaLibrary;
import dev.hugeblank.allium.lua.type.annotation.LuaWrapped;

import java.net.URI;

@LuaWrapped(name = "http")
public class HttpLib implements WrappedLuaLibrary {
    @LuaWrapped
    public HttpRequest request(String url) {
        return new HttpRequest(URI.create(url));
    }
}
