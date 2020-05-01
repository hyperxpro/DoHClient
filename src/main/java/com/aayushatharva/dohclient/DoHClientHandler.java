package com.aayushatharva.dohclient;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.xbill.DNS.Message;

import java.io.IOException;


class DoHClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse fullHttpRequest) throws IOException {
        byte[] pck = new byte[fullHttpRequest.content().readableBytes()];
        fullHttpRequest.content().readBytes(pck);

        System.out.println("--------------------------RESPONSE--------------------------");
        System.out.println(new Message(pck));
        System.out.println("------------------------------------------------------------");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
