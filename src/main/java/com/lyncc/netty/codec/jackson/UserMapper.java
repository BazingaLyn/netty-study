package com.lyncc.netty.codec.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserMapper {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ObjectMapper getInstance() {
        return MAPPER;
    }

}
