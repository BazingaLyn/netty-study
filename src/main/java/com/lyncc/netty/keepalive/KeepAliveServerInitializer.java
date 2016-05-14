package com.lyncc.netty.keepalive;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class KeepAliveServerInitializer extends ChannelInitializer<SocketChannel>{
    
    // 设置6秒检测chanel是否接受过心跳数据
    private static final int READ_WAIT_SECONDS = 6;
    
    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PING_TIMES = 3;
    

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
        pipeline.addLast("encoder", new ObjectEncoder());
        pipeline.addLast("pong", new IdleStateHandler(READ_WAIT_SECONDS, 0, 0,TimeUnit.SECONDS));
        
        pipeline.addLast("handler", new Heartbeat());
    }

}
