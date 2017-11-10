package com.migong;

import com.migong.entity.Bean;
import com.migong.map.CreateMap;

import java.util.Map;

public abstract class MiGongRoom {

    public static final int USER_COUNT = 2; // 每个房间的人数
    public static final long BEGIN_WAIT_TIME = 3; // 开始等待时间，s

    protected int size;
    protected int time;
    protected int speed;
    protected CreateMap createMap;
    protected Map<Integer,Bean> beans;
    public MiGongRoom(CreateMap createMap,int size,int time,int speed){
        this.createMap = createMap;
        this.size = size;
        this.time = time;
        this.speed = speed;
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

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
