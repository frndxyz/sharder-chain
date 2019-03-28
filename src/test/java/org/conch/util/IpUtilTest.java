package org.conch.util;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-03-28
 */
public class IpUtilTest {

    public static void getIpFromUrl(){
        System.out.println(IpUtil.getIpFromUrl("192.168.31.1"));
        System.out.println(IpUtil.getIpFromUrl("devboot.sharder.io"));
        System.out.println(IpUtil.getIpFromUrl("192.168.31.1:8080"));
        System.out.println(IpUtil.getIpFromUrl("devboot.sharder.io:8080"));  
    }
    
    public static void isDomainTest(){
        System.out.println(IpUtil.isDomain("192.168.31.1"));
        System.out.println(IpUtil.isDomain("devboot.sharder.io"));
        System.out.println(IpUtil.isDomain("192.168.31.1:8080"));
        System.out.println(IpUtil.isDomain("devboot.sharder.io:8080")); 
    }
    public static void main(String[] args) {
        isDomainTest();
        getIpFromUrl();
    }
}
