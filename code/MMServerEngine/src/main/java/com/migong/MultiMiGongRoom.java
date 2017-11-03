package com.migong;

import com.migong.entity.Bean;
import com.migong.entity.MiGongThreadFactory;
import com.migong.entity.RoomUser;
import com.migong.entity.RoomUserState;
import com.migong.map.CreateMap;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiMiGongRoom extends MiGongRoom {
    private static final Logger log = LoggerFactory.getLogger(MultiMiGongRoom.class);

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


    public synchronized void eatBean(String accountId,int pos){
        // 判断豆是否存在
        Bean bean = beans.remove(pos);
        if(bean == null){
            log.warn("bean is not exist,maybe eat by other");
            return;
        }
        RoomUser roomUser = roomUsers.get(accountId);
        roomUser.getEatBeanNotSend().add(bean);
        roomUser.getEatBean().add(bean);
    }

    public void begin() {
        this.beginTime = System.currentTimeMillis();
        // 开启循环
        roomBeginExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                frame.getAndIncrement();

                List<MiGongPB.PBUserMoveInfo> userMoveInfos = null;
                List<MiGongPB.SCSendEatBean> sendEatBeans = null;
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
                    if(entry.getValue().getEatBeanNotSend().size() > 0){
                        Iterator<Bean> it = entry.getValue().getEatBeanNotSend().iterator();
                        while (it.hasNext()){
                            Bean bean = it.next();
                            it.remove();
                            if(sendEatBeans == null){
                                sendEatBeans = new ArrayList<>();
                            }
                            MiGongPB.SCSendEatBean.Builder builder = MiGongPB.SCSendEatBean.newBuilder();
                            builder.setBeanPos(bean.toInt(size));
                            builder.setUserId(entry.getKey());
                            sendEatBeans.add(builder.build());
                        }
                    }
                }
                if(userMoveInfos != null){
                    MiGongPB.SCUserMove.Builder builder = MiGongPB.SCUserMove.newBuilder();
                    builder.addAllUserMoveInfos(userMoveInfos);
                    byte[] data = builder.build().toByteArray();
                    sendAllUsers(MiGongOpcode.SCUserMove,data);
                }
                if(sendEatBeans != null){
                    for(MiGongPB.SCSendEatBean scSendEatBean : sendEatBeans){ // todo 后面多条要弄成一个协议，一次发送
                        sendAllUsers(MiGongOpcode.SCSendEatBean,scSendEatBean.toByteArray());
                    }
                }
            }
        },0,60, TimeUnit.MILLISECONDS);
    }
    public void sendAllUsers(int opcode,byte[] data){
        for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
            entry.getValue().getSession().getMessageSender().sendMessage(opcode,data);
        }
    }
}
