package dev.hugeblank.bouquet.api.lib.http;


import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;

import java.net.URI;

@LuaWrapped(name = "http")
public class HttpLib {
    @LuaWrapped
    public HttpRequest request(String url) {
        return new HttpRequest(URI.create(url));
    }
}
