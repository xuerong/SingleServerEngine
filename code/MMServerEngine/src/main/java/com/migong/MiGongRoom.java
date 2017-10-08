package com.migong;

import com.migong.map.CreateMap;

public abstract class MiGongRoom {

    public static final int USER_COUNT = 4; // 每个房间的人数
    public static final long MAX_WAIT_TIME = 40 * 1000l; // 等待最多时间


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
