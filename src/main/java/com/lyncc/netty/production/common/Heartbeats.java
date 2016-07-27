package com.lyncc.netty.production.common;

import static com.lyncc.netty.production.common.NettyCommonProtocol.HEAD_LENGTH;
import static com.lyncc.netty.production.common.NettyCommonProtocol.HEARTBEAT;
import static com.lyncc.netty.production.common.NettyCommonProtocol.MAGIC;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author BazingaLyn
 * @description 心跳包
 * @time
 * @modifytime
 */
public class Heartbeats {

    private static final ByteBuf HEARTBEAT_BUF;

    static {
        ByteBuf buf = Unpooled.buffer(HEAD_LENGTH);
        buf.writeShort(MAGIC);
        buf.writeByte(HEARTBEAT);
        buf.writeByte(0);
        buf.writeLong(0);
        buf.writeInt(0);
        HEARTBEAT_BUF = Unpooled.unmodifiableBuffer(Unpooled.unreleasableBuffer(buf));
    }

    public static ByteBuf heartbeatContent() {
        return HEARTBEAT_BUF.duplicate();
    }
}
