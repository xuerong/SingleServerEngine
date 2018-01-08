package com.mm.engine.sysBean.service;

import com.migong.entity.MiGongPassInfo;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.control.statistics.Statistics;
import com.mm.engine.framework.control.statistics.StatisticsData;
import com.mm.engine.framework.control.statistics.StatisticsStore;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.ServerInfo;
import com.mm.engine.framework.data.entity.account.*;
import com.mm.engine.framework.data.entity.session.ConnectionClose;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.entity.session.SessionService;
import com.mm.engine.framework.data.persistence.dao.DatabaseHelper;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.LocalizationMessage;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.IdService;
import com.mm.engine.framework.server.Server;
import com.mm.engine.framework.server.ServerType;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.util.Util;
import com.protocol.AccountOpcode;
import com.protocol.AccountPB;
import com.sys.SysPara;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by a on 2016/9/20.
 */
@Service(init = "init")
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    // account-session
    private ConcurrentHashMap<String,Session> sessionMap;
    // 系统启动的时候把所有的服务器加载到内存，然后，定期更新服务器列表，这样，新加的服务器就进来了
    private List<ServerInfo> serverInfos;
    private ServerInfo currentServer; // 当前需要分配的server,一直分配到该服务器，直到不行的再重新设置当前服务器

    public AccountSysService accountSysService;
    private DataService dataService;
    private IdService idService;

    public void init(){
        sessionMap = new ConcurrentHashMap<>();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                List<ServerInfo> serverInfos = dataService.selectList(ServerInfo.class,null);
                if(serverInfos == null || serverInfos.size() == 0){
                    throw new MMException("server info is empty");
                }
                serverInfos.sort(new Comparator<ServerInfo>() {
                    @Override
                    public int compare(ServerInfo o1, ServerInfo o2) {
                        return o2.getAccountCount() - o1.getAccountCount();
                    }
                });
                AccountService.this.serverInfos = serverInfos;

                if(!refreshCurrentServer()){
                    throw new MMException("server is too full to add people!!!");
                }
            }
        },0,5, TimeUnit.MINUTES);
    }

    public RetPacket login(int opcode, Object data, ChannelHandlerContext ctx,AttributeKey<String> sessionKey) throws Throwable{
        AccountPB.CSLogin csLoginMain = AccountPB.CSLogin.parseFrom((byte[])data);
        String accountId = csLoginMain.getAccountId();
        String remoteAddress = ctx.channel().remoteAddress().toString();
        String ip = remoteAddress;
        if(remoteAddress != null){
            ip = remoteAddress.split(":")[0].replace("/","");
        }
        LoginSegment loginSegment = accountSysService.login(accountId,csLoginMain.getUrl(),ip);
        Account account = loginSegment.getAccount();

        {
            // 一些对account的设置，并保存
        }
        Session session = loginSegment.getSession();
        session.setLocalization(csLoginMain.getLocalization());
        LocalizationMessage.setThreadLocalization(session.getLocalization());
        MessageSender messageSender = new NettyPBMessageSender(ctx.channel());
        session.setMessageSender(messageSender);
        final ChannelHandlerContext _ctx = ctx;
        session.setConnectionClose(new ConnectionClose() {
            @Override
            public void close() {
                AccountPB.SCBeTakePlace.Builder builder = AccountPB.SCBeTakePlace.newBuilder();
                messageSender.sendMessageSync(AccountOpcode.SCBeTakePlace,builder.build().toByteArray());
                _ctx.close();
            }
        });
        ctx.channel().attr(sessionKey).set(loginSegment.getSession().getSessionId());
        AccountPB.SCLogin.Builder builder = AccountPB.SCLogin.newBuilder();
        builder.setSessionId(loginSegment.getSession().getSessionId());
        builder.setServerTime(System.currentTimeMillis());
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
    @Tx
    @Request(opcode = AccountOpcode.CSGetLoginInfo)
    public RetPacket getLoginInfo(Object data, Session session) throws Throwable{
        AccountPB.CSGetLoginInfo loginInfo = AccountPB.CSGetLoginInfo.parseFrom((byte[])data);
        if(StringUtils.isEmpty(loginInfo.getDeviceId())){
            throw new ToClientException(LocalizationMessage.getText("deviceIdIsEmpty",loginInfo.getDeviceId()));
        }
        DeviceAccount deviceAccount = dataService.selectObject(DeviceAccount.class,"deviceId=?",loginInfo.getDeviceId());
        if(deviceAccount == null){ // 创建新的账号
            deviceAccount = new DeviceAccount();
            deviceAccount.setDeviceId(loginInfo.getDeviceId());
            deviceAccount.setAccountId(String.valueOf(idService.acquireLong(AccountService.class)));
            deviceAccount.setCreateTime(new Timestamp(System.currentTimeMillis()));
            ServerInfo serverInfo = serverForNewUser();
            deviceAccount.setIp(serverInfo.getIp());
            deviceAccount.setLastLoginTime(new Timestamp(System.currentTimeMillis()));
            deviceAccount.setPort(serverInfo.getPort());
            deviceAccount.setServerId(serverInfo.getId());
            dataService.insert(deviceAccount);
            serverInfo.setAccountCount(serverInfo.getAccountCount() + 1);
            dataService.update(serverInfo);
            log.info("new user register,device id = {},ip = {}",loginInfo.getDeviceId(),session.getIp());
        }
        AccountPB.SCGetLoginInfo.Builder builder = AccountPB.SCGetLoginInfo.newBuilder();
        builder.setServerId(deviceAccount.getServerId());
        builder.setPort(deviceAccount.getPort());
        builder.setIp(deviceAccount.getIp());
        builder.setAccountId(deviceAccount.getAccountId());
        log.info("new user login,device id = {},ip = {}",loginInfo.getDeviceId(),session.getIp());
        RetPacket retPacket = new RetPacketImpl(AccountOpcode.SCGetLoginInfo,false,builder.build().toByteArray());
        return retPacket;
    }
    // 为新玩家分配服务器，规则
    private ServerInfo serverForNewUser(){
        if(!refreshCurrentServer()){
            throw new ToClientException(LocalizationMessage.getText("severFull"));
        }
        return currentServer;
    }
    private synchronized boolean refreshCurrentServer(){ // 刷新当前分配server
        if(currentServer == null || currentServer.isFull()){
            ServerInfo leastServerInfo = serverInfos.get(0);
            for(ServerInfo serverInfo : serverInfos){
                if(!serverInfo.isFull()){
                    currentServer = serverInfo;
                    return true;
                }
                if(serverInfo.getAccountCount() < leastServerInfo.getAccountCount()){
                    leastServerInfo = serverInfo;
                }
            }
            // 能走到这里，说明服务器都满了
            currentServer = leastServerInfo;
            if(currentServer.isMax()){
                log.error("all server is too full!!!!!!!! can not register new user");
                return false;
            }
            log.error("all server is full!!!!!!!! must add new Server");
        }
        return true;
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

    /**
     * 统计
     * 1玩家注册（新注册）
     * 2玩家登陆（日活跃，留存(1,3,7,14,30)）
     * @return
     */
    List<String> heads = Arrays.asList("日期","玩家数","日活跃","新注册","1日留存","3日留存","7日留存","14日留存","30日留存");
    List<String> headKeys = Arrays.asList("date","userCount","dayLogin","new","liu1","liu3","liu7","liu14","liu30");
    @Statistics(id = "accountStatistics",name = "玩家统计")
    public StatisticsData accountStatistics(){
        StatisticsData ret = new StatisticsData();
        ret.setHeads(heads);

        List<List<String>> datas = new ArrayList<>();
        List<StatisticsStore> statisticsStores = dataService.selectList(StatisticsStore.class,"type=?","accountStatistics");
        for(StatisticsStore statisticsStore : statisticsStores){
            JSONObject jsonObject = JSONObject.fromObject(statisticsStore.getContent());
            List<String> list = new ArrayList<>();
//            System.out.println(jsonObject);
            for(String head : headKeys) {
//                System.out.println(head+","+jsonObject.get(head));
                list.add(jsonObject.get(head).toString());
            }
            datas.add(list);
        }
        ret.setDatas(datas);

        return ret;
    }
    /**
     * 每天统计一下数据，保存在数据库中
     * @param interval
     */
    @Updatable(cronExpression = "0 0 0 * * ?")
    public void accountStatistics(int interval){
        StatisticsStore statisticsStore = new StatisticsStore();
        statisticsStore.setId(idService.acquireLong(StatisticsStore.class));
        statisticsStore.setType("accountStatistics");

        JSONObject jsonObject = new JSONObject();
        // 这里的单位用s
        long oneDay = 24l*60*60;
        long staTime = Util.getBeginTimeToday()/1000 - oneDay;

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//
//        System.out.println(df.format(new Date(staTime)));//

        jsonObject.put("date",df.format(new Date(staTime*1000)));

        jsonObject.put("userCount",dataService.selectCount(Account.class,""));
        jsonObject.put("dayLogin", dataService.selectCountBySql("select count(*) from account where unix_timestamp(lastLoginTime) > ?",staTime));
        jsonObject.put("new",dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ?",staTime));

        long from = staTime - oneDay,to = staTime;
        long all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        long liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu1",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*3;
        to = staTime - oneDay*2;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu3",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*7;
        to = staTime - oneDay*6;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu7",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*14;
        to = staTime - oneDay*13;
        all = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where unix_timestamp(createTime) > ? and unix_timestamp(createTime)<? and unix_timestamp(lastLoginTime) >?",from,to,staTime);
        jsonObject.put("liu14",liu==0?0:liu*100/all + "%");

        from = staTime - oneDay*30;
        to = staTime - oneDay*29;
        all = dataService.selectCountBySql("select count(*) from account where createTime > ? and createTime<?",from,to);
        liu = dataService.selectCountBySql("select count(*) from account where createTime > ? and createTime<? and lastLoginTime >?",from,to,staTime);
        jsonObject.put("liu30",liu==0?0:liu*100/all + "%");


        statisticsStore.setContent(jsonObject.toString());
        dataService.insert(statisticsStore);
    }
}
