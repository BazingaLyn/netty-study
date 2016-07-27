package com.lyncc.netty.production.common;

/**
 * 
 * @author BazingaLyn
 * @description Netty的C/S端的之间约定的协议
 * @time 2016年7月27日09:20:55
 * @modifytime
 */
public class NettyCommonProtocol {
	
	/** 协议头长度 */
    public static final int HEAD_LENGTH = 16;
    /** Magic */
    public static final short MAGIC = (short) 0xbabe;


    /** Request */
    public static final byte REQUEST = 1;
    /** Response */
    public static final byte RESPONSE = 2;

    public static final byte SERVICE_1 = 3;
    public static final byte SERVICE_2 = 4;
    public static final byte SERVICE_3 = 5;
    public static final byte SERVICE_4 = 6;

    /** Acknowledge */
    public static final byte ACK = 126;
    /** Heartbeat */
    public static final byte HEARTBEAT = 127;

    private byte sign;
    private byte status;
    private long id;
    private int bodyLength;

    public byte sign() {
        return sign;
    }

    public void sign(byte sign) {
        this.sign = sign;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    public int bodyLength() {
        return bodyLength;
    }

    public void bodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

	@Override
	public String toString() {
		return "NettyCommonProtocol [sign=" + sign + ", status=" + status + ", id=" + id + ", bodyLength=" + bodyLength + "]";
	}

}
