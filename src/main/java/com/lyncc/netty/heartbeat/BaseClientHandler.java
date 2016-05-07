package com.lyncc.netty.heartbeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 
 * @author bazingaLyncc
 * 描述：客户端的第一个自定义的inbound处理器
 * 时间  2016年5月3日
 */
public class BaseClientHandler extends ChannelInboundHandlerAdapter{
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("BaseClient1Handler channelActive");
        ctx.fireChannelActive();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("BaseClient1Handler channelInactive");
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String)msg;
        System.out.println(message);
        if(message.equals("Heartbeat")){
            ctx.write("has read message from server");
            ctx.flush();
        }
        ReferenceCountUtil.release(msg);
    }
    
    

}
