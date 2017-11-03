package com.migong;

import com.migong.map.CreateMap;

public class SingleMiGongRoom extends MiGongRoom {

    private int level;

    public SingleMiGongRoom(CreateMap createMap,int size) {
        super(createMap,size);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
