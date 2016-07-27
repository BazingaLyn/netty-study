package com.lyncc.netty.production.srv.acceptor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author BazingaLyn
 * @description Netty Server接收端的{@link ServerBootstrap}的初始化和Netty的一些启动参数
 * @time 2016年7月20日19:56:45
 * @modifytime
 */
public abstract class NettySrvAcceptor implements SrvAcceptor {
	
	private static final Logger logger = LoggerFactory.getLogger(NettySrvAcceptor.class);
	
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
	
	protected final SocketAddress localAddress;
	
	//netty base element
	private ServerBootstrap bootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private int nWorkers;
    protected volatile ByteBufAllocator allocator;
	
	
	protected final HashedWheelTimer timer = new HashedWheelTimer(new ThreadFactory() {
		
		private AtomicInteger threadIndex = new AtomicInteger(0);
		
		public Thread newThread(Runnable runnable) {
			return new Thread(runnable, "NettySrvAcceptorExecutor_" + this.threadIndex.incrementAndGet());
		}
	});
	
	public NettySrvAcceptor(SocketAddress localAddress) {
        this(localAddress, AVAILABLE_PROCESSORS << 1);
    }

    public NettySrvAcceptor(SocketAddress localAddress, int nWorkers) {
        this.localAddress = localAddress;
        this.nWorkers = nWorkers;
    }
    
    //netty的元素初始化
    protected void init(){
    	ThreadFactory bossFactory = new DefaultThreadFactory("netty.acceptor.boss");
        ThreadFactory workerFactory = new DefaultThreadFactory("netty.acceptor.worker");
        
        boss = initEventLoopGroup(1, bossFactory);
        
        worker = initEventLoopGroup(nWorkers, workerFactory);
        //使用池化的directBuffer
        /**
         * 一般高性能的场景下,使用的堆外内存，也就是直接内存，使用堆外内存的好处就是减少内存的拷贝，和上下文的切换，缺点是
         * 堆外内存处理的不好容易发生堆外内存OOM
         * 当然也要看当前的JVM是否只是使用堆外内存，换而言之就是是否能够获取到Unsafe对象#PlatformDependent.directBufferPreferred()
         */
        allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
        //create && group
        bootstrap= new ServerBootstrap().group(worker, worker);
        //ByteBufAllocator 配置
        bootstrap.childOption(ChannelOption.ALLOCATOR, allocator);
    	
    }
    
    public void start() throws InterruptedException {
    	this.start(true);
    }
    
    public void start(boolean sync) throws InterruptedException {
    	ChannelFuture future = bind(localAddress).sync();

        logger.info("netty acceptor server start");

        if (sync) {
            future.channel().closeFuture().sync();
        }
    	
    }
    
    public SocketAddress localAddress() {
		return localAddress;
	}
    
    protected ServerBootstrap bootstrap() {
        return bootstrap;
    }
    
	public void shutdownGracefully() {
    	boss.shutdownGracefully().awaitUninterruptibly();
        worker.shutdownGracefully().awaitUninterruptibly();
    }

	protected abstract EventLoopGroup initEventLoopGroup(int nthread, ThreadFactory bossFactory);
	
	protected abstract ChannelFuture bind(SocketAddress localAddress);

}
