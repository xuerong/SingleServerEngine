package com.migong;

import com.migong.entity.Bean;
import com.migong.entity.MiGongThreadFactory;
import com.migong.entity.RoomUser;
import com.migong.entity.RoomUserState;
import com.migong.map.CreateMap;
import com.migong.map.Element;
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

    private MiGongService miGongService;
    private Map<String,RoomUser> roomUsers = new HashMap<>();
    private int arrivedCount = 0;
    private long beginTime;
    private volatile boolean isOver = false;

    private AtomicInteger frame = new AtomicInteger(0);


    public MultiMiGongRoom(CreateMap createMap,int size, List<RoomUser> roomUsers,MiGongService miGongService) {
        super(createMap,size);
        this.miGongService = miGongService;
        List<Integer> list = new ArrayList<>(); // (1,1)(1,size-1)(size-1,1)(size-1,size-1)
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        Random random = new Random(System.currentTimeMillis());
        for(RoomUser roomUser : roomUsers){
            // 为玩家分配入口和出口
            int pos = list.remove(random.nextInt(list.size()));
            switch (pos){
                case 1:
                    roomUser.setIn(new Element(1,1));
                    roomUser.setOut(new Element(size - 1,size - 1));
                    break;
                case 2:
                    roomUser.setIn(new Element(1,size-1));
                    roomUser.setOut(new Element(size - 1,1));
                    break;
                case 3:
                    roomUser.setIn(new Element(size-1,1));
                    roomUser.setOut(new Element(1,size - 1));
                    break;
                case 4:
                    roomUser.setIn(new Element(size-1,size-1));
                    roomUser.setOut(new Element(1,1));
                    break;
            }
            this.roomUsers.put(roomUser.getSession().getAccountId(),roomUser);
        }
    }

    public void userMove(String accountId,float x,float y,int dir,int speed){
        RoomUser roomUser = roomUsers.get(accountId);
        if(roomUser != null && roomUser.checkState(x,y,dir,speed)){
            // 设置
            roomUser.setState(x,y,dir,speed);
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

    public synchronized void userArrived(String accountId){
        RoomUser roomUser = roomUsers.get(accountId);
        roomUser.setRoomRank(arrivedCount++);
        // 直接推送应该也可以
        MiGongPB.SCUserArrived.Builder builder = MiGongPB.SCUserArrived.newBuilder();
        builder.setUserId(accountId);
        sendAllUsers(MiGongOpcode.SCUserArrived,builder.build().toByteArray());
        // 如果游戏结束了，推送结束信息
        boolean over = true;
        for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
            if(entry.getValue().getRoomRank()<0){
                over = false;
                break;
            }
        }
        if(over){
            // 结束该房间,或者等待定时器结束它.
            doOver(OverType.AllArrived);
        }
    }

    public void start(){
        roomBeginExecutor.schedule(()->{ // 3秒钟开始
            begin();
            MiGongPB.SCBegin.Builder builder = MiGongPB.SCBegin.newBuilder();
            sendAllUsers(MiGongOpcode.SCBegin,builder.build().toByteArray());
            roomBeginExecutor.schedule(()->{ // 时间到了结束
                doOver(OverType.TimeOut);

            },MiGongRoom.ROOM_MAX_TIME,TimeUnit.SECONDS);

        },MiGongRoom.BEGIN_WAIT_TIME,TimeUnit.SECONDS);
    }

    public void doOver(OverType overType){
        this.isOver = true;
        MiGongPB.SCGameOver.Builder overBuilder = MiGongPB.SCGameOver.newBuilder();
        overBuilder.setOverType(overType.ordinal());
        for(RoomUser ru : roomUsers.values()){
            MiGongPB.PBGameOverUserInfo.Builder userBuilder = MiGongPB.PBGameOverUserInfo.newBuilder();
            userBuilder.setUserId(ru.getSession().getAccountId());
            userBuilder.setScore(ru.getScore());
            userBuilder.setRank(ru.getRoomRank());
            userBuilder.setUserName(ru.getSession().getAccountId());
            userBuilder.setArrived(ru.getRoomRank() >= 0?1:0);
            overBuilder.addUserInfos(userBuilder);
        }
        sendAllUsers(MiGongOpcode.SCGameOver,overBuilder.build().toByteArray());
        // todo 保存房间信息
        System.out.println("shutdown");
        roomBeginExecutor.shutdownNow();
        // 通知miGongService清除该房间
        miGongService.multiRoomOver(this);
    }

    public void begin() {
        this.beginTime = System.currentTimeMillis();
        // 开启循环
        roomBeginExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                frame.getAndIncrement();

                List<MiGongPB.PBUserMoveInfo> userMoveInfos = null;
                List<MiGongPB.PBEatBeanInfo> sendEatBeans = null;
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
                            MiGongPB.PBEatBeanInfo.Builder builder = MiGongPB.PBEatBeanInfo.newBuilder();
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
                    MiGongPB.SCSendEatBean.Builder builder = MiGongPB.SCSendEatBean.newBuilder();
                    builder.addAllBeans(sendEatBeans);
                    sendAllUsers(MiGongOpcode.SCSendEatBean,builder.build().toByteArray());
                }
            }
        },0,60, TimeUnit.MILLISECONDS);
    }
    public void sendAllUsers(int opcode,byte[] data){
        for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
            entry.getValue().getSession().getMessageSender().sendMessage(opcode,data);
        }
    }

    public boolean isOver() {
        return isOver;
    }



    enum OverType{
        Other,
        AllArrived,
        TimeOut,
    }
}
