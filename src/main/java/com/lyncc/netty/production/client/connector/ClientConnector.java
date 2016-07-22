package com.lyncc.netty.production.client.connector;

import io.netty.channel.Channel;

/**
 * 
 * @author BazingaLyn
 * @description
 * @time 2016年7月21日10:47:12
 * @modifytime
 */
public interface ClientConnector {
	
	Channel connect(int port,String host);
	
	void shutdownGracefully();
	
}
