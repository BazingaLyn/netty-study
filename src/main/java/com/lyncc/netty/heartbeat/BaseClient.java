package com.lyncc.netty.heartbeat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class BaseClient {

    public void connect(int port, String host) throws Exception {
        ChannelFuture future = null;
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("decoder", new StringDecoder());
                        p.addLast("encoder", new StringEncoder());
                        p.addLast("ping", new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));
                        p.addLast(new BaseClientHandler());
                    }
                });

        try {
            future = b.connect(host, port).sync();
            future.channel().writeAndFlush("Hello Netty Server ,I am a common client");
            future.channel().closeFuture().sync();
        } finally {
//            group.shutdownGracefully();
            if (null != future) {
                if (future.channel() != null && future.channel().isOpen()) {
                    future.channel().close();
                }
            }
            System.out.println("准备重连");
            connect(port, host);
            System.out.println("重连成功");
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        new BaseClient().connect(port, "127.0.0.1");
    }

}
