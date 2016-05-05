package com.lyncc.netty.stickpackage.delimiter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;

public class BaseServer {

    private int port;
    
    public BaseServer(int port) {
        this.port = port;
    }
    
    public void start(){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap sbs = new ServerBootstrap().group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        
                        protected void initChannel(SocketChannel ch) throws Exception {
                            
                            ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
                            ChannelPipeline p = ch.pipeline();
                            
                            p.addLast(new DelimiterBasedFrameDecoder(1024,delimiter));
                            p.addLast(new StringDecoder());
                            
                            p.addLast(new BaseServerHandler());
                        };
                        
                    }).option(ChannelOption.SO_BACKLOG, 128)   
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
             // 绑定端口，开始接收进来的连接
             ChannelFuture future = sbs.bind(port).sync();  
             
             System.out.println("Server start listen at " + port );
             future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new BaseServer(port).start();
    }
}
