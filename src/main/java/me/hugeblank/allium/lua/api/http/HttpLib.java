package me.hugeblank.allium.lua.api.http;

import me.hugeblank.allium.lua.api.WrappedLuaLibrary;
import me.hugeblank.allium.lua.type.LuaWrapped;

import java.net.URI;

@LuaWrapped(name = "http")
public class HttpLib implements WrappedLuaLibrary {
    @LuaWrapped
    public HttpRequest request(String url) {
        return new HttpRequest(URI.create(url));
    }
}
