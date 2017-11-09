package com.migong;

import com.migong.entity.*;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.account.Account;
import com.mm.engine.framework.data.entity.account.LogoutEventData;
import com.mm.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.sysPara.SysParaService;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.sys.SysPara;
import com.table.MiGongPass;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

/**
 * 功能：
 * 单人的：根据等级等获取地图，时间到的失败推送，结束的时候发送走过的路径校验，并记录
 * 多人的：请求匹配（放入匹配队列），匹配完成创建房间并推送，操作（移动，道具），位置同步和校验（全缓存），到达终点请求和同步，结束推送同步，房间心跳
 *
 * 同步方案：
 * 1、前端每次操作时，告诉服务器：（位置，方向，速度）
 * 2、服务器启动60帧每秒的循环，在循环中转发每个前端的操作：（位置，方向，速度，帧数）
 * 3、前端接收到服务器转发的各个前端操作的指令，独立计算
 * 4、开启其它线程用于操作校验
 * 5、开始和结束的时候也要发
 *
 * 道具：加速，瞬移，提示一定路段，
 *
 *
 *
 * 后续vip可以从如下几个点做：速度，体力，道具
 * 数据设计：
 * 系统配数：每日精力，四个星级的精力，无尽版速度，天梯速度，匹配等待时长，无尽版和匹配版的时长
 * 配数：
 * 关卡：id,size,time,speed,bean1,bean5,bean10,star1,star2,star3,star4,energy,reward(预留奖励道具)
 * 天梯：title,ladderScore
 * vip：speed,energy,item,energyRecovery
 *
 * 数据库：
 * 玩家：userId,unlimitedPass,unlimitedStar,starCount(启动的时候校验一下),vip,ladderScore,energy      ：无尽版排行按照unlimitedPass-unlimitedStar排行
 * 推图（已经过的）：userId,passId,star,useTime,score：重新打的时候，存储条件：星级大，或者星级相等分数大
 *
 *
 */
@Service(init = "init")
public class MiGongService {
    private static final Logger log = LoggerFactory.getLogger(MiGongService.class);

    public static final int SPEED_DEFAULT = 10;

    private MiGongRank miGongRank;
    private DataService dataService;
    private SendMessageService sendMessageService;
    private SysParaService sysParaService;
    /**
     * 这个是玩家获取迷宫后缓存的该迷宫信息，在玩家过关的时候用来校验是否正常过关。同时在如下几种情况下要清除：
     * 1、玩家断开连接
     * 2、到点失败
     */
    ConcurrentHashMap<String,MiGongPassInfo> miGongPassInfoMap = new ConcurrentHashMap<>();
    /**
     * 匹配中的玩家，grade（段位）- （accountId-开始匹配时间）
     */
    ConcurrentHashMap<Integer,ConcurrentLinkedQueue<RoomUser>> matchingUsers = new ConcurrentHashMap<>();

