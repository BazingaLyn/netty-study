package com.lyncc.netty.codec.custom;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtocolEncoder extends MessageToByteEncoder<ProtocolMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMsg msg, ByteBuf out) throws Exception {
        if (null == msg || null == msg.getProtocolHeader()) {
             throw new Exception("msg is null");
        }
        
        ProtocolHeader header = msg.getProtocolHeader();
        String body = msg.getBody();
        
        byte[] bodyBytes = body.getBytes(Charset.forName("utf-8"));
        int bodySize = bodyBytes.length;
        out.writeByte(header.getMagic());
        out.writeByte(header.getMsgType());
        out.writeShort(header.getReserve());
        out.writeShort(header.getSn());
        out.writeInt(bodySize);
        out.writeBytes(bodyBytes);
    }

}
