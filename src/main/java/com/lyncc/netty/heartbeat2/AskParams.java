package com.lyncc.netty.heartbeat2;

import java.io.Serializable;

public class AskParams implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String auth;

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
    

}
