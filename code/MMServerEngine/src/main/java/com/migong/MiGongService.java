package com.migong;

import com.migong.entity.*;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.control.room.Room;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.account.Account;
import com.mm.engine.framework.data.entity.account.LogoutEventData;
import com.mm.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.MiGongLevel;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;
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
 * public static final int CSGetMiGongLevel = 12001;
 public static final int SCGetMiGongLevel = 12002;
 public static final int CSGetMiGongMap = 12003;
 public static final int SCGetMiGongMap = 12004;
 public static final int CSPassFinish = 12005;
 public static final int SCPassFinish = 12006;
 public static final int CSUseItem = 12007;
 public static final int SCUseItem = 12008;
 public static final int CSMatching = 12009;
 public static final int SCMatching = 12010;
 public static final int SCMatchingSuccess = 12011;
 public static final int PBOtherInfo = 12012;
 public static final int SCMatchingFail = 12013;
 public static final int SCBegin = 12014;
 public static final int CSMove = 12015;
 public static final int SCMove = 12016;
 public static final int SCUserMove = 12017;
 public static final int CSArrived = 12018;
 public static final int SCArrived = 12019;
 public static final int SCUserArrived = 12020;
 public static final int SCGameOver = 12021;
 public static final int PBGameOverUserInfo = 12022;
 public static final int CSRoomHeart = 12023;
 public static final int SCRoomHeart = 12024;
 public static final int CSSendWalkingRoute = 12025;
 public static final int SCSendWalkingRoute = 12026;
 public static final int CSCommon = 12027;
 public static final int SCCommon = 12028;
 */
@Service(init = "init")
public class MiGongService {
    private static final Logger log = LoggerFactory.getLogger(MiGongService.class);

    public static final int SPEED_DEFAULT = 10;

    private MiGongRank miGongRank;
    private DataService dataService;
    private SendMessageService sendMessageService;
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
     * 房间id，房间对象
     */


    private final Map<Integer,MiGongLevel> levelMap = new HashMap<>();
    public void init(){
        System.out.println("MiGongService init");
        for(MiGongLevel miGongLevel : MiGongLevel.datas){
            levelMap.put(miGongLevel.getLevel(),miGongLevel);
        }

    }
    @Request(opcode = MiGongOpcode.CSGetMiGongLevel)
    public RetPacket getMiGongLevel(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetMiGongLevel getMiGongLevel = MiGongPB.CSGetMiGongLevel.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        //
        MiGongPB.SCGetMiGongLevel.Builder builder = MiGongPB.SCGetMiGongLevel.newBuilder();
        builder.setOpenLevel(userMiGong.getLevel());
        builder.setOpenPass(userMiGong.getPass());
        for(MiGongLevel miGongLevel : MiGongLevel.datas){
            builder.addPassCountInLevel(miGongLevel.getPass());
        }
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session) throws Throwable{
        System.out.println("do request getMap");
        MiGongPB.CSGetMiGongMap getMiGongMap = MiGongPB.CSGetMiGongMap.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        // 当前等级和关卡
        checkLevelAndPass(getMiGongMap.getLevel(),getMiGongMap.getPass(),userMiGong);
        if(miGongPassInfoMap.containsKey(session.getAccountId())){
            miGongPassInfoMap.remove(session.getAccountId());
            log.error("do CSGetMiGongMap but miGongPassInfoMap has data,accountId = {}",session.getAccountId());
        }
        //
        MiGongLevel miGongLevel = levelMap.get(getMiGongMap.getLevel());
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setLevel(getMiGongMap.getLevel());
        miGongPassInfo.setPass(getMiGongMap.getPass());
        miGongPassInfo.setDifficulty(miGongLevel.getDifficulty());
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo);
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
        builder.setTarget(miGongPassInfo.getTarget());

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

