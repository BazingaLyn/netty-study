package com.lyncc.netty.heartbeat;

public class PingMsg extends BaseMsg{
    
    public PingMsg() {
        super();
        setType(MsgType.PING);
    }

}
