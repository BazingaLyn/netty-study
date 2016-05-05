package com.lyncc.netty.component.channelhandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 
 * @author bazingaLyncc
 * 描述：客户端的第二个自定义的inbound处理器
 * 时间  2016年5月3日
 */
public class BaseClient2Handler extends ChannelInboundHandlerAdapter{
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("BaseClient2Handler Active");
    }
   

}
