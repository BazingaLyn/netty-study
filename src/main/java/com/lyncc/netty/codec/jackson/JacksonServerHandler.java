package com.lyncc.netty.codec.jackson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class JacksonServerHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String jsonString = "";
        if (msg instanceof User) {
            
            User user = (User)msg;
            ctx.writeAndFlush(user);
            
            jsonString = UserMapper.getInstance().writeValueAsString(user); 
        } else {
            ctx.writeAndFlush(msg);
            jsonString = UserMapper.getInstance().writeValueAsString(msg);
        }
        System.out.println("Server get msg form Client -" + jsonString);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { 
        Channel incoming = ctx.channel();
        System.out.println("SimpleChatClient:"+incoming.remoteAddress()+"异常");
        cause.printStackTrace();
        ctx.close();
    }

}
