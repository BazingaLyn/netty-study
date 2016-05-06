package com.lyncc.netty.codec.jackson;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;


public class JacksonClient {

    private final String host;
    private final int port;

    public JacksonClient(String host, int port){
        this.host = host;
        this.port = port;
    }
    
    public static void main(String[] args) throws Exception{
        new JacksonClient("localhost", 8082).run();
    }
    
    public void run() throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap  = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new JacksonClientHandlerInitializer());
            
            Channel channel = bootstrap.connect(host, port).sync().channel();

            // 发送对象
            User user = new User();
            user.setId(1);
            user.setAge(21);
            user.setName("BazingaLyncc");
            
            List<String> friends = new ArrayList<String>();
            friends.add("TED");
            friends.add("MISS");
            user.setFriends(friends);
            
            channel.write(user);
            channel.flush();
 
            // 等待连接关闭
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }
}
