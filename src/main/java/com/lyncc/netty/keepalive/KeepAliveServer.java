package com.lyncc.netty.keepalive;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class KeepAliveServer {
    
    private int port ;

    public KeepAliveServer(int port) {
        this.port = port;
    }
    
    ChannelFuture f ;
    
    ServerBootstrap b ;
    
    public void startServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new KeepAliveServerInitializer());
            // 服务器绑定端口监听
            f = b.bind(port).sync();
            // 监听服务器关闭监听，此方法会阻塞
            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        
    }

}


