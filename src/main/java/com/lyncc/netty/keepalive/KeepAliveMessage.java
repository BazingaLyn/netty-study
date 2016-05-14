package com.lyncc.netty.keepalive;

public class KeepAliveMessage implements java.io.Serializable{

	private static final long serialVersionUID = 479148324800517968L;

	private String sn ;
	
	private int reqCode ; // 1:心跳请求  2:心跳反馈

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public int getReqCode() {
		return reqCode;
	}

	public void setReqCode(int reqCode) {
		this.reqCode = reqCode;
	}
}
