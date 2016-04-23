package com.lyncc.netty.hello;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class HelloWorldServer {

    private int port;
    
    public HelloWorldServer(int port) {
        this.port = port;
    }
    
    public void start(){
        EventLoopGroup group = new NioEventLoopGroup();
        
        ServerBootstrap sbs = new ServerBootstrap().group(group).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<Channel>() {
                    
                    protected void initChannel(Channel ch) throws Exception {
                        
                        
                    };
                    
                });
    }
}
