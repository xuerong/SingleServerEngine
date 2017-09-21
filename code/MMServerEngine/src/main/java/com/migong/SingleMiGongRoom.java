package com.migong;

import com.migong.map.CreateMap;

public class SingleMiGongRoom extends MiGongRoom {

    private int level;

    public SingleMiGongRoom(CreateMap createMap) {
        super(createMap);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
