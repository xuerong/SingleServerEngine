package com.migong;

import com.migong.map.CreateMap;

public abstract class MiGongRoom {
    private CreateMap createMap;
    public MiGongRoom(CreateMap createMap){
        this.createMap = createMap;
    }

    public CreateMap getCreateMap() {
        return createMap;
    }

    public void setCreateMap(CreateMap createMap) {
        this.createMap = createMap;
    }
}
