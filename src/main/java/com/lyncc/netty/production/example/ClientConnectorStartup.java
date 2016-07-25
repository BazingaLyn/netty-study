package com.lyncc.netty.production.example;

import io.netty.channel.Channel;

import com.lyncc.netty.production.client.connector.DefaultCommonClientConnector;

/**
 * 
 * @author BazingaLyn
 * @description 客户链接端 启动类
 * @time 2016年7月22日14:53:32
 * @modifytime
 */
public class ClientConnectorStartup {
	
	public static void main(String[] args) {
		
		DefaultCommonClientConnector clientConnector = new DefaultCommonClientConnector();
		Channel channel = clientConnector.connect(20011, "127.0.0.1");
	}

}
