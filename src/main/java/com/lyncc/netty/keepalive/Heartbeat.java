package com.lyncc.netty.keepalive;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class Heartbeat extends SimpleChannelInboundHandler<KeepAliveMessage>{
    
    //失败计数器：未收到client端发送的ping请求
    private int unRecPingTimes = 0 ;
    
    //每个chanel对应一个线程，此处用来存储对应于每个线程的一些基础数据，此处不一定要为KeepAliveMessage对象
    ThreadLocal<KeepAliveMessage> localMsgInfo = new ThreadLocal<KeepAliveMessage>(); 
    
    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PING_TIMES = 3;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, KeepAliveMessage msg) throws Exception {
        
        System.out.println(ctx.channel().remoteAddress() + " Say : sn=" + msg.getSn()+",reqcode="+msg.getReqCode());
        
        if(Utils.notEmpty(msg.getSn())&&msg.getReqCode()==1){
            msg.setReqCode(Constants.RET_CODE);
            ctx.channel().writeAndFlush(msg);
            // 失败计数器清零
            unRecPingTimes = 0;
            if(localMsgInfo.get()==null){
                KeepAliveMessage localMsg = new KeepAliveMessage();
                localMsg.setSn(msg.getSn());
                localMsgInfo.set(localMsg);
            }
        }else{
            ctx.channel().close();
        }
        
    }
    
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                /*读超时*/
                System.out.println("===服务端===(READER_IDLE 读超时)");
                // 失败计数器次数大于等于3次的时候，关闭链接，等待client重连
                if(unRecPingTimes >= MAX_UN_REC_PING_TIMES){
                    System.out.println("===服务端===(读超时，关闭chanel)");
                    // 连续超过N次未收到client的ping消息，那么关闭该通道，等待client重连
                    ctx.channel().close();
                }else{
                    // 失败计数器加1
                    unRecPingTimes++;
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                /*写超时*/   
                System.out.println("===服务端===(WRITER_IDLE 写超时)");
            } else if (event.state() == IdleState.ALL_IDLE) {
                /*总超时*/
                System.out.println("===服务端===(ALL_IDLE 总超时)");
            }
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("错误原因："+cause.getMessage());
        if(localMsgInfo.get()!=null){
            /*
             * 从管理集合中移除设备号等唯一标示，标示设备离线
             */
            // TODO
        }
        ctx.channel().close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client active ");
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 关闭，等待重连
        ctx.close();
        if(localMsgInfo.get()!=null){
            /*
             * 从管理集合中移除设备号等唯一标示，标示设备离线
             */
            // TODO
        }
        System.out.println("===服务端===(客户端失效)");
    }

}
