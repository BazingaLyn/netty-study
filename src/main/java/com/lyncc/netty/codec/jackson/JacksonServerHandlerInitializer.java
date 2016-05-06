package com.lyncc.netty.codec.jackson;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class JacksonServerHandlerInitializer extends ChannelInitializer<SocketChannel>{

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new UserDecoder<User>(User.class));
        pipeline.addLast(new UserEncoder());
        pipeline.addLast(new JacksonServerHandler());
    }

}
