package com.lyncc.netty.production.srv.acceptor;

import static com.lyncc.netty.production.common.NettyCommonProtocol.HEARTBEAT;
import static com.lyncc.netty.production.common.NettyCommonProtocol.MAGIC;
import static com.lyncc.netty.production.common.NettyCommonProtocol.REQUEST;
import static com.lyncc.netty.production.common.NettyCommonProtocol.SERVICE_1;
import static com.lyncc.netty.production.common.NettyCommonProtocol.SERVICE_2;
import static com.lyncc.netty.production.common.NettyCommonProtocol.SERVICE_3;
import static com.lyncc.netty.production.common.NettyCommonProtocol.SERVICE_4;
import static com.lyncc.netty.production.serializer.SerializerHolder.serializerImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Signal;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lyncc.netty.production.common.Acknowledge;
import com.lyncc.netty.production.common.Message;
import com.lyncc.netty.production.common.NativeSupport;
import com.lyncc.netty.production.common.NettyCommonProtocol;
import com.lyncc.netty.production.common.NettyEvent;
import com.lyncc.netty.production.common.NettyEventType;

/**
 * 
 * @author BazingaLyn
 * @description 基本的常用的netty Server配置
 * @time 2016年7月20日20:49:53
 * @modifytime
 */
public class DefaultCommonSrvAcceptor extends DefaultSrvAcceptor {
	

	private static final Logger logger = LoggerFactory.getLogger(DefaultCommonSrvAcceptor.class);
	
	//acceptor的trigger
	private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
	
	//message的编码器
	private final MessageEncoder encoder = new MessageEncoder();
	
	//Ack的编码器
	private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();
	
	//SimpleChannelInboundHandler类型的handler只处理@{link Message}类型的数据
	private final MessageHandler handler = new MessageHandler();
	
	private final ChannelEventListener channelEventListener;

	public DefaultCommonSrvAcceptor(int port,ChannelEventListener channelEventListener) {
		super(new InetSocketAddress(port));
		this.init();
		this.channelEventListener = channelEventListener;
	}
	
	@Override
	protected void init() {
		super.init();
		
		/**
		 * backlog参数的含义:
		 * 一个未完成连接的队列，此队列维护着那些已收到了客户端SYN分节信息，等待完成三路握手的连接，socket的状态是SYN_RCVD
		 * .一个已完成的连接的队列，此队列包含了那些已经完成三路握手的连接，socket的状态是ESTABLISHED
		 * backlog参数历史上被定义为上面两个队列的大小之和
		 * 当客户端的第一个SYN到达的时候，TCP会在未完成队列中增加一个新的记录然后回复给客户端三路握手中的第二个分节(服务端的SYN和针对客户端的ACK)
		 * ，这条记录会在未完成队列中一直存在，直到三路握手中的最后一个分节到达，或者直到超时(Berkeley时间将这个超时定义为75秒)
		 * 如果当客户端SYN到达的时候队列已满，TCP将会忽略后续到达的SYN，但是不会给客户端发送RST信息，因为此时允许客户端重传SYN分节，如果返回错误
		 * 信息，那么客户端将无法分清到底是服务端对应端口上没有相应应用程序还是服务端对应端口上队列已满这两种情况
		 */
		bootstrap().option(ChannelOption.SO_BACKLOG, 32768)
		/**
	     * [TCP/IP协议详解]中描述:
	     * 当TCP执行一个主动关闭, 并发回最后一个ACK ,该连接必须在TIME_WAIT状态停留的时间为2倍的MSL.
	     * 这样可让TCP再次发送最后的ACK以防这个ACK丢失(另一端超时并重发最后的FIN).
	     * 这种2MSL等待的另一个结果是这个TCP连接在2MSL等待期间, 定义这个连接的插口对(TCP四元组)不能再被使用.
	     * 这个连接只能在2MSL结束后才能再被使用.
	     *
	     * 许多具体的实现中允许一个进程重新使用仍处于2MSL等待的端口(通常是设置选项SO_REUSEADDR),
	     * 但TCP不能允许一个新的连接建立在相同的插口对上。
	     */
		.option(ChannelOption.SO_REUSEADDR, true)
		//
		.childOption(ChannelOption.SO_REUSEADDR, true)
		/**
	     * 为TCP套接字设置keepalive选项时, 如果在2个小时（实际值与具体实现有关）内在
	     * 任意方向上都没有跨越套接字交换数据, 则 TCP 会自动将 keepalive 探头发送到对端.
	     * 此探头是对端必须响应的TCP段.
	     *
	     * 期望的响应为以下三种之一:
	     * 1. 收到期望的对端ACK响应
	     *      不通知应用程序(因为一切正常), 在另一个2小时的不活动时间过后，TCP将发送另一个探头。
	     * 2. 对端响应RST
	     *      通知本地TCP对端已崩溃并重新启动, 套接字被关闭.
	     * 3. 对端没有响
	     *      套接字被关闭。
	     *
	     * 此选项的目的是检测对端主机是否崩溃, 仅对TCP套接字有效.
	     */
		.childOption(ChannelOption.SO_KEEPALIVE, true)
		/**
	     * 对此连接禁用 Nagle 算法.
	     * 在确认以前的写入数据之前不会缓冲写入网络的数据. 仅对TCP有效.
	     *
	     * Nagle算法试图减少TCP包的数量和结构性开销, 将多个较小的包组合成较大的包进行发送.
	     * 但这不是重点, 关键是这个算法受TCP延迟确认影响, 会导致相继两次向连接发送请求包,
	     * 读数据时会有一个最多达500毫秒的延时.
	     *
	     * 这叫做“ACK delay”, 解决办法是设置TCP_NODELAY。
	     */
        .childOption(ChannelOption.TCP_NODELAY, true)
        /**
         * 禁用掉半关闭的状态的链接状态
         * TCP四次握手关闭连接的时候，step2-step3中出现的状态
         */
        .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
		
	}


