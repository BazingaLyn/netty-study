package com.lyncc.netty.production.example;

import com.lyncc.netty.production.srv.acceptor.DefaultCommonSrvAcceptor;

public class SrvAcceptorStartup {
	
	public static void main(String[] args) throws InterruptedException {
		
		DefaultCommonSrvAcceptor defaultCommonSrvAcceptor = new DefaultCommonSrvAcceptor(20011,null);
		defaultCommonSrvAcceptor.start();
		
	}

}
