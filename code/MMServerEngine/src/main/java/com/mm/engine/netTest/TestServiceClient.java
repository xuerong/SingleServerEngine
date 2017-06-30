package com.mm.engine.netTest;

import com.mm.engine.framework.net.client.http.HttpClient;
import com.mm.engine.framework.net.client.http.HttpPBPacket;
import com.mm.engine.framework.net.client.socket.NettyClient;
import com.protocol.AccountOpcode;
import com.protocol.AccountPB;
import com.protocol.Test;
import com.protocol.TestOpcode;

/**
 * Created by a on 2016/11/3.
 */
public class TestServiceClient {
    public static void main(String[] args) throws Throwable{
        // 账号
        String accountId = "accountId_1241";
        // 连接nodeServer
        NettyClient nettyClient = new NettyClient("10.1.6.254",8003);
        nettyClient.start();
        // 登录nodeServer
        AccountPB.CSLogin.Builder loginNodeBuilder = AccountPB.CSLogin.newBuilder();
        loginNodeBuilder.setAccountId(accountId);
        loginNodeBuilder.setUrl("aaa");
        loginNodeBuilder.setIp("ip");
        byte[] reData = nettyClient.send(AccountOpcode.CSLogin,loginNodeBuilder.build().toByteArray());
        AccountPB.SCLogin scLoginNode = AccountPB.SCLogin.parseFrom(reData);
        System.out.println("scLoginNode"+scLoginNode);
        // request测试
        Test.CSTest.Builder csTestBuilder = Test.CSTest.newBuilder();
        csTestBuilder.setCsStr("send server data...");
        byte[] testRetData = nettyClient.send(TestOpcode.CSTest,csTestBuilder.build().toByteArray());
        Test.SCTest scTest = Test.SCTest.parseFrom(testRetData);
        System.out.println("receive server back data:"+scTest.getScStr());
        // 登出mainServer，同时也登出了nodeServer
        AccountPB.CSLogout.Builder logoutMainBuilder = AccountPB.CSLogout.newBuilder();
        logoutMainBuilder.setAccountId(accountId);
        reData = nettyClient.send(AccountOpcode.CSLogout,logoutMainBuilder.build().toByteArray());
//        AccountPB.SCLogout scLogoutMain = AccountPB.SCLogout.parseFrom(reData);
        System.out.println("logout");
    }
}