    /**
     * 匹配时间超时的消息推送
     */
    ExecutorService matchOutTimeExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new MiGongThreadFactory("matchOutTime"));
    /**
     * 匹配成功的房间创建
     */
    ExecutorService roomCreateExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new MiGongThreadFactory("roomCreate"));

    /**
     * 玩家-房间
     */
    ConcurrentHashMap<String,MultiMiGongRoom> userRooms = new ConcurrentHashMap<>();
    /**
     * 玩家状态：有状态的时候放入，无状态的时候移除（掉线，退出房间等）
     * 这里只针对匹配，放置多个匹配
     */
    ConcurrentHashMap<String,UserState> userStates = new ConcurrentHashMap<>();


    private final Map<Integer,MiGongPass> passMap = new HashMap<>();
    public void init(){
        System.out.println("MiGongService init");
        for(MiGongPass miGongPass : MiGongPass.datas){
            passMap.put(miGongPass.getId(), miGongPass);
        }

    }
    @Request(opcode = MiGongOpcode.CSBaseInfo)
    public RetPacket getbaseInfo(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());
        MiGongPB.SCBaseInfo.Builder builder = MiGongPB.SCBaseInfo.newBuilder();
        builder.setEnergy(getEnergyByRefresh(userMiGong));
        for(Map.Entry<String,String> sysPara : SysPara.paras.entrySet()){
            MiGongPB.PBSysPara.Builder sysParaBuilder = MiGongPB.PBSysPara.newBuilder();
            sysParaBuilder.setKey(sysPara.getKey());
            sysParaBuilder.setValue(sysPara.getValue());
            builder.addSysParas(sysParaBuilder);
        }
        return new RetPacketImpl(MiGongOpcode.SCBaseInfo, builder.build().toByteArray());
    }

    /**
     * 获取玩家精力
     * @param userMiGong
     * @return 玩家精力
     */
    public int getEnergyByRefresh(UserMiGong userMiGong){
        if(!DateUtils.isSameDay(new Timestamp(userMiGong.getEnergyUpdateTime()),new Timestamp(System.currentTimeMillis()))){
            userMiGong.setEnergy(sysParaService.getInt(SysPara.energyPerDay));
            userMiGong.setEnergyUpdateTime(System.currentTimeMillis());
            dataService.update(userMiGong);
        }
        return userMiGong.getEnergy();
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongLevel)
    public RetPacket getMiGongLevel(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetMiGongLevel getMiGongLevel = MiGongPB.CSGetMiGongLevel.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        //
        MiGongPB.SCGetMiGongLevel.Builder builder = MiGongPB.SCGetMiGongLevel.newBuilder();
        List<UserPass> userPasses =  dataService.selectList(UserPass.class,"userId=?",session.getAccountId());
        if(userPasses != null) {
            for (UserPass userPass : userPasses) {
                builder.addStarInLevel(userPass.getStar());
            }
        }
        builder.setPassCount(MiGongPass.datas.length);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session) throws Throwable{
        System.out.println("do request getMap");
        MiGongPB.CSGetMiGongMap getMiGongMap = MiGongPB.CSGetMiGongMap.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        // 当前等级和关卡
        checkLevelAndPass(getMiGongMap.getPass(),userMiGong);
        if(miGongPassInfoMap.containsKey(session.getAccountId())){
            miGongPassInfoMap.remove(session.getAccountId());
            log.error("do CSGetMiGongMap but miGongPassInfoMap has data,accountId = {}",session.getAccountId());
        }
        // todo 消耗精力，还有无线模式
        //
        MiGongPass miGongPass = passMap.get(getMiGongMap.getPass());
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setPass(getMiGongMap.getPass());
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo,miGongPass.getSize(),miGongPass.getTime(),miGongPass.getSpeed()); // 这个方法后面直接去掉
        MiGongPB.SCGetMiGongMap.Builder builder = MiGongPB.SCGetMiGongMap.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//地图


        List<Integer> integers = new ArrayList<>(miGongPassInfo.getSize()*miGongPassInfo.getSize());
        for(byte[] aa : miGongPassInfo.getCreateMap().getMap()){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);
        builder.setTime(miGongPassInfo.getTime());
        builder.setSpeed(miGongPassInfo.getSpeed());
        builder.setStart(miGongPassInfo.getStart().toInt(miGongPassInfo.getSize()));
        for(Bean bean : miGongPassInfo.getBeans()){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setPos(bean.toInt(miGongPassInfo.getSize()));
            beanBuilder.setScore(bean.getScore());
            builder.addBeans(beanBuilder);
        }
        builder.setEnergy(getEnergyByRefresh(userMiGong)); // todo 这个是剩余精力
        builder.setPass(miGongPass.getId());
        builder.setStar1(miGongPass.getStar1());
        builder.setStar2(miGongPass.getStar2());
        builder.setStar3(miGongPass.getStar3());
        builder.setStar4(miGongPass.getStar4());

        System.out.println("miGongPassInfo.getEnd():"+miGongPassInfo.getEnd());
        builder.setEnd(miGongPassInfo.getEnd().toInt(miGongPassInfo.getSize()));
        miGongPassInfoMap.put(session.getAccountId(),miGongPassInfo);
        byte[] sendData = builder.build().toByteArray();
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongMap, sendData);
    }
    private UserMiGong get(String userId){
        UserMiGong userMiGong = dataService.selectObject(UserMiGong.class,"userId=?",userId);
        if(userMiGong == null){
            userMiGong = new UserMiGong();
            userMiGong.setUserId(userId);
            dataService.insert(userMiGong);
        }
        return userMiGong;
    }
    @Tx()
    @Request(opcode = MiGongOpcode.CSPassFinish)
    public RetPacket passFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSPassFinish passFinish = MiGongPB.CSPassFinish.parseFrom((byte[])clientData);
        // 校验关卡
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.remove(session.getAccountId());
        if(miGongPassInfo == null || miGongPassInfo.getPass() != passFinish.getPass()){
            throw new ToClientException("Invalid params");
        }
        UserMiGong userMiGong = get(session.getAccountId());
        checkLevelAndPass(passFinish.getPass(),userMiGong);
        boolean isSuccess = false;
        if(passFinish.getSuccess() > 0){
            // 校验
            List<Integer> routeList = passFinish.getRouteList();
            isSuccess = miGongPassInfo.getCreateMap().checkRouteWithoutSkill(routeList);
        }
        //
        if(isSuccess){
            // 根据路径和配置获取星级，，是否插入？是否更新？，更新userMiGong星级和关卡，
            MiGongPass miGongPass = passMap.get(miGongPassInfo.getPass());
            int allScore = calScore(passFinish.getRouteList(),miGongPassInfo);
            int star = calStar(allScore,miGongPass);
            if(star > 0){
                if(miGongPassInfo.getPass() > userMiGong.getPass()){
                    UserPass userPass = new UserPass();
                    userPass.setUserId(session.getAccountId());
                    userPass.setPassId(miGongPassInfo.getPass());
                    userPass.setStar(star);
                    userPass.setScore(allScore);
                    userPass.setUseTime((int)(System.currentTimeMillis() - miGongPassInfo.getStartTime().getTime())/1000);
                    dataService.insert(userPass);
                    userMiGong.setPass(passFinish.getPass());
                    userMiGong.setStarCount(userMiGong.getStarCount() + star);
                    dataService.update(userMiGong);
                }else{
                    UserPass userPass = dataService.selectObject(UserPass.class,"userId=? and passId=?",session.getAccountId(),miGongPassInfo.getPass());
                    if(star > userPass.getStar()){
                        userMiGong.setStarCount(userMiGong.getStarCount() + (star - userPass.getStar()));
                        dataService.update(userMiGong);
                    }
                    if(star > userPass.getStar() || (star == userPass.getStar() && allScore > userPass.getScore())){
                        userPass.setScore(allScore);
                        userPass.setStar(star);
                        dataService.update(userPass);
                    }
                }
            }else{
                isSuccess = false;
            }

        }
        MiGongPB.SCPassFinish.Builder builder = MiGongPB.SCPassFinish.newBuilder();
        builder.setOpenLevel(1);
        builder.setOpenPass(userMiGong.getPass());
        builder.setSuccess(isSuccess?1:0);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }
    private int calScore(List<Integer> routes,MiGongPassInfo miGongPassInfo){
        Set<Integer> posForBean = new HashSet<>();
        for(Integer pos : routes){
            posForBean.add(pos);
        }
        int size = miGongPassInfo.getSize();
        int allScore = 0;
        for(Bean bean : miGongPassInfo.getBeans()){
            if(posForBean.contains(bean.getX() * size + bean.getY())) {
                allScore += bean.getScore();
            }
        }
        return allScore;
    }
    private int calStar(int score,MiGongPass miGongPass){
        return calStar(score,miGongPass.getStar1(),miGongPass.getStar2(),miGongPass.getStar3(),miGongPass.getStar4());
    }
    private int calStar(int score,int star1,int star2,int star3,int star4){
        if(score < star1){
            return 0;
        }else if(score < star2){
            return 1;
        }else if(score < star3){
            return 2;
        }else if(score < star4){
            return 3;
        }
        return 4;
    }

    @Request(opcode = MiGongOpcode.CSUnlimitedInfo)
    public RetPacket unlimitedInfo(Object clientData, Session session) throws Throwable{
        // todo 判断无尽模式是否打开，
        UserMiGong userMiGong = get(session.getAccountId());
        MiGongPB.SCUnlimitedInfo.Builder builder = MiGongPB.SCUnlimitedInfo.newBuilder();
        builder.setPass(userMiGong.getUnlimitedPass());
        builder.setStar(userMiGong.getUnlimitedStar());
        builder.setRank(miGongRank.getRank(userMiGong.getUserId()));// 排行系统

        List<UserMiGong> rank = miGongRank.getFront();
        int i = 1;
        for(UserMiGong rankMiGong : rank){
            MiGongPB.PBUnlimitedRankInfo.Builder info = MiGongPB.PBUnlimitedRankInfo.newBuilder();
            info.setPass(rankMiGong.getUnlimitedPass());
            info.setStar(rankMiGong.getUnlimitedStar());
            info.setRank(i++);
            info.setUserId(rankMiGong.getUserId());
            info.setUserName(dataService.selectObject(Account.class,"id=?",rankMiGong.getUserId()).getName());
            builder.addUnlimitedRankInfo(info);
        }
        return new RetPacketImpl(MiGongOpcode.SCUnlimitedInfo, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSUnlimitedGo)
    public RetPacket unlimitedGo(Object clientData, Session session) throws Throwable{
        // todo 判断无尽模式是否打开，
        UserMiGong userMiGong = get(session.getAccountId());
        // todo 消耗精力
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo,10,400,sysParaService.getInt(SysPara.unlimitedSpeed));
        MiGongPB.SCUnlimitedGo.Builder builder = MiGongPB.SCUnlimitedGo.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//地图

        List<Integer> integers = new ArrayList<>(miGongPassInfo.getSize()*miGongPassInfo.getSize());
        for(byte[] aa : miGongPassInfo.getCreateMap().getMap()){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);
        builder.setSpeed(miGongPassInfo.getSpeed());
        builder.setEnd(miGongPassInfo.getEnd().toInt(miGongPassInfo.getSize()));
        builder.setStart(miGongPassInfo.getStart().toInt(miGongPassInfo.getSize()));
        builder.setTime(miGongPassInfo.getTime());
        for(Bean bean : miGongPassInfo.getBeans()){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setScore(bean.getScore());
            beanBuilder.setPos(bean.toInt(miGongPassInfo.getSize()));
            builder.addBeans(beanBuilder);
        }
        builder.setStar1(10);// todo 4Stars
        builder.setStar2(20);
        builder.setStar3(30);
        builder.setStar4(40);
        builder.setPass(userMiGong.getUnlimitedPass() + 1);
        builder.setEnergy(getEnergyByRefresh(userMiGong));// todo 精力，系统参数

        miGongPassInfoMap.put(session.getAccountId(),miGongPassInfo);

        return new RetPacketImpl(MiGongOpcode.SCUnlimitedGo, builder.build().toByteArray());
    }

    @Request(opcode = MiGongOpcode.CSUnlimitedFinish)
    public RetPacket unlimitedFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSUnlimitedFinish unlimitedFinish = MiGongPB.CSUnlimitedFinish.parseFrom((byte[])clientData);
        // 校验关卡
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.remove(session.getAccountId());
        if(miGongPassInfo == null){
            throw new ToClientException("Invalid params");
        }
        UserMiGong userMiGong = get(session.getAccountId());

        boolean isSuccess = false;
        if(unlimitedFinish.getSuccess() > 0){
            // 校验
            List<Integer> routeList = unlimitedFinish.getRouteList();
            isSuccess = miGongPassInfo.getCreateMap().checkRouteWithoutSkill(routeList);
        }
        //
        if(isSuccess){
            int allScore = calScore(unlimitedFinish.getRouteList(),miGongPassInfo);
            int star = calStar(allScore,10,20,30,40);
            if(star > 0){
                userMiGong.setUnlimitedPass(userMiGong.getUnlimitedPass()+1);
                userMiGong.setUnlimitedStar(userMiGong.getUnlimitedStar() + star);
                dataService.update(userMiGong);
                miGongRank.putUnlimited(userMiGong);
            }else{
                isSuccess = false;
            }
        }
        MiGongPB.SCUnlimitedFinish.Builder builder = MiGongPB.SCUnlimitedFinish.newBuilder();
        builder.setOpenPass(userMiGong.getUnlimitedPass());
        builder.setSuccess(isSuccess?1:0);
        return new RetPacketImpl(MiGongOpcode.SCUnlimitedFinish, builder.build().toByteArray());
    }

    /**
     * 每分钟检查一次，去掉过期的MiGongPassInfo
     * 注意：要过期一段时间，否则，有些是延时20s
     * @param interval
     */
    @Updatable(isAsynchronous = true,cycle = 60000)
    public void checkMiGongPassInfo(int interval){
        long currentTime = System.currentTimeMillis();
        for(Map.Entry<String,MiGongPassInfo> entry : miGongPassInfoMap.entrySet()){
            MiGongPassInfo miGongPassInfo = entry.getValue();
            if((miGongPassInfo.getTime()+20)*1000 < currentTime - miGongPassInfo.getStartTime().getTime()){
                miGongPassInfoMap.remove(entry.getKey());
                log.warn("mi gong info remove by checkMiGongPassInfo,uid = {}",entry.getKey());
            }
        }
    }
    /**
     * 检车对应关卡是否可以进入
     * @param pass
     * @param userMiGong
     * @throws Throwable
     */
    private void checkLevelAndPass(int pass,UserMiGong userMiGong) throws Throwable{
        MiGongPass miGongPass = passMap.get(pass);
        if(miGongPass == null || pass < 1){
            throw new ToClientException("Invalid params");
        }
        if(pass - userMiGong.getPass() > 1){
            throw new ToClientException("Invalid params");
        }
    }
    /**
     * 根据难度，获取迷宫的大小和开门数，额，后面做成配置也好
     * 大小和开门数相对于难度的系数
     * 难度与时间的系数
     * 速度
     * 1-15,2-20,
     * @param miGongPassInfo
     * @return size,door,time(s),speed
     */
    private MiGongPassInfo miGongParamsByDifficulty(MiGongPassInfo miGongPassInfo,int size,int time,int speed){
        size+=1;
        int door = 0;
        miGongPassInfo.setSize(size);
        miGongPassInfo.setDoor(door);
        Element startElement = new Element(1,1); // todo 开始和结束可以考虑改成随机
        miGongPassInfo.setStart(startElement);
        Element endElement = new Element(size-1,size-1);
        miGongPassInfo.setEnd(endElement);
        CreateMap createMap = new CreateMap(size-1,size-1,startElement,endElement);
        miGongPassInfo.setCreateMap(createMap);
        byte[][] map=createMap.getMap();											//获取地图数组
        // 拆墙
        for(int i=0;i<door;i++){
            int x=(int)(Math.random()*(size-2))+1;					//1---tr-1
            int y=(int)(Math.random()*(size-2))+1;					//1---td-1
            map[x][y]=0;
        }
        miGongPassInfo.setTime(time);
        miGongPassInfo.setSpeed(speed);

        //生成豆子的位置
        Bean[] beans = createBeans(size); // todo 创建豆子pass和unlimited要根据配置
        miGongPassInfo.setBeans(beans);

        return miGongPassInfo;
    }

    /**
     * 生成豆子的逻辑，目前定为，中间一个10分的，四边四个五分的，40个1分的
     * @return
     */
    private Bean[] createBeans(int size){
        //
        int bean10Count = 1;
        int bean5Count = 4;
        int bean1Count = 40;
        Bean[] ret = new Bean[bean10Count + bean5Count + bean1Count];
        ret[0] = new Bean(size/2,size/2,10);

        ret[1] = new Bean(size/2,1,5);
        ret[2] = new Bean(size/2,size-1,5);
        ret[3] = new Bean(1,size/2,5);
        ret[4] = new Bean(size-1,size/2,5);

        Random random = new Random(System.currentTimeMillis());
        Set<Integer> hasCreatePos = new HashSet<>(ret.length);
        hasCreatePos.add((ret[0].getX()-1)*(size - 1) + (ret[0].getY()-1));
        hasCreatePos.add((ret[1].getX()-1)*(size - 1) + (ret[1].getY()-1));
        hasCreatePos.add((ret[2].getX()-1)*(size - 1) + (ret[2].getY()-1));
        hasCreatePos.add((ret[3].getX()-1)*(size - 1) + (ret[3].getY()-1));
        hasCreatePos.add((ret[4].getX()-1)*(size - 1) + (ret[4].getY()-1));

        for(int i =0;i<bean1Count;i++){
            int posInt = random.nextInt((size - 1)*(size-1));
            if(hasCreatePos.contains(posInt)){
                i--;
            }else{
                ret[i+5] = new Bean(posInt/(size-1) + 1,posInt%(size-1)+1,1);
                hasCreatePos.add(posInt);
            }
        }
        return ret;
    }

    @Request(opcode = MiGongOpcode.CSSendWalkingRoute)
    public RetPacket walkingRoute(Object clientData, Session session) throws Throwable{
        MiGongPB.CSSendWalkingRoute csSendWalkingRoute = MiGongPB.CSSendWalkingRoute.parseFrom((byte[])clientData);
        List<Integer> list = csSendWalkingRoute.getRouteList();

        // TODO 校验

        MiGongPB.SCSendWalkingRoute.Builder builder = MiGongPB.SCSendWalkingRoute.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCSendWalkingRoute, builder.build().toByteArray());
    }

    @EventListener(event = SysConstantDefine.Event_AccountLogout)
    public void onAccountLogout(EventData data){
        // 清理缓存的miGongPassInfoMap   清理信息
        final LogoutEventData logoutEventData = (LogoutEventData)data.getData();
        miGongPassInfoMap.remove(logoutEventData.getSession().getAccountId());
        // todo 从Matching的queue中移除，如果是房间最后一个，移除房间
        UserState userState = userStates.remove(logoutEventData.getSession().getAccountId());
        if(userState != null){
            // 不用移除queue，那个session已经失效了
        }
    }
    /////////////////////////////////////////////////////////////联网对战

    /**
     *匹配对战 ：把玩家放入匹配队列，查看队列大小，大于匹配数则开新线程开房间，
     */
    @Request(opcode = MiGongOpcode.CSMatching)
    public RetPacket matching(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());
        // todo 该玩家没有在排队也没有在房间
        UserState userState = userStates.get(session.getAccountId());
        if(userState == null || userState == UserState.None){
            userStates.put(session.getAccountId(),UserState.Matching);
        }else{
            throw new ToClientException("your state is "+userState.getDescribe()+",can not march");
        }
        ConcurrentLinkedQueue<RoomUser> queue = getMatchingQueue(scoreToGrade(userMiGong.getLadderScore()));
        queue.offer(new RoomUser(session,System.currentTimeMillis()));
        MiGongPB.SCMatching.Builder builder = MiGongPB.SCMatching.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCMatching, builder.build().toByteArray());
    }

    /**
     * 玩家移动
     */
    @Request(opcode = MiGongOpcode.CSMove)
    public RetPacket move(Object clientData, Session session) throws Throwable{
        MiGongPB.CSMove move = MiGongPB.CSMove.parseFrom((byte[])clientData);
        int dir = move.getDir();
        int speed = move.getSpeed();
        // 操作
        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException("room is not exist");
        }
        room.userMove(session.getAccountId(),move.getPosX(),move.getPosY(),dir,speed);

        MiGongPB.SCMove.Builder builder = MiGongPB.SCMove.newBuilder();
        return new RetPacketImpl(MiGongOpcode.CSMove, builder.build().toByteArray());
    }
    /**
     * 玩家吃豆
     */
    @Request(opcode = MiGongOpcode.CSEatBean)
    public RetPacket eatBean(Object clientData, Session session) throws Throwable{

        MiGongPB.CSEatBean eatBean = MiGongPB.CSEatBean.parseFrom((byte[])clientData);
        int pos = eatBean.getBeanPos();

        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException("room is not exist");
        }
        room.eatBean(session.getAccountId(),pos);
