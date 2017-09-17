package com.migong;

import com.migong.map.CreateMap;

public abstract class Room {
    private CreateMap createMap;
    public Room(CreateMap createMap){
        this.createMap = createMap;
    }

    public CreateMap getCreateMap() {
        return createMap;
    }

    public void setCreateMap(CreateMap createMap) {
        this.createMap = createMap;
    }
}
