package com.migong;

import com.migong.entity.Bean;
import com.migong.map.CreateMap;

import java.util.Map;

public abstract class MiGongRoom {

    public static final int USER_COUNT = 2; // 每个房间的人数
    public static final long MAX_WAIT_TIME = 40 * 1000l; // 匹配等待最多时间
    public static final long ROOM_MAX_TIME = 400 * 1000l; // todo 房间时间，要不要定死？还是根据情况设置?还是配置？
    public static final long BEGIN_WAIT_TIME = 3; // 开始等待时间，s

    protected int size;
    protected CreateMap createMap;
    protected Map<Integer,Bean> beans;
    public MiGongRoom(CreateMap createMap,int size){
        this.createMap = createMap;
        this.size = size;
    }


    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public CreateMap getCreateMap() {
        return createMap;
    }

    public void setCreateMap(CreateMap createMap) {
        this.createMap = createMap;
    }

    public Map<Integer, Bean> getBeans() {
        return beans;
    }

    public void setBeans(Map<Integer, Bean> beans) {
        this.beans = beans;
    }
}
