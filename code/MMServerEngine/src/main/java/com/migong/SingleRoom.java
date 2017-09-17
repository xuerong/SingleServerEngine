package com.migong;

import com.migong.map.CreateMap;

public class SingleRoom extends Room{

    private int level;

    public SingleRoom(CreateMap createMap) {
        super(createMap);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
