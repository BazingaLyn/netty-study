
package com.lyncc.netty.production.serializer;

import com.lyncc.netty.production.serializer.protostuff.ProtoStuffSerializer;


public final class SerializerHolder {

    private static final Serializer serializer = new ProtoStuffSerializer();

    public static Serializer serializerImpl() {
        return serializer;
    }
}