//        RoomUser roomUser = room.getRoomUser(session.getAccountId());
        // todo 开启一个新线程进行校验操作正误
//        roomUser.addBean(pos);

        MiGongPB.SCEatBean.Builder builder = MiGongPB.SCEatBean.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCEatBean, builder.build().toByteArray());
    }

    /**
     * 玩家抵达终点，要判断游戏结束
     */
    @Request(opcode = MiGongOpcode.CSArrived)
    public RetPacket arrived(Object clientData, Session session) throws Throwable{
        MiGongPB.CSArrived arrived = MiGongPB.CSArrived.parseFrom((byte[])clientData);
        int pos = arrived.getPos();
        // todo 校验pos是否正确，并校验玩家情况
        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException("room is not exist");
        }
        room.userArrived(session.getAccountId());

        MiGongPB.SCArrived.Builder builder = MiGongPB.SCArrived.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCEatBean, builder.build().toByteArray());
    }

    /**
     * 每秒执行一次匹配，如果小于匹配数量，检查等待时间
     * @param interval
     */
    @Updatable(isAsynchronous = true,cycle = 1000)
    public void doMatching(int interval){
        if(matchingUsers.size() > 1000){
            log.warn("grade count is too much! size = {}",matchingUsers.size());
        }
        long currentTime = System.currentTimeMillis();
        for(Map.Entry<Integer,ConcurrentLinkedQueue<RoomUser>> entry : matchingUsers.entrySet()){
            ConcurrentLinkedQueue<RoomUser> queue = entry.getValue();
            List<RoomUser> roomUserList = new ArrayList<>();
            while (!queue.isEmpty()){
                RoomUser roomUser = queue.poll();
                if(currentTime - roomUser.getBeginTime() > MiGongRoom.MAX_WAIT_TIME){
                    matchOutTimeExecutor.execute(new SendMatchFailRunnable(roomUser));
                }else if(!roomUser.getSession().isAvailable()){
                    log.info("session is not available , user maybe logout");
                }else{
                    // 加入匹配
                    roomUserList.add(roomUser);
                    if(roomUserList.size() >= MiGongRoom.USER_COUNT){
                        // 创建房间
                        roomCreateExecutor.execute(new CreateRoomRunnable(entry.getKey(), roomUserList));
                        // 创建新的容器
                        roomUserList = new ArrayList<>();
                    }
                }
            }
            if(roomUserList.size() > 0){
                for(RoomUser roomUser : roomUserList){
                    queue.offer(roomUser); // 放回去
                }
            }
        }
    }
    class SendMatchFailRunnable implements Runnable{
        RoomUser roomUser;
        protected SendMatchFailRunnable(RoomUser roomUser){
            this.roomUser = roomUser;
        }
        @Override
        public void run() {
            MiGongPB.SCMatchingFail.Builder builder = MiGongPB.SCMatchingFail.newBuilder();
            sendMessageService.sendMessage(roomUser.getSession().getAccountId(), MiGongOpcode.SCMatchingFail,builder.build().toByteArray());
        }
    }
    class CreateRoomRunnable implements Runnable{
        private int grade;
        private List<RoomUser> roomUserList;
        protected  CreateRoomRunnable(int grade,List<RoomUser> roomUserList){
            this.grade = grade;
            this.roomUserList = roomUserList;
        }
        @Override
        public void run() {
            // 创建房间
            createRoom(grade, roomUserList);
        }
    }
    /**
     * 根据段位创建迷宫房间
     * @return
     */
    private void createRoom(int grade,final List<RoomUser> roomUserList){
        int size = grade*2+15+1;
        int door = 0;
        Element startElement = new Element(1,1); // 这个在这里没有用
        Element endElement = new Element(size-1,size-1);
        //
        CreateMap createMap = new CreateMap(size-1,size-1,startElement,endElement);

        MultiMiGongRoom multiMiGongRoom = new MultiMiGongRoom(createMap, size,roomUserList,this);

//        List<MiGongPB.PBOtherInfo> otherInfoList = new ArrayList<>();
//        for(RoomUser roomUser : roomUserList){
//            MiGongPB.PBOtherInfo.Builder ob = MiGongPB.PBOtherInfo.newBuilder();
//            ob.setStart(startElement.toInt(size));
//            ob.setEnd(endElement.toInt(size));
//            ob.setUserId(roomUser.getSession().getAccountId());
//            Account account = dataService.selectObject(Account.class,"id=?", roomUser.getSession().getAccountId());
//            ob.setUserName(account.getName());
//            otherInfoList.add(ob.build());
//        }
        MiGongPB.SCMatchingSuccess.Builder builder = MiGongPB.SCMatchingSuccess.newBuilder();

        for(byte[] aa : createMap.getMap()){
            for(byte b : aa){
                builder.addMap((int)b);
            }
        }
        // 豆子
        Bean[] beans = createBeans(size);
        Map<Integer,Bean> beanMap = new HashMap<>();
        for(Bean bean : beans){
            beanMap.put(bean.toInt(size),bean);
        }
        multiMiGongRoom.setBeans(beanMap);
        for(Bean bean : beans){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setPos(bean.toInt(size));
            beanBuilder.setScore(bean.getScore());
            builder.addBeans(beanBuilder);
        }
        builder.setTime(MiGongRoom.ROOM_MAX_TIME);
        builder.setSpeed(SPEED_DEFAULT); // todo 速度
        int index = 0;
        for(RoomUser roomUser : roomUserList){
            userRooms.put(roomUser.getSession().getAccountId(),multiMiGongRoom);
            //
            userStates.put(roomUser.getSession().getAccountId(),UserState.Playing);
            // 推送

            List<MiGongPB.PBOtherInfo> otherInfoList = new ArrayList<>();
            for(RoomUser roomUser2 : roomUserList){
                if(roomUser2 == roomUser){
                    builder.setStart(roomUser.getIn().toInt(size));
                    builder.setEnd(roomUser.getOut().toInt(size));
                    continue;
                }
                MiGongPB.PBOtherInfo.Builder ob = MiGongPB.PBOtherInfo.newBuilder();
                ob.setStart(roomUser2.getIn().toInt(size));
                ob.setEnd(roomUser2.getOut().toInt(size));
                ob.setUserId(roomUser2.getSession().getAccountId());
                Account account = dataService.selectObject(Account.class,"id=?", roomUser2.getSession().getAccountId());
                ob.setUserName(account.getName());
                otherInfoList.add(ob.build());
            }

//            MiGongPB.PBOtherInfo otherInfo = otherInfoList.remove(index++);
            builder.clearOtherInfos();
            builder.addAllOtherInfos(otherInfoList);
//            otherInfoList.add(otherInfo);
            System.out.println("send matchingsuccess");
            roomUser.getSession().getMessageSender().sendMessage(MiGongOpcode.SCMatchingSuccess,builder.build().toByteArray());
        }
        multiMiGongRoom.start();
    }

    // 房间结束的时候回回调这个函数，来清除房间信息
    public void multiRoomOver(MultiMiGongRoom room){
        for(RoomUser roomUser : room.getRoomUsers().values()){
            userRooms.remove(roomUser.getSession().getAccountId());
            userStates.remove(roomUser.getSession().getAccountId());
        }
    }

    private ConcurrentLinkedQueue<RoomUser> getMatchingQueue(int grade){
        ConcurrentLinkedQueue<RoomUser> queue = matchingUsers.get(grade);
        if(queue == null){
            ConcurrentLinkedQueue<RoomUser> _queue = new ConcurrentLinkedQueue<RoomUser>();
            matchingUsers.putIfAbsent(grade,_queue);
            queue = matchingUsers.get(grade);
        }
        return queue;
    }
    // 分数转段位，首先段位不能太多
    private int scoreToGrade(int score){
        return score/10;
    }

    enum UserState{
        None("idle"),
        Matching("matching"),
        Playing("in room");

        private final String describe;
        UserState(String describe){
            this.describe = describe;
        }
        public String getDescribe(){
            return describe;
        }
    }
}





































