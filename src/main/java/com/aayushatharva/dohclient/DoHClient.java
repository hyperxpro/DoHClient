package com.aayushatharva.dohclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.*;
import org.xbill.DNS.*;

import java.util.Arrays;

public class DoHClient {

    public static void main(String[] args) throws Exception {

        OpenSsl.ensureAvailability();

        SslContext sslCtx = SslContextBuilder.forClient()
                .protocols("TLSv1.3", "TLSv1.2")
                .ciphers(Arrays.asList(
                        "TLS_AES_256_GCM_SHA384",
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_CHACHA20_POLY1305_SHA256",
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
                .sslProvider(SslProvider.OPENSSL)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();

        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channelFactory(() -> {
                    if (Epoll.isAvailable()) {
                        return new EpollSocketChannel();
                    } else {
                        return new NioSocketChannel();
                    }
                })
                .handler(new DoHClientInitializer(sslCtx));

        Channel channel = bootstrap.connect("dns.google", 443).sync().channel();

        Thread.sleep(1000);

        Message message = Message.newQuery(Record.newRecord(Name.fromString("www.google.com."), Type.A, DClass.IN));

        System.out.println("--------------------------REQUEST--------------------------");
        System.out.println(message);
        System.out.println("-----------------------------------------------------------");

        HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "https://dns.google/dns-query",
                Unpooled.wrappedBuffer(message.toWire()));
        httpRequest.headers()
                .add(HttpHeaderNames.HOST, "dns.google")
                .add(HttpHeaderNames.CONTENT_TYPE, "application/dns-message")
                .add(HttpHeaderNames.ACCEPT, "application/dns-message");


        channel.writeAndFlush(httpRequest).sync();
    }
}
