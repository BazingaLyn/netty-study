package com.lyncc.netty.hello;

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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class HelloWorldClient {
    
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        initChannel();
    }
    
    public static void initChannel() throws InterruptedException{
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast("decoder", new StringDecoder());
                     p.addLast("encoder", new StringEncoder());
                     p.addLast(new HelloWorldClientHandler());
                 }
             });
//            SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
            SocketAddress socketAddress =  new InetSocketAddress(HOST, PORT);
            ChannelFuture future = b.connect(socketAddress).sync();
            future.channel().writeAndFlush("hello world");
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    
    }

}
