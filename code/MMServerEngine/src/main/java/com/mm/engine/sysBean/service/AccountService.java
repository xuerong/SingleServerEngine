package com.mm.engine.sysBean.service;

import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.entity.account.Account;
import com.mm.engine.framework.data.entity.account.AccountSysService;
import com.mm.engine.framework.data.entity.account.LoginSegment;
import com.mm.engine.framework.data.entity.account.MessageSender;
import com.mm.engine.framework.data.entity.session.ConnectionClose;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.ServerType;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.protocol.AccountOpcode;
import com.protocol.AccountPB;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/9/20.
 */
@Service(init = "init")
public class AccountService {
    // account-session
    private ConcurrentHashMap<String,Session> sessionMap;

    public AccountSysService accountSysService;

    public void init(){
        sessionMap = new ConcurrentHashMap<>();
    }

    public RetPacket login(int opcode, Object data, ChannelHandlerContext ctx,AttributeKey<String> sessionKey) throws Throwable{
        AccountPB.CSLogin csLoginMain = AccountPB.CSLogin.parseFrom((byte[])data);
        String accountId = csLoginMain.getAccountId();

        LoginSegment loginSegment = accountSysService.login(accountId,csLoginMain.getUrl(),ctx.channel().remoteAddress().toString());
        Account account = loginSegment.getAccount();

        {
            // 一些对account的设置，并保存
        }
        Session session = loginSegment.getSession();
        MessageSender messageSender = new NettyPBMessageSender(ctx.channel());
        session.setMessageSender(messageSender);
        final ChannelHandlerContext _ctx = ctx;
        session.setConnectionClose(new ConnectionClose() {
            @Override
            public void close() {
                _ctx.close();
            }
        });
        ctx.channel().attr(sessionKey).set(loginSegment.getSession().getSessionId());
        AccountPB.SCLogin.Builder builder = AccountPB.SCLogin.newBuilder();
        builder.setSessionId(loginSegment.getSession().getSessionId());
        RetPacket retPacket = new RetPacketImpl(AccountOpcode.SCLogin,false,builder.build().toByteArray());
        return retPacket;
    }

    @Request(opcode = AccountOpcode.CSLogin)
    public RetPacket login(Object data, Session session) throws Throwable{
        if(!ServerType.isMainServer()){
            throw new MMException("login fail,this is not mainServer");
        }
        AccountPB.CSLogin csLoginMain = AccountPB.CSLogin.parseFrom((byte[])data);
        String accountId = csLoginMain.getAccountId();

        LoginSegment loginSegment = accountSysService.login(accountId,session.getUrl(),session.getIp());
        Account account = loginSegment.getAccount();

        {
            // 一些对account的设置，并保存
        }
        AccountPB.SCLogin.Builder builder = AccountPB.SCLogin.newBuilder();
        builder.setSessionId(loginSegment.getSession().getSessionId());
        RetPacket retPacket = new RetPacketImpl(AccountOpcode.SCLogin,false,builder.build().toByteArray());
        return retPacket;
    }
    @Request(opcode = AccountOpcode.CSLogout)
    public RetPacket logout(Object data,Session session) throws Throwable{
//        if(!ServerType.isMainServer()){
//            throw new MMException("logout fail,this is not mainServer");
//        }
        AccountPB.CSLogout csLogoutMain = AccountPB.CSLogout.parseFrom((byte[])data);
        String accountId = csLogoutMain.getAccountId();
        accountSysService.logout(accountId);

        AccountPB.SCLogout.Builder builder = AccountPB.SCLogout.newBuilder();
        RetPacket retPacket = new RetPacketImpl(AccountOpcode.SCLogout,false,builder.build().toByteArray());
        return retPacket;
    }

}
