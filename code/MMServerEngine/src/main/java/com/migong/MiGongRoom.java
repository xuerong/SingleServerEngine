package com.migong;

import com.migong.map.CreateMap;

public abstract class MiGongRoom {

    public static final int USER_COUNT = 2; // 每个房间的人数
    public static final long MAX_WAIT_TIME = 40 * 1000l; // 等待最多时间
    public static final long BEGIN_WAIT_TIME = 3; // 开始等待时间，s


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
