package com.lyncc.netty.production.example;

import static com.lyncc.netty.production.common.NettyCommonProtocol.REQUEST;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lyncc.netty.production.client.connector.DefaultCommonClientConnector;
import com.lyncc.netty.production.client.connector.DefaultCommonClientConnector.MessageNonAck;
import com.lyncc.netty.production.common.Message;
import com.lyncc.netty.production.srv.acceptor.DefaultCommonSrvAcceptor;

/**
 * 
 * @author BazingaLyn
 * @description 客户链接端 启动类
 * @time 2016年7月22日14:53:32
 * @modifytime
 */
public class ClientConnectorStartup {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultCommonSrvAcceptor.class);
	
	public static void main(String[] args) {
		
		DefaultCommonClientConnector clientConnector = new DefaultCommonClientConnector();
		Channel channel = clientConnector.connect(20011, "127.0.0.1");
		User user = new User(1, "dubbo");
		Message message = new Message();
		message.sign(REQUEST);
		message.data(user);
		//获取到channel发送双方规定的message格式的信息
		channel.writeAndFlush(message).addListener(new ChannelFutureListener() {
			
			public void operationComplete(ChannelFuture future) throws Exception {
				 if(!future.isSuccess()) {  
	                    logger.info("send fail,reason is {}",future.cause().getMessage());  
	                } 
			}
		});
		//防止对象处理发生异常的情况
		MessageNonAck msgNonAck = new MessageNonAck(message, channel);
		clientConnector.addNeedAckMessageInfo(msgNonAck);
	}
	
	public static class User {
		
		private Integer id;
		
		private String username;
		

		public User(Integer id, String username) {
			this.id = id;
			this.username = username;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@Override
		public String toString() {
			return "User [id=" + id + ", username=" + username + "]";
		}
		
	}

}
