package com.lyncc.netty.production;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionWatchdog.class);

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final int port;
    private final String host;

    private volatile boolean reconnect = true;
    private int attempts;

    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port,String host) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.port = port;
        this.host = host;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        attempts = 0;

        logger.info("Connects with {}.", channel);

        ctx.fireChannelActive();
    }

    /**
     * 因为链路断掉之后，会触发channelInActive方法，进行重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        boolean doReconnect = reconnect;
        if (doReconnect) {
            if (attempts < 12) {
                attempts++;
            }
            long timeout = 2 << attempts;
            timer.newTimeout(this, timeout, MILLISECONDS);
        }

        logger.warn("Disconnects with {}, port: {},host {}, reconnect: {}.", ctx.channel(), port,host, doReconnect);

        ctx.fireChannelInactive();
    }

    public void run(Timeout timeout) throws Exception {

        ChannelFuture future;
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(host,port);
        }

        future.addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture f) throws Exception {
                boolean succeed = f.isSuccess();

                logger.warn("Reconnects with {}, {}.", host+":"+port, succeed ? "succeed" : "failed");

                if (!succeed) {
                    f.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }
}
