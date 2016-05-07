package com.lyncc.netty.heartbeat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

public class BaseServerHandler extends ChannelInboundHandlerAdapter{
    
    
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
            .unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat",
                    CharsetUtil.UTF_8));  

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        System.out.println("hehehehe");
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            String type = "";
            if (event.state() == IdleState.READER_IDLE) {
                type = "read idle";
                System.out.println( ctx.channel().remoteAddress()+"超时类型：" + type);
            } else if (event.state() == IdleState.WRITER_IDLE) {
                type = "write idle";
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(
                        ChannelFutureListener.CLOSE_ON_FAILURE);
            }  
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("server channelRead..");
        System.out.println(ctx.channel().remoteAddress()+"->Server :"+ msg.toString());
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello my name is lyncc", CharsetUtil.UTF_8));
    }
    
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
