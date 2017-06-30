package com.mm.engine.framework.data.entity.account;

import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.control.event.EventService;
import com.mm.engine.framework.control.netEvent.remote.RemoteCallService;
import com.mm.engine.framework.control.netEvent.ServerInfo;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.entrance.Entrance;
import com.mm.engine.framework.security.MonitorService;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.Server;
import com.mm.engine.framework.server.ServerType;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/9/18.
 * 客户端首先登陆mainServer，获取需要登陆的nodeServer
 * mainServer保留该用户的登陆nodeServer
 *
 * 抛出三个事件：创建account，登陆，登出
 * 登陆对外接口：loginMain，传出LoginSegment
 * 登出对外接口：logout
 *
 * TODO 有三个东西要具有一致性：账号、session、socket连接
 *
 * TODO 有必要再登陆登出的时候给sessionId和accountId们加锁，以防止出现并发问题
 * TODO 在mainServer上面登陆后，如果在指定时间内没有在nodeServer上面登陆，则清除掉它
 */
@Service(init = "init")
public class AccountSysService {
    private static final Logger log = LoggerFactory.getLogger(AccountSysService.class);
    /**
     * 用于nodeServer：
     * accountId - sessionId
     *  login的时候，用这个校验：
     *  不存在：不允许登录，
     */
    private ConcurrentHashMap<String,String> nodeServerLoginMark = new ConcurrentHashMap<>();

    private DataService dataService;
    private SessionService sessionService;
    private RemoteCallService remoteCallService;
    private MonitorService monitorService;
    private EventService eventService;

    public void init(){
        dataService = BeanHelper.getServiceBean(DataService.class);
        sessionService = BeanHelper.getServiceBean(SessionService.class);
        remoteCallService = BeanHelper.getServiceBean(RemoteCallService.class);
        eventService = BeanHelper.getServiceBean(EventService.class);
    }
    /**
     * 登陆mainServer，
     * 1、mainServer获取分配给它的nodeServer
     * 2、通知nodeServer客户端的登陆请求，后面客户端登陆nodeServer时要校验
     * 3、返回分配的nodeServer的地址和访问用的sessionId
     *
     * 如果已经登陆，则把之前的账号顶下来:要考虑多个机器同时登陆一个账号时的同步问题
     * @param id
     */
    @Tx(tx = true,lock = true,lockClass = {Account.class})
    public LoginSegment login(String id,String url,String ip){
        // check id
        if(id == null || id.length() == 0){
            throw new MMException("id error, id="+id);
        }
        // get account
        Account account = dataService.selectObject(Account.class,"id=?",id);
        if(account == null){
            // 没有则创建
            account = createAccount(id);
            dataService.insert(account);
            eventService.fireEventSyn(account,SysConstantDefine.Event_AccountCreate);
//            throw new MMException("account is not exist, id="+id);
        }

        Session session = applyForLogin(id,url,ip);
        if(session == null){
            throw new MMException("login false,see log on ");
        }
        //
        LoginSegment loginSegment = new LoginSegment();
        loginSegment.setSession(session);
        loginSegment.setAccount(account);

        log.info("accountId="+id+" loginMain success,nodeServerAdd="+",sessionId="+session.getSessionId());

        return loginSegment;
    }
    /**
    * nodeServer接收，来自mainServer的一个account的login请求
    * 如果可以登录，
    * 1、如果已经登录，在这里销毁之前的session
    * 2、创建session，
    * @return 返回sessionId
     **/
    public Session applyForLogin(String id,String url,String ip){
        Session session = sessionService.create(url,ip);
        String olderSessionId = nodeServerLoginMark.put(id,session.getSessionId());
        if(olderSessionId != null){
            // 通知下线
            doLogout(id,olderSessionId,LogoutReason.replaceLogout);
        }
        session.setAccountId(id);
        eventService.fireEventSyn(session,SysConstantDefine.Event_AccountLogin);
        return session;
    }

    /**
     * 登出mainServer
     * account主动登出，
     */
    public void logout(String id){
        applyForLogout(id);
        log.info("accountId="+id+" logoutMain success");
    }

    /**
     * mainServer向nodeServer要求登出某个玩家
     * 去掉session
     * @param id
     */
    public void applyForLogout(String id){
        String sessionId = nodeServerLoginMark.get(id);
        if(sessionId == null){
            throw new MMException("sessionId is not exist , accountId = "+id+"");
        }
        doLogout(id,sessionId,LogoutReason.userLogout);
    }

    /**
     * 由于网络断线而导致的登出，要通知mainServer
     */
    public void netDisconnect(String sessionId){
        Session session = sessionService.get(sessionId);
        if(session == null){
            // 说明：1还没有登录nodeServer，2正常的登出，该清理的已经清理完成
            return;
        }
        doLogout(session.getAccountId(),sessionId,LogoutReason.netDisconnect);
    }

    /**
     * 执行登出操作 TODO 是否要强制断开socket连接？有必要
     * @param sessionId
     * @param logoutReason
     */
    private void doLogout(String accountId,String sessionId , LogoutReason logoutReason){
        Session session = sessionService.removeSession(sessionId);
        if(session.getAccountId() == null){
            log.error("session.getAccountId() == null"+logoutReason.toString());
        }else{
            Account account = dataService.selectObject(Account.class,"id="+session.getAccountId());
            if(account!=null){
                nodeServerLoginMark.remove(account.getId());
            }
        }
        LogoutEventData logoutEventData = new LogoutEventData();
        logoutEventData.setSession(session);
        logoutEventData.setLogoutReason(logoutReason);
        eventService.fireEventSyn(logoutEventData,SysConstantDefine.Event_AccountLogout);
        //强制掉线
        session.closeConnect();
    }

    /**
     * 创建一个account
     * TODO 这个要初始化哪些数据呢？
     * accountId,有一定要求,比如要求(字母,数字,下划线,不准有空格,逗号之类的)
     * @param id
     * @return
     */
    private Account createAccount(String id){
        Account account = new Account();
        account.setId(id);
        return account;
    }


}
