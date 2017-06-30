package com.mm.engine.framework.net.entrance.http;

import com.google.protobuf.AbstractMessage;
import com.mm.engine.framework.control.request.RequestService;
import com.mm.engine.framework.data.entity.account.AccountSysService;
import com.mm.engine.framework.data.entity.account.LoginSegment;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.net.code.HttpDecoder;
import com.mm.engine.framework.net.entrance.Entrance;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.ServerType;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by a on 2016/8/29.
 */
public class PlayerRequestJettyEntrance extends Entrance {
    private static final Logger log = LoggerFactory.getLogger(PlayerRequestJettyEntrance.class);

    private Server server;


    public PlayerRequestJettyEntrance(){}

    private SessionService sessionService;
    private RequestService requestService;
    private AccountSysService accountSysService;
    @Override
    public void start() throws Exception {
        sessionService = BeanHelper.getServiceBean(SessionService.class);
        requestService = BeanHelper.getServiceBean(RequestService.class);
        accountSysService = BeanHelper.getServiceBean(AccountSysService.class);

        Handler entranceHandler = new AbstractHandler(){
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
                fire(request,response,"EntranceJetty");
            }
        };

        server = new Server(this.port);
        server.setHandler(entranceHandler);
        server.start();
    }

    private void fire(HttpServletRequest request, HttpServletResponse response,String entranceName){
        try {
            byte[] data = HttpDecoder.decode(request);
            // 获取controller，并根据controller获取相应的编解码器
            String opcodeStr = request.getHeader(SysConstantDefine.opcodeKey);
            if(StringUtils.isEmpty(opcodeStr) || !StringUtils.isNumeric(opcodeStr)){
                throw new MMException("opcode error");
            }
            int opcode=Integer.parseInt(opcodeStr);
            if(opcode == SysConstantDefine.loginOpcode){
                if(!ServerType.isMainServer()){
                    throw new MMException("login fail,this is not mainServer");
                }
                // 解析出来account
                String accountId = request.getHeader(SysConstantDefine.accountId);
                LoginSegment loginSegment = accountSysService.login(accountId,request.getContextPath(), Util.getIp(request));
                response.setHeader(SysConstantDefine.opcodeKey,""+SysConstantDefine.loginOpcode);
                response.setHeader(SysConstantDefine.sessionId,""+loginSegment.getSession().getSessionId());
                // 还要其他的吗？
                response.getOutputStream().flush();
                return;
            }else if(opcode == SysConstantDefine.logoutOpcode){
                if(!ServerType.isMainServer()){
                    throw new MMException("logout fail,this is not mainServer");
                }
                // 解析出来account
                String accountId = request.getHeader(SysConstantDefine.accountId);
                accountSysService.logout(accountId);
                response.setHeader(SysConstantDefine.opcodeKey,""+SysConstantDefine.logoutOpcode);
                // 还要其他的吗？
                response.getOutputStream().flush();
                return ;
            }
            // 获取sessionId
            String sessionId = request.getHeader(SysConstantDefine.sessionId);
            if(StringUtils.isEmpty(sessionId)){
                throw new MMException("not give sessionId");
            }
            Session session = sessionService.get(sessionId);
            if(session == null){
                throw new MMException("find no session , please login in mainServer before request");
            }
            RetPacket rePacket = requestService.handle(opcode, data, session);

            if(rePacket==null){
                // 处理包失败
                throw new MMException("处理消息错误,session:"+session.getSessionId());
            }
            response.setHeader(SysConstantDefine.opcodeKey,""+rePacket.getOpcode());
            response.setHeader(SysConstantDefine.sessionId,session.getSessionId());
            // TODO 这个是针对protocol buf协议的返回方式
            AbstractMessage.Builder<?> builder=(AbstractMessage.Builder<?>)rePacket.getRetData();
            byte[] reData=builder.build().toByteArray();
            // 这个地方要+1
            response.setBufferSize(reData.length+1);
            response.setContentLength(reData.length);
            response.getOutputStream().write(reData, 0, reData.length);
            response.getOutputStream().flush();
//            response.getOutputStream().close();
        }catch (Throwable e){
            // TODO 两种更可能：MMException和非MMException
            e.printStackTrace();
            throw new RuntimeException(entranceName+" Exception");
        }
    }

    @Override
    public void stop() throws Exception {
        if(server != null){
            server.stop();
        }
    }
}
