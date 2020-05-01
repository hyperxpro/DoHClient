package com.aayushatharva.dohclient;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;

class DoHClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public DoHClientInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(sslCtx.newHandler(socketChannel.alloc()));
        socketChannel.pipeline().addLast(new ALPNHandler());
    }

    /**
     * <p> Application Protocol Negotiation Handler </p>
     */
    private static class ALPNHandler extends ApplicationProtocolNegotiationHandler {

        ALPNHandler() {
            super(ApplicationProtocolNames.HTTP_1_1);
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
            if (protocol.equalsIgnoreCase(ApplicationProtocolNames.HTTP_2)) {
                Http2Connection connection = new DefaultHttp2Connection(false);

                InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection)
                        .propagateSettings(false)
                        .validateHttpHeaders(true)
                        .maxContentLength(1024 * 5)
                        .build();

                HttpToHttp2ConnectionHandler http2Handler = new HttpToHttp2ConnectionHandlerBuilder()
                        .frameListener(new DelegatingDecompressorFrameListener(connection, listener))
                        .connection(connection)
                        .build();

                ctx.pipeline().addLast(http2Handler,
                        new HttpObjectAggregator(1024 * 5, true),
                        new DoHClientHandler());
            } else if (protocol.equalsIgnoreCase(ApplicationProtocolNames.HTTP_1_1)) {
                ctx.pipeline().addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(1024 * 5, true),
                        new DoHClientHandler());
            } else {
                throw new IllegalArgumentException("Unsupported Protocol: " + protocol);
            }
        }

        @Override
        protected void handshakeFailure(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
        }
    }
}
