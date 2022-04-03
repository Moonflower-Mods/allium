package me.hugeblank.allium.lua.api.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.lua.type.annotation.LuaWrapped;
import net.minecraft.server.ServerNetworkIo;
import org.squiddev.cobalt.LuaError;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

@LuaWrapped
public class HttpRequest extends ChannelInitializer<SocketChannel> {
    private static final SslContext SSL_CONTEXT;

    static {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);

            SSL_CONTEXT = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .trustManager(factory)
                .build();
        } catch (SSLException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private final URI uri;
    private final DefaultFullHttpRequest rawRequest;
    @LuaWrapped public final LuaByteBuf body;
    private int maxFollowedRedirects = 3;
    private int followedRedirects = 0;

    private CompletableFuture<HttpResponse> sendFuture;

    public HttpRequest(URI uri) {
        this.uri = uri;
        this.body = new LuaByteBuf(Unpooled.buffer(), StandardCharsets.UTF_8);
        this.rawRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath(), this.body.getRaw());

        this.rawRequest.headers().set(HttpHeaderNames.HOST, uri.getHost());
        this.rawRequest.headers().set(HttpHeaderNames.USER_AGENT, "Allium/" + Allium.VERSION);
        this.rawRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        this.rawRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP_DEFLATE);
    }

    @LuaWrapped
    public HttpRequest method(String method) {
        this.rawRequest.setMethod(HttpMethod.valueOf(method));
        return this;
    }

    @LuaWrapped
    public HttpRequest header(String key, String value) {
        this.rawRequest.headers().set(key, value);
        return this;
    }

    @LuaWrapped
    public HttpRequest contentType(String mimeType) {
        return header(HttpHeaderNames.CONTENT_TYPE.toString(), mimeType + "; charset=" + body.getCharset());
    }

    @LuaWrapped
    public HttpRequest maxFollowedRedirects(int maxFollowedRedirects) {
        this.maxFollowedRedirects = maxFollowedRedirects;
        return this;
    }

    @LuaWrapped
    public CompletableFuture<HttpResponse> send() {
        if (sendFuture != null) return sendFuture;

        sendFuture = new CompletableFuture<>();

        doRequestTo(uri);

        return sendFuture;
    }

    private void doRequestTo(URI newUri) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(ServerNetworkIo.DEFAULT_CHANNEL.get())
            .channel(NioSocketChannel.class)
            .handler(this);

        var port = newUri.getPort();

        if (port < 0) port = newUri.getScheme().equals("https") ? 443 : 80;

        var chanFuture = bootstrap.connect(newUri.getHost(), port);
        chanFuture.addListener(unused -> {
            if (!chanFuture.isSuccess()) {
                sendFuture.completeExceptionally(new LuaError(chanFuture.cause()));
                return;
            }

            var chan = chanFuture.channel();

            if (body.readableBytes() > 0) {
                this.rawRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
            }

            rawRequest.retain();

            chan.writeAndFlush(rawRequest);
        });
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        var p = ch.pipeline();

        if (uri.getScheme().equals("https"))
            p.addLast(SSL_CONTEXT.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));

        p.addLast(new HttpClientCodec());
        p.addLast(new HttpContentDecompressor());
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new Handler());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sendFuture.completeExceptionally(cause);
        super.exceptionCaught(ctx, cause);
    }

    private class Handler extends SimpleChannelInboundHandler<HttpObject> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            if (msg instanceof FullHttpResponse response) {
                if (response.status().codeClass() == HttpStatusClass.REDIRECTION) {
                    followedRedirects++;
                    if (maxFollowedRedirects >= followedRedirects) {
                        URI location = URI.create(response.headers().get(HttpHeaderNames.LOCATION));

                        rawRequest.headers().set(HttpHeaderNames.HOST, location.getHost());
                        rawRequest.setUri(location.getRawPath());

                        if (response.status() == HttpResponseStatus.SEE_OTHER)
                            rawRequest.setMethod(HttpMethod.GET);

                        doRequestTo(location);

                        return;
                    }
                }

                rawRequest.release();
                sendFuture.complete(new HttpResponse(response));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            sendFuture.completeExceptionally(cause);
        }
    }
}
