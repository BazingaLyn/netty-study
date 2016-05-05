package com.lyncc.netty.stickpackage.delimiter;

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
    
    private int counter;
    
    static final String FIXED_REQ = "Hi,Lyncc,Learn English.$_";
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for (int i = 0; i < 100; i++) {
            ctx.writeAndFlush(Unpooled.copiedBuffer(FIXED_REQ.getBytes()));
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        
        System.out.println("Now is : " + msg + " ; the counter is : "+ ++counter);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
    
    

}
