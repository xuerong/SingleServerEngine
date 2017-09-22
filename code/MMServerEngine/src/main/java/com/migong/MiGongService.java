package com.migong;

import com.migong.entity.MiGongPassInfo;
import com.migong.entity.UserMiGong;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.MiGongLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 */
@Service(init = "init")
public class MiGongService {
    private DataService dataService;
//    ConcurrentHashMap<>

    private final Map<Integer,MiGongLevel> levelMap = new HashMap<>();
    public void init(){
        System.out.println("MiGongService init");
        for(MiGongLevel miGongLevel : MiGongLevel.datas){
            levelMap.put(miGongLevel.getLevel(),miGongLevel);
        }
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session) throws Throwable{
        System.out.println("do request getMap");
        MiGongPB.CSGetMiGongMap getMiGongMap = MiGongPB.CSGetMiGongMap.parseFrom((byte[])clientData);
        UserMiGong userMiGong = dataService.selectCreateIfAbsent(UserMiGong.class,"userId={}",session.getAccountId());
        // 当前等级和关卡
        MiGongLevel miGongLevel = levelMap.get(getMiGongMap.getLevel());
        if(miGongLevel == null || miGongLevel.getPass() < getMiGongMap.getPass() || getMiGongMap.getPass() < 1){
            throw new ToClientException("Invalid params");
        }
        if(getMiGongMap.getLevel() >= userMiGong.getLevel()){
            if(getMiGongMap.getLevel() == userMiGong.getLevel() && getMiGongMap.getPass() > userMiGong.getPass()+1){
                throw new ToClientException("Invalid params");
            }
            if(getMiGongMap.getLevel() > userMiGong.getLevel()){
                if(getMiGongMap.getLevel() != userMiGong.getLevel() +1 || getMiGongMap.getPass() != 1 ||
                        (userMiGong.getLevel() >0 && userMiGong.getPass() < levelMap.get(userMiGong.getLevel()).getPass())){
                    throw new ToClientException("Invalid params");
                }
            }
        }
        //
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setDifficulty(miGongLevel.getDifficulty());
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo);
        MiGongPB.SCGetMiGongMap.Builder builder = MiGongPB.SCGetMiGongMap.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//随机产生地图


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

        byte[] sendData = builder.build().toByteArray();
//        System.out.println("builder.getMapList().size() = "+builder.getMapList().size()+",sendData length:"+sendData.length);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongMap, sendData);
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

        // 校验

        MiGongPB.SCSendWalkingRoute.Builder builder = MiGongPB.SCSendWalkingRoute.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCSendWalkingRoute, builder.build().toByteArray());
    }

}
