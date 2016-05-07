package com.lyncc.netty.heartbeat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

public class NettyServerHandler extends SimpleChannelInboundHandler<BaseMsg> {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannelMap.remove((SocketChannel) ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseMsg msg) throws Exception {
        if (MsgType.LOGIN.equals(msg.getType())) {
            LoginMsg loginMsg = (LoginMsg) msg;
            if ("robin".equals(loginMsg.getUserName()) && "yao".equals(loginMsg.getPassword())) {
                NettyChannelMap.add(loginMsg.getClientId(), (SocketChannel) ctx.channel());
                System.out.println("client" + loginMsg.getClientId() + " 登录成功");
            }
        } else {
            if (NettyChannelMap.get(msg.getClientId()) == null) {
                // 说明未登录，或者连接断了，服务器向客户端发起登录请求，让客户端重新登录
                LoginMsg loginMsg = new LoginMsg();
                ctx.channel().writeAndFlush(loginMsg);
            }
        }
        switch (msg.getType()){
        case PING:{
            PingMsg pingMsg=(PingMsg)msg;
            PingMsg replyPing=new PingMsg();
            NettyChannelMap.get(pingMsg.getClientId()).writeAndFlush(replyPing);
        }break;
        case ASK:{
            //收到客户端的请求
            AskMsg askMsg=(AskMsg)msg;
            if("authToken".equals(askMsg.getParams().getAuth())){
                ReplyServerBody replyBody=new ReplyServerBody("server info");
                ReplyMsg replyMsg=new ReplyMsg();
                replyMsg.setBody(replyBody);
                NettyChannelMap.get(askMsg.getClientId()).writeAndFlush(replyMsg);
            }
        }break;
        case REPLY:{
            //收到客户端回复
            ReplyMsg replyMsg=(ReplyMsg)msg;
            ReplyClientBody clientBody=(ReplyClientBody)replyMsg.getBody();
            System.out.println("receive client msg: "+clientBody.getClientInfo());
        }break;
        default:break;
    }
        ReferenceCountUtil.release(msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("server SimpleChatClient:"+incoming.remoteAddress()+"异常");
        cause.printStackTrace();
        ctx.close();
    }

}
