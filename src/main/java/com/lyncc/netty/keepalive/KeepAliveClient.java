package com.lyncc.netty.keepalive;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeepAliveClient {

	private String host ;
	private int port ;
	
	private EventLoopGroup group ;
	
	private Bootstrap b ;
	
	private Channel ch ;
	
	// 定义客户端没有收到服务端的pong消息的最大次数
	private static final int MAX_UN_REC_PONG_TIMES = 3;
	
	// 多长时间未请求后，发送心跳
	private static final int WRITE_WAIT_SECONDS = 5;
	
	// 隔N秒后重连
	private static final int RE_CONN_WAIT_SECONDS = 5;
	
	// 客户端连续N次没有收到服务端的pong消息  计数器
	private int unRecPongTimes = 0 ;
	
	private ScheduledExecutorService executorService ;
	
	// 是否停止
	private boolean isStop = false ;

	public KeepAliveClient(String host, int port) {
		this.host = host ;
		this.port = port ;
		group = new NioEventLoopGroup();
		b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(new HeartbeatInitializer());
	}

	public void start() {
		connServer();
	}
	
	private void connServer(){
		
		isStop = false;
		
		if(executorService!=null){
			executorService.shutdown();
		}
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleWithFixedDelay(new Runnable() {
			
			boolean isConnSucc = true;
			
			public void run() {
				try {
					// 重置计数器
					unRecPongTimes = 0;
					// 连接服务端
					if(ch!=null&&ch.isOpen()){
						ch.close();
					}
					ch = b.connect(host, port).sync().channel();
					// 此方法会阻塞
//					ch.closeFuture().sync();
					System.out.println("connect server finish");
				} catch (Exception e) {
					e.printStackTrace();
					isConnSucc = false ;
				} finally{
					if(isConnSucc){
						if(executorService!=null){
							executorService.shutdown();
						}
					}
				}
			}
		}, RE_CONN_WAIT_SECONDS, RE_CONN_WAIT_SECONDS, TimeUnit.SECONDS);
	}
	
	public class HeartbeatInitializer extends ChannelInitializer<SocketChannel> {
		 
	    @Override
	    protected void initChannel(SocketChannel ch) throws Exception {
	        ChannelPipeline pipeline = ch.pipeline();
	 
			pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
	        pipeline.addLast("encoder", new ObjectEncoder());
	 
	        pipeline.addLast("ping", new IdleStateHandler(0, WRITE_WAIT_SECONDS, 0,TimeUnit.SECONDS));
	        // 客户端的逻辑
	        pipeline.addLast("handler", new ClientHandler());
	    }
	}

	public class ClientHandler extends SimpleChannelInboundHandler<KeepAliveMessage> {
		 
	    @Override
	    protected void channelRead0(ChannelHandlerContext ctx, KeepAliveMessage msg)
	            throws Exception {
	    	System.out.println("Server say : sn=" + msg.getSn()+",reqcode="+msg.getReqCode());
	        if (Constants.RET_CODE == msg.getReqCode()) {
	        	// 计数器清零
	        	unRecPongTimes = 0;
	        }
	    }
	    
	    @Override
	    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	        System.out.println("Client active ");
	        super.channelActive(ctx);
	    }

	    @Override
	    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	        System.out.println("Client close ");
	        super.channelInactive(ctx);
	        /*
	         * 重连
	         */
	        if(!isStop){
	        	connServer();
	        }
	    }
	    
	    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	        if (evt instanceof IdleStateEvent) {
	            IdleStateEvent event = (IdleStateEvent) evt;
	            if (event.state() == IdleState.READER_IDLE) {
	                /*读超时*/
	                System.out.println("===服务端===(READER_IDLE 读超时)");
	            } else if (event.state() == IdleState.WRITER_IDLE) {
	                /*写超时*/   
	                System.out.println("===服务端===(WRITER_IDLE 写超时)");
	                if(unRecPongTimes < MAX_UN_REC_PONG_TIMES){
	                	ctx.channel().writeAndFlush(getSrcMsg()) ;
	                	unRecPongTimes++;
	                }else{
	                	ctx.channel().close();
	                }
	            } else if (event.state() == IdleState.ALL_IDLE) {
	                /*总超时*/
	                System.out.println("===服务端===(ALL_IDLE 总超时)");
	            }
	        }
	    }
	}
	
	private KeepAliveMessage getSrcMsg(){
		KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
		// 设备码
        keepAliveMessage.setSn("sn_123456abcdfef");
        keepAliveMessage.setReqCode(Constants.REQ_CODE);
        return keepAliveMessage ;
	}
	
	public void stop(){
		isStop = true;
		if(ch!=null&&ch.isOpen()){
			ch.close();
		}
		if(executorService!=null){
			executorService.shutdown();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		KeepAliveClient keepAliveServer = new KeepAliveClient("127.0.0.1",1666);
		keepAliveServer.start();
	}
}
