package com.migong.entity;

import com.migong.map.CreateMap;
import com.migong.map.Element;

/**
 * Created by Administrator on 2017/9/22.
 * 这个不进行存储了，就是暂存玩家正在玩的关卡信息
 */
public class MiGongPassInfo {
    private CreateMap createMap;
    private Element start;
    private Element end;
    private int speed;
    private int time;
    private int size;
    private int door;
    private int difficulty;

    public CreateMap getCreateMap() {
        return createMap;
    }

    public void setCreateMap(CreateMap createMap) {
        this.createMap = createMap;
    }

    public Element getStart() {
        return start;
    }

    public void setStart(Element start) {
        this.start = start;
    }

    public Element getEnd() {
        return end;
    }

    public void setEnd(Element end) {
        this.end = end;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDoor() {
        return door;
    }

    public void setDoor(int door) {
        this.door = door;
    }
}
