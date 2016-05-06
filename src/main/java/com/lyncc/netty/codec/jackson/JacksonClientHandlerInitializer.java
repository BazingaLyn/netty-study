package com.lyncc.netty.codec.jackson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class JacksonClientHandlerInitializer extends ChannelInitializer<Channel>{

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new UserDecoder<User>(User.class));
        pipeline.addLast(new UserEncoder());
        pipeline.addLast(new JacksonClientHandler());
    }

}