	@Override
	protected EventLoopGroup initEventLoopGroup(int nthread, ThreadFactory bossFactory) {
		return NativeSupport.isSupportNativeET() ? new EpollEventLoopGroup(nthread, bossFactory) : new NioEventLoopGroup(nthread, bossFactory);
	}


	@Override
	protected ChannelFuture bind(SocketAddress localAddress) {
		ServerBootstrap boot = bootstrap();
		
		boot.channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                		//每隔60s的时间内如果没有接受到任何的read事件的话，则会触发userEventTriggered事件，并指定IdleState的类型为READER_IDLE
                		new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS),
                		//因为我们在client端设置了每隔30s会发送一个心跳包过来，如果60s都没有收到心跳，则说明链路发生了问题
                        idleStateTrigger,
                        //message的解码器
                        new MessageDecoder(),
                        encoder,
                        ackEncoder,
                        new NettyConnetManageHandler(),
                        handler);
            }
        });
		
		return boot.bind(localAddress);
	}
	
	/**
	 * 解码器，继承于ReplayingDecoder
	 * @author BazingaLyn
	 * @description 
	 * @time
	 * @modifytime
	 */
	/**
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 用来表示消息类型
     * + 1 // 空
     * + 8 // 消息 id long 类型
     * + 4 // 消息体body长度, int类型
     */
	static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

		//构造函数 设置初始的枚举类型是什么
        public MessageDecoder() {
            super(State.HEADER_MAGIC);
        }

        // 协议头
        private final NettyCommonProtocol header = new NettyCommonProtocol();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case HEADER_MAGIC:
                    checkMagic(in.readShort());             // MAGIC
                    checkpoint(State.HEADER_SIGN);
                case HEADER_SIGN:
                    header.sign(in.readByte());             // 消息标志位
                    checkpoint(State.HEADER_STATUS);
                case HEADER_STATUS:
                    in.readByte();                          // no-op
                    checkpoint(State.HEADER_ID);
                case HEADER_ID:
                    header.id(in.readLong());               // 消息id
                    checkpoint(State.HEADER_BODY_LENGTH);
                case HEADER_BODY_LENGTH:
                    header.bodyLength(in.readInt());        // 消息体长度
                    checkpoint(State.BODY);
                case BODY:
                    switch (header.sign()) {
                        case HEARTBEAT:
                            break;
                        case REQUEST:
                        case SERVICE_1:
                        case SERVICE_2:
                        case SERVICE_3:
                        case SERVICE_4: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Message msg = serializerImpl().readObject(bytes, Message.class);
                            msg.sign(header.sign());
                            out.add(msg);

                            break;
                        }
                        default:
                            throw new IllegalAccessException();
                    }
                    checkpoint(State.HEADER_MAGIC);
            }
        }
        private static void checkMagic(short magic) throws Signal {
            if (MAGIC != magic) {
                throw new IllegalArgumentException();
            }
        }

        enum State {
            HEADER_MAGIC,
            HEADER_SIGN,
            HEADER_STATUS,
            HEADER_ID,
            HEADER_BODY_LENGTH,
            BODY
        }
	}
	
	/**
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 用来表示消息类型
     * + 1 // 空
     * + 8 // 消息 id long 类型
     * + 4 // 消息体body长度, int类型
     */
    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            byte[] bytes = serializerImpl().writeObject(msg);
            out.writeShort(MAGIC)
                    .writeByte(msg.sign())
                    .writeByte(0)
                    .writeLong(0)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        }
    }
    
    @ChannelHandler.Sharable
    class MessageHandler extends SimpleChannelInboundHandler<Message> {
    	
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
			Channel channel = ctx.channel();
			logger.info(message.toString());
    		
    		// 接收到发布信息的时候，要给Client端回复ACK
			channel.writeAndFlush(new Acknowledge(message.sequence())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		}
    }
    
    class NettyConnetManageHandler extends ChannelDuplexHandler {
    	
    	@Override
    	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise future) throws Exception {
    		final String local = localAddress == null ? "UNKNOW" : localAddress.toString();
            final String remote = remoteAddress == null ? "UNKNOW" : remoteAddress.toString();
            logger.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);
    		super.connect(ctx, remoteAddress, localAddress, future);
    		
    		if (DefaultCommonSrvAcceptor.this.channelEventListener != null) {
    			DefaultCommonSrvAcceptor.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress
                    .toString(), ctx.channel()));
            }
    	}
    	
    	@Override
    	public void disconnect(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
    		final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            super.disconnect(ctx, future);

            if (DefaultCommonSrvAcceptor.this.channelEventListener != null) {
            	DefaultCommonSrvAcceptor.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress
                    .toString(), ctx.channel()));
            }
    	}
    	
    	@Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            super.close(ctx, promise);

            if (DefaultCommonSrvAcceptor.this.channelEventListener != null) {
            	DefaultCommonSrvAcceptor.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress
                    .toString(), ctx.channel()));
            }
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            if (DefaultCommonSrvAcceptor.this.channelEventListener != null) {
            	DefaultCommonSrvAcceptor.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress
                    .toString(), ctx.channel()));
            }
        }
    }

	@Override
	protected ChannelEventListener getChannelEventListener() {
		return channelEventListener;
	}
}
