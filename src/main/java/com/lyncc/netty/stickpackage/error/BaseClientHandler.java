package com.lyncc.netty.stickpackage.error;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 
 * @author bazingaLyncc
 * 描述：客户端的第一个自定义的inbound处理器
 * 时间  2016年5月3日
 */
public class BaseClientHandler extends ChannelInboundHandlerAdapter{
    
    private byte[] req;
    
    private int counter;
    
    public BaseClientHandler() {
        req = ("QUERY TIME ORDER" + System.getProperty("line.separator"))
            .getBytes();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message = null;
        for (int i = 0; i < 100; i++) {
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
        throws Exception {
        String buf = (String) msg;
        System.out.println("Now is : " + buf + " ; the counter is : "+ ++counter);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
    
    

}
