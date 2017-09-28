package com.migong;

import com.migong.entity.MiGongPassInfo;
import com.migong.entity.UserMiGong;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.EntityCreator;
import com.mm.engine.framework.data.entity.account.LogoutEventData;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.MiGongLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private DataService dataService;
    /**
     * 这个是玩家获取迷宫后缓存的该迷宫信息，在玩家过关的时候用来校验是否正常过关。同时在如下几种情况下要清除：
     * 1、玩家断开连接
     * 2、到点失败
     */
    ConcurrentHashMap<String,MiGongPassInfo> miGongPassInfoMap = new ConcurrentHashMap<>();


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

    /**
     * 每分钟检查一次，去掉过期的MiGongPassInfo
     * 注意：要过期一段时间，否则，有些是延时20s
     * @param interval
     */
    @Updatable(isAsynchronous = false,cycle = 60000)
    public void checkMiGongPassInfo(int interval){
        long currentTime = System.currentTimeMillis();
        for(Map.Entry<String,MiGongPassInfo> entry : miGongPassInfoMap.entrySet()){
            MiGongPassInfo miGongPassInfo = entry.getValue();
            if((miGongPassInfo.getTime()+20)*1000 < currentTime - miGongPassInfo.getStartTime().getTime()){
                miGongPassInfoMap.remove(entry.getKey());
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
        int size = difficulty*5+15;
        int door = 0;
        miGongPassInfo.setSize(size);
        miGongPassInfo.setDoor(door);
        miGongPassInfo.setStart(new Element(0,0));
        miGongPassInfo.setEnd(new Element(size-1,size-1));
        CreateMap createMap = new CreateMap(size,size,new Element(0,0),new Element(size-1,size-1));
        miGongPassInfo.setCreateMap(createMap);
        byte[][] map=createMap.getMap();											//获取地图数组
        // 拆墙
        for(int i=0;i<door;i++){
            int x=(int)(Math.random()*(size-2))+1;					//1---tr-1
            int y=(int)(Math.random()*(size-2))+1;					//1---td-1
            map[x][y]=0;
        }
        miGongPassInfo.setTime(difficulty*30);
        miGongPassInfo.setSpeed(10);
        return miGongPassInfo;
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
    }
}
