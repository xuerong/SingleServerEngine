package com.migong;

import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.protocol.AccountPB;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;

import java.util.ArrayList;
import java.util.List;

@Service(init = "init")
public class MiGongService {
    public void init(){
        System.out.println("MiGongService init");
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session){
        System.out.println("do request getMap");
        MiGongPB.SCGetMiGongMap.Builder builder = MiGongPB.SCGetMiGongMap.newBuilder();

        int size = 50;
        CreateMap myMap=new CreateMap(size,size,new Element(0,0),new Element(size-1,size-1));							//随机产生地图
        byte[][] map=myMap.getMap();											//获取地图数组
        List<Integer> integers = new ArrayList<>(size*size);
        for(byte[] aa : map){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);

        byte[] sendData = builder.build().toByteArray();
        System.out.println("builder.getMapList().size() = "+builder.getMapList().size()+",sendData length:"+sendData.length);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongMap, sendData);
    }
}
