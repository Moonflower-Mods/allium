package me.hugeblank.allium.lua.api.http;

import com.google.common.collect.Streams;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import me.hugeblank.allium.lua.type.annotation.CoerceToNative;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@LuaWrapped
public class HttpResponse {
    private final FullHttpResponse raw;
    @LuaWrapped public final LuaByteBuf body;

    public HttpResponse(FullHttpResponse raw) {
        this.raw = raw;
        raw.retain();

        this.body = new LuaByteBuf(raw.content(), HttpUtil.getCharset(raw, StandardCharsets.UTF_8));
    }

    @LuaWrapped
    public int status() {
        return raw.status().code();
    }

    @LuaWrapped
    public @CoerceToNative Map<String, String> headers() {
        return Streams.stream(raw.headers()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
