package com.lyncc.netty.component.simplehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class BaseClientHandler extends SimpleChannelInboundHandler<String>{
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("Client channelRead0 received:" + msg);
    }
    
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println("Client channelRead received:" + msg);
//        
//    }
  
     @Override
     public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
         cause.printStackTrace();
         ctx.close();
     }
   

}
