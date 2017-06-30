package com.mm.engine.framework.data.entity.session;

import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.cache.CacheService;
import com.mm.engine.framework.data.entity.account.Account;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.Server;
import com.mm.engine.framework.server.configure.EngineConfigure;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/11/16.
 * SessionService：session的管理器，用来：
 * 获取session
 * 创建session
 * 定期更新session
 * 删除session
 * 保存session
 *
 * Session作为一个工具来存在，不是和框架任何部分耦合的
 */
@Service(init = "init")
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private ConcurrentHashMap<String,Session> sessionMap;

    private DataService dataService;

    public void init(){
        sessionMap = new ConcurrentHashMap<>();
    }

    public Session get(String sessionId){
        Session session = (Session) sessionMap.get(sessionId);
        if(session!=null) {
            // 更新session时间
            session.setLastUpdateTime(new Date());
        }
        return session;
    }
    public Session create(String url,String ip){
        Session session=new Session(url, createSessionIdPrefix(), ip,new Date());
        sessionMap.put(session.getSessionId(),session);
        return session;
    }

    // TODO 这个地方如何赋值？可以取final值，所以可以从配置文件中取：这些改到accountService中去
//    @Updatable(isAsynchronous = true,cycle = EngineConfigure.sessionUpdateCycle)
//    public void update(int interval){
//        final long currentTime=System.currentTimeMillis();
//        final List<String> expiredIds = new ArrayList<>();
//        // 先找出过期的session，然后更新
//        int expiredIdNum = 0;
//        for (Map.Entry<String,Long> entry :updateTime.entrySet()) {
//            if(currentTime - entry.getValue() >=survivalTime){
//                expiredIds.add(entry.getKey());
//            }
//            if(expiredIdNum++ > maxOnceRemoveSessionCount){
//                break;
//            }
//        }
//        for (String key :expiredIds) {
//            removeSession((Session) cacheService.get(key));
//        }
//    }
    public Session removeSession(String sessionId){
        Session session = sessionMap.get(sessionId);
        removeSession(session);
        return session;
    }
    public void removeSession(Session session){
        if(session == null){
            log.warn("session == null while remove session");
            return;
        }
        Account account = dataService.selectObject(Account.class,"id="+session.getAccountId());
        if(account != null){
            account.destroySession();
        }
        sessionMap.remove(session.getSessionId());
    }

    public ConcurrentHashMap<String, Session> getSessionMap() {
        return sessionMap;
    }

    private String createSessionIdPrefix(){
        return UUID.randomUUID().toString();
    }
}
