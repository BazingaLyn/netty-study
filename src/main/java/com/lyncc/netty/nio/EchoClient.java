package com.lyncc.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class EchoClient {

	public static void main(String[] args) throws IOException {

		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		InetSocketAddress s = new InetSocketAddress("localhost", 2000);
		channel.connect(s);

		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

		Charset charset = Charset.forName("GBK");

		boolean isFinished = false;
		while (!isFinished) {
			int num = selector.select();
			if (num > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey k : keys) {
					if (k.isConnectable()) {
						SocketChannel sc = (SocketChannel) k.channel();
						sc.configureBlocking(false);
						sc.finishConnect();
						sc.register(selector, SelectionKey.OP_READ);

						ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
						ByteBuffer info = charset.encode("好了克隆技术杜洛克防水堵漏开发!");
						echoBuffer.put(info);

						echoBuffer.flip();

						sc.write(echoBuffer);
						echoBuffer.clear();

					} else if (k.isValid() && k.isReadable()) {
						ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
						SocketChannel sc = (SocketChannel) k.channel();
						sc.read(echoBuffer);
						echoBuffer.flip();

						System.out.println("echo server return:" + charset.decode(echoBuffer).toString());
						echoBuffer.clear();

						isFinished = true;

						k.cancel();
						sc.close();
						selector.close();
					}
				}
			}
		}
	}
}
