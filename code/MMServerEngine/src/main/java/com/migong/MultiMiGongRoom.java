package com.migong;

import com.migong.entity.*;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.mm.engine.framework.control.room.Room;
import com.mm.engine.framework.data.DataService;
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
    private long beginTime;
    private volatile boolean isOver = false;
    private Future future;

    private int grade; // 段位

    private AtomicInteger frame = new AtomicInteger(0);


    public MultiMiGongRoom(int grade,CreateMap createMap,int size,int time,int speed, List<RoomUser> roomUsers,MiGongService miGongService) {
        super(createMap,size,time,speed);
        this.grade = grade;
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
        roomUser.setSuccess(true);
        // 直接推送应该也可以
        MiGongPB.SCUserArrived.Builder builder = MiGongPB.SCUserArrived.newBuilder();
        builder.setUserId(accountId);
        sendAllUsers(MiGongOpcode.SCUserArrived,builder.build().toByteArray());
        // 如果游戏结束了，推送结束信息
        boolean over = true;
        for(Map.Entry<String,RoomUser> entry : roomUsers.entrySet()){
            if(!entry.getValue().isSuccess()){
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
        future = roomBeginExecutor.schedule(()->{ // 3秒钟开始
            begin();
            MiGongPB.SCBegin.Builder builder = MiGongPB.SCBegin.newBuilder();
            sendAllUsers(MiGongOpcode.SCBegin,builder.build().toByteArray());
            future = roomBeginExecutor.schedule(()->{ // 时间到了结束
                doOver(OverType.TimeOut);

            },time,TimeUnit.SECONDS);

        },MiGongRoom.BEGIN_WAIT_TIME,TimeUnit.SECONDS);
    }

    public void doOver(OverType overType){
        this.isOver = true;

        List<RoomUser> roomUserList = Arrays.asList(roomUsers.values().toArray(new RoomUser[roomUsers.values().size()]));
        roomUserList.sort(new Comparator<RoomUser>() {
            @Override
            public int compare(RoomUser o1, RoomUser o2) {
                if(!o1.isSuccess() && o2.isSuccess()){
                    return 1;
                }else if(o1.isSuccess() && !o2.isSuccess()){
                    return -1;
                }
                return o2.getScore() - o1.getScore();
            }
        });


        MiGongPB.SCGameOver.Builder overBuilder = MiGongPB.SCGameOver.newBuilder();
        overBuilder.setOverType(overType.ordinal());
        int rank=1;
        for(RoomUser ru : roomUserList){
            //
            MiGongPB.PBGameOverUserInfo.Builder userBuilder = MiGongPB.PBGameOverUserInfo.newBuilder();
            userBuilder.setUserId(ru.getSession().getAccountId());
            userBuilder.setScore(ru.getScore());
            userBuilder.setRank(rank++);
            userBuilder.setUserName(ru.getSession().getAccountId());
            userBuilder.setArrived(ru.isSuccess()?1:0);
            overBuilder.addUserInfos(userBuilder);
        }
        sendAllUsers(MiGongOpcode.SCGameOver,overBuilder.build().toByteArray());
        if(future != null){
            future.cancel(true);
        }
        // 通知miGongService清除该房间
        miGongService.multiRoomOver(this);
    }
    // todo 玩家掉线:玩家掉线可以不做任何处理，外部已经移除了玩家的所有信息，所以，不影响玩家再次玩，
    // 而这次结束之后，自动清理外部的数据
    public void userLogout(String userId){
        RoomUser roomUser = roomUsers.get(userId);
        if(roomUser != null){
            roomUser.setUserState(RoomUser.UserState.Offline);
        }
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
            if(entry.getValue().getUserState() != RoomUser.UserState.Offline) {
                entry.getValue().getSession().getMessageSender().sendMessage(opcode, data);
            }
        }
    }

    public boolean isOver() {
        return isOver;
    }

    public int getGrade() {
        return grade;
    }

    /**
     * 保存历史记录的String
     * 四个玩家的信息：accountId，操作记录（方向，位置，速度，时间），吃的豆子数量，排名
     * 房间的信息：时间，地图，豆子的位置，
     * @return
     */
    public String toInfoString(){
        char sp1='|',sp2=';',sp3=',';
        StringBuilder sb = new StringBuilder();
        sb.append(beginTime).append(sp1)
                .append(size).append(sp1)
                .append(time).append(sp1)
                .append(speed).append(sp1)
                .append(grade).append(sp1)
                .append(mapToString()).append(sp1)
                .append(beansToString()).append(sp1)
                .append(allRoomUserToString(roomUsers.values()));
        return sb.toString();
    }
    private String allRoomUserToString(Collection<RoomUser> roomUsers){
        StringBuilder sb = new StringBuilder();
        for(RoomUser roomUser :roomUsers){
            sb.append(roomUserToString(roomUser)).append("|");
        }
        return sb.toString();
    }
    private String roomUserToString(RoomUser roomUser){
        StringBuilder sb = new StringBuilder();
        sb.append(roomUser.getSession().getAccountId()).append("|")
                .append(getBeanScore(roomUser.getEatBean())).append("|")
                .append(roomUser.getRoomRank()).append("|")
                .append(operListToString(roomUser.getHistory()));
        return sb.toString();
    }
    private int getBeanScore(List<Bean> beans){
        int result = 0;
        for(Bean bean : beans){
            result += bean.getScore();
        }
        return result;
    }
    private String operListToString(List<RoomUserState> roomUserStates){
        StringBuilder sb = new StringBuilder();
        for(RoomUserState roomUserState : roomUserStates){
            sb.append(roomUserState.getPosX()).append(",").append(roomUserState.getPosY()).append(",").append(roomUserState.getDir()).append(",")
                    .append(roomUserState.getSpeed()).append(",").append(roomUserState.getTime()).append(";");
        }
        if(sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }
    private String mapToString(){
        StringBuilder sb = new StringBuilder();
        int i=0,j=0;
        for(byte[] bs : createMap.getMap()){
            for(byte b : bs){
                sb.append(i).append(",").append(j).append(";");
                j++;
            }
            i++;
        }
        if(sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }
    private String beansToString(){
        StringBuilder sb = new StringBuilder();
        for(Bean bean : beans.values()){
            sb.append(bean.getX()).append(",").append(bean.getY()).append(",").append(bean.getScore()).append(";");
        }
        if(sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }


    enum OverType{
        Other,
        AllArrived,
        TimeOut,
    }
}
