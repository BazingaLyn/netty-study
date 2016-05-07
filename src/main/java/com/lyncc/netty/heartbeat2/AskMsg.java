package com.lyncc.netty.heartbeat2;

public class AskMsg extends BaseMsg {
    
    public AskMsg() {
        super();
        setType(MsgType.ASK);
    }
    
    private AskParams params;

    public AskParams getParams() {
        return params;
    }

    public void setParams(AskParams params) {
        this.params = params;
    }
    

}
