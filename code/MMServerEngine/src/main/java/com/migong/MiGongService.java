package com.migong;

import com.migong.entity.UserMiGong;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;

import java.util.ArrayList;
import java.util.List;
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

    public void init(){
        System.out.println("MiGongService init");
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session) throws Throwable{
        System.out.println("do request getMap");
        MiGongPB.CSGetMiGongMap getMiGongMap = MiGongPB.CSGetMiGongMap.parseFrom((byte[])clientData);
        UserMiGong userMiGong = dataService.selectCreateIfAbsent(UserMiGong.class,"userId={}",session.getAccountId());
//        if()

        MiGongPB.SCGetMiGongMap.Builder builder = MiGongPB.SCGetMiGongMap.newBuilder();

        int size = 20;
        CreateMap myMap=new CreateMap(size,size,new Element(0,0),new Element(size-1,size-1));							//随机产生地图
        byte[][] map=myMap.getMap();											//获取地图数组

        // 拆墙
        int num = 0;
        for(int i=0;i<num;i++){
            int x=(int)(Math.random()*(size-2))+1;					//1---tr-1
            int y=(int)(Math.random()*(size-2))+1;					//1---td-1
            map[x][y]=0;
        }

        List<Integer> integers = new ArrayList<>(size*size);
        for(byte[] aa : map){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);

        byte[] sendData = builder.build().toByteArray();
//        System.out.println("builder.getMapList().size() = "+builder.getMapList().size()+",sendData length:"+sendData.length);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongMap, sendData);
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
