package com.lyncc.netty.production.srv.acceptor;

import java.net.SocketAddress;

/**
 * 
 * @author BazingaLyn
 * @description netty server端的标准接口定义
 * @time 2016年7月20日16:41:04
 * @modifytime
 */
public interface SrvAcceptor {
	
	SocketAddress localAddress();
	
	void start() throws InterruptedException;
	
	void shutdownGracefully();
	
	void start(boolean sync) throws InterruptedException;

}