    @Request(opcode = MiGongOpcode.CSPassFinish)
    public RetPacket passFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSPassFinish passFinish = MiGongPB.CSPassFinish.parseFrom((byte[])clientData);
        // 校验关卡
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.remove(session.getAccountId());
        if(miGongPassInfo == null || miGongPassInfo.getLevel() != passFinish.getLevel() || miGongPassInfo.getPass() != passFinish.getPass()){
            throw new ToClientException("Invalid params");
        }
        UserMiGong userMiGong = get(session.getAccountId());
        checkLevelAndPass(passFinish.getLevel(),passFinish.getPass(),userMiGong);
        boolean isSuccess = false;
        if(passFinish.getSuccess() > 0){
            // 校验
            List<Integer> routeList = passFinish.getRouteList();
            isSuccess = miGongPassInfo.getCreateMap().checkRouteWithoutSkill(routeList);
        }
        //
        if(isSuccess){
            if(passFinish.getLevel() > userMiGong.getLevel() || passFinish.getPass() > userMiGong.getPass()){
                userMiGong.setLevel(passFinish.getLevel());
                userMiGong.setPass(passFinish.getPass());
                dataService.update(userMiGong);
            }
        }
        MiGongPB.SCPassFinish.Builder builder = MiGongPB.SCPassFinish.newBuilder();
        builder.setOpenLevel(userMiGong.getLevel());
        builder.setOpenPass(userMiGong.getPass());
        builder.setSuccess(isSuccess?1:0);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }

    @Request(opcode = MiGongOpcode.CSUnlimitedInfo)
    public RetPacket unlimitedInfo(Object clientData, Session session) throws Throwable{
        // todo 判断无尽模式是否打开，
        UserMiGong userMiGong = get(session.getAccountId());
        MiGongPB.SCUnlimitedInfo.Builder builder = MiGongPB.SCUnlimitedInfo.newBuilder();
        builder.setPass(userMiGong.getPassUnlimited());
        builder.setRank(miGongRank.getRank(userMiGong.getUserId()));// 排行系统

        List<UserMiGong> rank = miGongRank.getFront();
        int i = 1;
        for(UserMiGong rankMiGong : rank){
            MiGongPB.PBUnlimitedRankInfo.Builder info = MiGongPB.PBUnlimitedRankInfo.newBuilder();
            info.setPass(rankMiGong.getPassUnlimited());
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

        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setDifficulty(1); // 无尽模式设置难度
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo);
        MiGongPB.SCUnlimitedGo.Builder builder = MiGongPB.SCUnlimitedGo.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//地图

        List<Integer> integers = new ArrayList<>(miGongPassInfo.getSize()*miGongPassInfo.getSize());
        for(byte[] aa : miGongPassInfo.getCreateMap().getMap()){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);
        builder.setTarget(miGongPassInfo.getTarget());
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
            userMiGong.setPassUnlimited(userMiGong.getPassUnlimited()+1);
            dataService.update(userMiGong);
            miGongRank.putUnlimited(userMiGong);
        }
        MiGongPB.SCUnlimitedFinish.Builder builder = MiGongPB.SCUnlimitedFinish.newBuilder();
        builder.setOpenPass(userMiGong.getPassUnlimited());
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
     * @param level
     * @param pass
     * @param userMiGong
     * @throws Throwable
     */
    private void checkLevelAndPass(int level,int pass,UserMiGong userMiGong) throws Throwable{
        MiGongLevel miGongLevel = levelMap.get(level);
        if(miGongLevel == null || miGongLevel.getPass() < pass || pass < 1){
            throw new ToClientException("Invalid params");
        }
        if(level >= userMiGong.getLevel()){
            if(level == userMiGong.getLevel() && pass > userMiGong.getPass()+1){
                throw new ToClientException("Invalid params");
            }
            if(level > userMiGong.getLevel()){
                if(level != userMiGong.getLevel() +1 || pass != 1 ||
                        (userMiGong.getLevel() >0 && userMiGong.getPass() < levelMap.get(userMiGong.getLevel()).getPass())){
                    throw new ToClientException("Invalid params");
                }
            }
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
    private MiGongPassInfo miGongParamsByDifficulty(MiGongPassInfo miGongPassInfo){
        int difficulty = miGongPassInfo.getDifficulty();
        int size = difficulty*5+15+1;
        int door = 0;
        miGongPassInfo.setSize(size);
        miGongPassInfo.setDoor(door);
        Element startElement = new Element(1,1);
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
        miGongPassInfo.setTime(difficulty*300);
        miGongPassInfo.setSpeed(SPEED_DEFAULT);

        //生成豆子的位置
        Bean[] beans = createBeans(size);
        miGongPassInfo.setBeans(beans);
        miGongPassInfo.setTarget(beans.length - 30 + difficulty); // todo 这个难度算法后面要改

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
        for(int i =0;i<bean1Count;i++){
            int posInt = random.nextInt((size - 1)*(size-1));
            ret[i+5] = new Bean(posInt/(size-1) + 1,posInt%(size-1)+1,1);
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
    }
    /////////////////////////////////////////////////////////////联网对战

    /**
     *匹配对战 ：把玩家放入匹配队列，查看队列大小，大于匹配数则开新线程开房间，
     */
    @Request(opcode = MiGongOpcode.CSMatching)
    public RetPacket matching(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());
        // todo 该玩家没有在排队也没有在房间
        ConcurrentLinkedQueue<RoomUser> queue = getMatchingQueue(scoreToGrade(userMiGong.getScore()));
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
        if(room.isOver()){
            userRooms.remove(room);
        }

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
}





































