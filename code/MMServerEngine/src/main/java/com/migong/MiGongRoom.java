package com.migong;

import com.migong.entity.Bean;
import com.migong.map.CreateMap;

import java.util.Map;

public abstract class MiGongRoom {

    public static final int USER_COUNT = 2; // 每个房间的人数
    public static final long MAX_WAIT_TIME = 40 * 1000l; // 等待最多时间
    public static final long BEGIN_WAIT_TIME = 3; // 开始等待时间，s

    protected int size;
    protected CreateMap createMap;
    protected Map<Integer,Bean> beans;
    public MiGongRoom(CreateMap createMap){
        this.createMap = createMap;
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
