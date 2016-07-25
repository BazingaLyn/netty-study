package com.lyncc.netty.production.client.connector;

import static com.lyncc.netty.production.common.JProtocolHeader.ACK;
import static com.lyncc.netty.production.common.JProtocolHeader.MAGIC;
import static com.lyncc.netty.production.common.JProtocolHeader.OFFLINE_NOTICE;
import static com.lyncc.netty.production.common.JProtocolHeader.PUBLISH_CANCEL_SERVICE;
import static com.lyncc.netty.production.common.JProtocolHeader.PUBLISH_SERVICE;
import static com.lyncc.netty.production.serializer.SerializerHolder.serializerImpl;
import static java.util.concurrent.TimeUnit.SECONDS;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lyncc.netty.production.ConnectionWatchdog;
import com.lyncc.netty.production.common.Acknowledge;
import com.lyncc.netty.production.common.JProtocolHeader;
import com.lyncc.netty.production.common.Message;
import com.lyncc.netty.production.common.NativeSupport;
import com.lyncc.netty.production.common.exception.ConnectFailedException;
import com.lyncc.netty.production.srv.acceptor.AcknowledgeEncoder;

/**
 * 
 * @author BazingaLyn
 * @description 默认的一些比较常用的client的配置
 * @time 2016年7月22日14:54:37
 * @modifytime
 */
public class DefaultCommonClientConnector extends NettyClientConnector {
	
	//每个连接维护一个channel
	private volatile Channel channel;
	
	//信息处理的handler
	private final MessageHandler handler = new MessageHandler();
	//编码
    private final MessageEncoder encoder = new MessageEncoder();
    //ack
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();
	
	protected final HashedWheelTimer timer = new HashedWheelTimer(new ThreadFactory() {
		
		private AtomicInteger threadIndex = new AtomicInteger(0);
		
		public Thread newThread(Runnable r) {
			return new Thread(r, "NettyClientConnectorExecutor_" + this.threadIndex.incrementAndGet());
		}
	});
	
	private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();

	public DefaultCommonClientConnector() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		bootstrap().option(ChannelOption.ALLOCATOR, allocator)
		.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
		.option(ChannelOption.SO_REUSEADDR, true)
		.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3))
		.channel(NioSocketChannel.class);

		bootstrap().option(ChannelOption.SO_KEEPALIVE, true)
		.option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
		
	}

	public Channel connect(int port, String host) {
		
		final Bootstrap boot = bootstrap();
		
        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, port,host) {

            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                		//将自己[ConnectionWatchdog]装载到handler链中，当链路断掉之后，会触发ConnectionWatchdog #channelInActive方法
                		
                        this,
                        //每隔30s的时间触发一次userEventTriggered的方法，并且指定IdleState的状态位是WRITER_IDLE
                        new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS),
                        //实现userEventTriggered方法，并在state是WRITER_IDLE的时候发送一个心跳包到sever端，告诉server端我还活着
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler
                };
            }};
        watchdog.setReconnect(true);

        try {
            ChannelFuture future;
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect("127.0.0.1", 20011);
            }
            future.sync();
            channel = future.channel();
        } catch (Throwable t) {
            throw new ConnectFailedException("connects to [" + host + ":"+port+"] fails", t);
        }
		return channel;
	}
	
	@ChannelHandler.Sharable
    class MessageHandler extends ChannelInboundHandlerAdapter {
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			super.channelRead(ctx, msg);
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

        public MessageDecoder() {
            super(State.HEADER_MAGIC);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

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
                        case PUBLISH_SERVICE:
                        case PUBLISH_CANCEL_SERVICE:
                        case OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Message msg = serializerImpl().readObject(bytes, Message.class);
                            msg.sign(header.sign());
                            out.add(msg);

                            break;
                        }
                        case ACK: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Acknowledge ack = serializerImpl().readObject(bytes, Acknowledge.class);
                            out.add(ack);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException();

                    }
                    checkpoint(State.HEADER_MAGIC);
            }
        }

        private static void checkMagic(short magic) throws Exception {
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

	@Override
	protected EventLoopGroup initEventLoopGroup(int nWorkers, ThreadFactory workerFactory) {
		return NativeSupport.isSupportNativeET() ? new EpollEventLoopGroup(nWorkers, workerFactory) : new NioEventLoopGroup(nWorkers, workerFactory);
	}

}
