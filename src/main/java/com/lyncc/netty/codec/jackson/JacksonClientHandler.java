package com.lyncc.netty.codec.jackson;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class JacksonClientHandler extends SimpleChannelInboundHandler<Object>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String jsonString = "";
        if (msg instanceof User) {
            User user = (User) msg;
            
            jsonString = UserMapper.getInstance().writeValueAsString(user);  
        } else {
            jsonString = UserMapper.getInstance().writeValueAsString(msg);  
        }
        System.out.println("Client get msg form Server -" + jsonString);
    }

}
