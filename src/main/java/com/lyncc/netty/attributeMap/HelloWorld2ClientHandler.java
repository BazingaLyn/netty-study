package com.lyncc.netty.attributeMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.HashSet;

public class HelloWorld2ClientHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<HashSet<Integer>> NETTY_CHANNEL_KEY1 = AttributeKey.valueOf("netty.channel1");
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Attribute<HashSet<Integer>> attr = ctx.channel().attr(NETTY_CHANNEL_KEY1);
        HashSet<Integer> sets = attr.get();
        if (sets == null) {
            HashSet<Integer> newSet = new HashSet<Integer>();
            sets = attr.setIfAbsent(newSet);
            if(null == sets){
                System.out.println("GGGGGGGGGGGGGGGGGGGGGG NULLLLLLLLLLL");
                sets = newSet;
            }
            HashSet<Integer> sets2 = attr.get();
            System.out.println("RRRRRRRRRRRRRRRRR ==="+sets2.size());
            for(Integer i :sets2){
                System.out.println("value is GGGGGGGGGGG===="+i);
            }
            
        } 
        sets.add(1);
        HashSet<Integer> sets3 = attr.get();
        System.out.println("RRRRRRRRRRRRRRRRR2 ==="+sets3.size());
        System.out.println("HelloWorldC2ientHandler Active");
        ctx.fireChannelActive();
    }
    
    
    public static void main(String[] args) {
         Student student = new Student("1", 21);
         Student s =student;
         
         s.setAge(88);
         System.out.println(student.getAge());
        
    }
    
     static class Student {
       String id;
       int age;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public Student(String id, int age) {
        super();
        this.id = id;
        this.age = age;
    }
       
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Attribute<HashSet<Integer>> attr = ctx.channel().attr(NETTY_CHANNEL_KEY1);
        HashSet<Integer> sets = attr.get();
        if (sets == null) {
           System.out.println("没有值啊");
        }else{
            for(Integer i :sets){
                System.out.println("value is ===="+i);
            }
        }
        System.out.println("HelloWorldClientHandler read Message:" + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
