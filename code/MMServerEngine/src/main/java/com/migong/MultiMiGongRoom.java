package com.migong;

import com.migong.entity.MiGongThreadFactory;
import com.migong.entity.RoomUser;
import com.migong.entity.RoomUserState;
import com.migong.map.CreateMap;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiMiGongRoom extends MiGongRoom {
    private static ScheduledExecutorService roomBeginExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 1, new MiGongThreadFactory("roomBegin"));

    private Map<String,RoomUser> roomUsers = new HashMap<>();
    private long beginTime;

    private AtomicInteger frame = new AtomicInteger(0);

    public MultiMiGongRoom(CreateMap createMap, List<RoomUser> roomUsers) {
        super(createMap);
        for(RoomUser roomUser : roomUsers){
            this.roomUsers.put(roomUser.getSession().getAccountId(),roomUser);
        }
    }
    public RoomUser getRoomUser(String accountId){
        return roomUsers.get(accountId);
    }
    public Map<String, RoomUser> getRoomUsers() {
        return roomUsers;
    }

    public void begin() {
        this.beginTime = System.currentTimeMillis();
        // 开启循环
        roomBeginExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                frame.getAndIncrement();

                List<MiGongPB.PBUserMoveInfo> userMoveInfos = null;
                for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
                    if(entry.getValue().isChange()){
                        MiGongPB.PBUserMoveInfo.Builder builder = MiGongPB.PBUserMoveInfo.newBuilder();
                        RoomUserState roomUserState = entry.getValue().clearAndGetRoomUserState();
                        builder.setPosX(roomUserState.getPosX());
                        builder.setPosY(roomUserState.getPosY());
                        builder.setDir(roomUserState.getDir());
                        builder.setSpeed(roomUserState.getSpeed());
                        builder.setFrame(frame.get());
                        builder.setUserId(entry.getKey());
                        if(userMoveInfos == null){
                            userMoveInfos = new ArrayList<MiGongPB.PBUserMoveInfo>();
                        }
                        userMoveInfos.add(builder.build());
                    }
                }
                if(userMoveInfos != null){
                    MiGongPB.SCUserMove.Builder builder = MiGongPB.SCUserMove.newBuilder();
                    builder.addAllUserMoveInfos(userMoveInfos);
                    byte[] data = builder.build().toByteArray();
                    for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
                        entry.getValue().getSession().getMessageSender().sendMessage(MiGongOpcode.SCUserMove,data);
                    }
                }
            }
        },0,60, TimeUnit.MILLISECONDS);
    }
}
