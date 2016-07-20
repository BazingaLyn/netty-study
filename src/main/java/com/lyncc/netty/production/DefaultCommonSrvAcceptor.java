package com.lyncc.netty.production;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import com.lyncc.netty.production.common.NativeSupport;

/**
 * 
 * @author BazingaLyn
 * @description 基本的常用的netty Server配置
 * @time 2016年7月20日20:49:53
 * @modifytime
 */
public class DefaultCommonSrvAcceptor extends NettySrvAcceptor {

	public DefaultCommonSrvAcceptor(SocketAddress localAddress) {
		super(localAddress);
		this.init();
	}
	
	@Override
	protected void init() {
		super.init();
		
		//设置TCP的backlog,tcp链接的时候需要进行
		//三次握手，
		bootstrap().option(ChannelOption.SO_BACKLOG, 32768)
		//
		.option(ChannelOption.SO_REUSEADDR, true);
		//
		
		
	}


	@Override
	protected EventLoopGroup initEventLoopGroup(int nthread, ThreadFactory bossFactory) {
		return NativeSupport.isSupportNativeET() ? new EpollEventLoopGroup(nthread, bossFactory) : new NioEventLoopGroup(nthread, bossFactory);
	}


	@Override
	protected ChannelFuture bind(SocketAddress localAddress) {
		return null;
	}

}
