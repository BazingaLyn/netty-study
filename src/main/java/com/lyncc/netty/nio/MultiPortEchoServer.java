package com.lyncc.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class MultiPortEchoServer {
private Charset charset=Charset.forName("GBK");
    
    private int[] ports;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        int[] ps = {2000,2001};   //默认监听2000,2001端口
        
        new MultiPortEchoServer(ps);

    }
    
    public MultiPortEchoServer(int[] ports){
        this.ports = ports;
        try {
            go();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void go() throws IOException{
        
        Selector selector = Selector.open();
        
        for(int i=0;i<ports.length;i++){
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            ServerSocket socket = channel.socket();
            InetSocketAddress address = new InetSocketAddress("localhost",ports[i]);
            socket.bind(address);
            
            //注册接受连接事件
            channel.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println( "Going to listen on "+ports[i] ); 
        }
        
        while(true){
            
            int num = selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while(iter.hasNext()){
                SelectionKey key = iter.next();
                if((key.readyOps()&SelectionKey.OP_ACCEPT)==SelectionKey.OP_ACCEPT){
                    ServerSocketChannel  ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    
                    sc.register(selector, SelectionKey.OP_READ);
                    iter.remove();
                }else if((key.readyOps()&SelectionKey.OP_READ)==SelectionKey.OP_READ){
                    
                    SocketChannel sc = (SocketChannel) key.channel();
                    
                    if(!sc.isOpen()){
                        selector = Selector.open();
                    }else{
                        ByteBuffer echoBuffer = ByteBuffer.allocate(1024);  
                        
                        //int x = sc.read(echoBuffer);
                        while(sc.read(echoBuffer)>0){
                            
                            System.out.println( "Echoed "+charset.decode(echoBuffer).toString()+" from "+sc.socket().getInetAddress().getHostAddress() );  
                            echoBuffer.flip();
                            sc.write(echoBuffer);
                            echoBuffer.clear();
                        }
                        
                        
                        iter.remove();
                        
                        /*返回信息后关闭连接*/
                        key.cancel();
                        sc.close();
                    }
                }
            }
            
            keys.clear();
        }
    }


}
