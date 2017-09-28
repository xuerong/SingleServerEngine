package com.mm.engine.sysBean.entrance;

import com.mm.engine.framework.control.request.RequestService;
import com.mm.engine.framework.data.entity.account.AccountSysService;
import com.mm.engine.framework.data.entity.account.LoginSegment;
import com.mm.engine.framework.data.entity.account.MessageSender;
import com.mm.engine.framework.data.entity.session.ConnectionClose;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.entrance.Entrance;
import com.mm.engine.framework.net.entrance.socket.NettyHelper;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.sysBean.service.AccountService;
import com.mm.engine.sysBean.service.NettyPBMessageSender;
import com.protocol.AccountOpcode;
import com.protocol.AccountPB;
import com.protocol.BaseOpcode;
import com.protocol.BasePB;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by a on 2016/9/19.
 */
public class RequestNettyPBEntrance extends Entrance {
    private static final Logger log = LoggerFactory.getLogger(RequestNettyPBEntrance.class);

    Channel channel = null;
    private static AccountSysService accountSysService;
    private static SessionService sessionService;
    private static RequestService requestService;
    private static AccountService accountService;
    @Override
    public void start() throws Exception {
        accountSysService = BeanHelper.getServiceBean(AccountSysService.class);
        sessionService = BeanHelper.getServiceBean(SessionService.class);
        requestService = BeanHelper.getServiceBean(RequestService.class);
        accountService = BeanHelper.getServiceBean(AccountService.class);

        channel = NettyHelper.createAndStart(
                port,RequestNettyPBEncoder.class,RequestNettyPBDecoder.class,RequestNettyPBHandler.class,name);
        log.info("RequestNettyPBEntrance bind port :"+port);
    }

    @Override
    public void stop() throws Exception {

    }

    public static class RequestNettyPBHandler extends ChannelInboundHandlerAdapter {
        static AttributeKey<String> sessionKey = AttributeKey.newInstance("sessionKey");
        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception { // (1)
            super.channelActive(ctx);
            System.out.println("connect "+ctx.channel().remoteAddress().toString());
        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            System.out.println("disConnect"+ctx.channel().remoteAddress().toString());
            String sessionId = ctx.channel().attr(sessionKey).get();
            if(sessionId != null) {
                accountSysService.netDisconnect(sessionId);
            }else{
                log.error("channelInactive , but session = "+sessionId);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
            NettyPBPacket nettyPBPacket = (NettyPBPacket) msg;
//            log.info("nettyPBPacket.getOpcode() = "+nettyPBPacket.getOpcode());
            try {
                String sessionId = ctx.channel().attr(sessionKey).get();
                RetPacket retPacket = null;
                if (nettyPBPacket.getOpcode() == AccountOpcode.CSLogin) { // 登陆消息
                    retPacket = accountService.login(nettyPBPacket.getOpcode(),nettyPBPacket.getData(),ctx,sessionKey);
                }else{
                    Session session = checkAndGetSession(sessionId);
                    retPacket = requestService.handle(nettyPBPacket.getOpcode(),nettyPBPacket.getData(),session);
                }
                if(retPacket == null){
                    throw new MMException("server error!");
                }
                nettyPBPacket.setOpcode(retPacket.getOpcode());
//                System.out.println("id:"+id);
                nettyPBPacket.setData((byte[])retPacket.getRetData());
                ctx.writeAndFlush(nettyPBPacket);
            }catch (Throwable e){
                int errCode = -1000;
                String errMsg = "系统异常";
                if(e instanceof MMException){
                    MMException mmException = (MMException)e;
                    log.error("MMException:"+mmException.getMessage());
                }else if(e instanceof ToClientException){
                    ToClientException toClientException = (ToClientException)e;
                    errCode = toClientException.getErrCode();
                    errMsg = toClientException.getMessage();
                    log.error("ToClientException:"+toClientException.getMessage());
                }else {
                    log.error("",e);
                }
                BasePB.SCException.Builder scException = BasePB.SCException.newBuilder();
                scException.setErrCode(errCode);
                scException.setErrMsg(errMsg);
                nettyPBPacket.setOpcode(BaseOpcode.SCException);
                nettyPBPacket.setData(scException.build().toByteArray());
                ctx.writeAndFlush(nettyPBPacket);
            }
        }
        private Session checkAndGetSession(String sessionId){
            if (sessionId == null || sessionId.length() == 0){
                throw new MMException("won't get sessionId while :"+sessionId);
            }
            // 不是login，可以处理消息
            Session session = sessionService.get(sessionId);
            if(session == null){
                throw new MMException("login timeout , please login again");
            }
            return session;
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}
