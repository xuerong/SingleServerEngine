package com.migong.entity;

import com.migong.MiGongRoom;
import com.migong.MiGongService;
import com.migong.map.Element;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.security.LocalizationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RoomUser {
    private static final Logger log = LoggerFactory.getLogger(RoomUser.class);
    private Session session;
    private long beginTime; // 开始匹配的时间

    // 入口和出口
    private Element in;
    private Element out;

    private List<RoomUserState> history = new ArrayList<>();
    private RoomUserState roomUserState = new RoomUserState();
    private List<Bean> eatBeanNotSend = new ArrayList<>();
    private List<Bean> eatBean = new ArrayList<>();

    private volatile boolean isChange;

    private boolean isSuccess;
    private int roomRank = -1;

    private UserState userState;

    public RoomUser(Session session, long beginTime){
        this.session = session;
        this.beginTime = beginTime;
    }

    public synchronized void setState(float posX,float posY,float dirX,float dirY,int speed){
        roomUserState.setDirX(dirX);
        roomUserState.setDirY(dirY);
        roomUserState.setPosX(posX);
        roomUserState.setPosY(posY);
        roomUserState.setSpeed(speed);
        roomUserState.setTime(System.currentTimeMillis());
        this.isChange = true;
        history.add(roomUserState.clone()); // 存储历史
    }

    public synchronized RoomUserState clearAndGetRoomUserState() {
        this.isChange = false;
        return roomUserState.clone();
    }

    public int getScore(){
        int ret = 0;
        for(Bean bean : eatBean){
            ret+=bean.getScore();
        }
        return ret;
    }

    public boolean checkState(float x,float y,float dirX,float dirY,int speed){
        // todo 检验位置
        if(true){
            if(dirX != 0 && dirY != 0){
                if(MiGongService.debug2){
                    log.warn("dirx!=0 && diry!=0,is auto dir open?");
                }
                return false;
            }
        }
        return true;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public boolean isChange() {
        return isChange;
    }

    public List<Bean> getEatBeanNotSend() {
        return eatBeanNotSend;
    }

    public void setEatBeanNotSend(List<Bean> eatBeanNotSend) {
        this.eatBeanNotSend = eatBeanNotSend;
    }

    public List<Bean> getEatBean() {
        return eatBean;
    }

    public void setEatBean(List<Bean> eatBean) {
        this.eatBean = eatBean;
    }

    public int getRoomRank() {
        return roomRank;
    }

    public void setRoomRank(int roomRank) {
        this.roomRank = roomRank;
    }

    public Element getOut() {
        return out;
    }

    public void setOut(Element out) {
        this.out = out;
    }

    public Element getIn() {
        return in;
    }

    public void setIn(Element in) {
        this.in = in;
    }

    public List<RoomUserState> getHistory() {
        return history;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public UserState getUserState() {
        return userState;
    }

    public void setUserState(UserState userState) {
        this.userState = userState;
    }

    /**
     * idle=idle
     cancel=cancel
     matching=matching
     inRoom=in room
     offline=offline
     */

    public static enum UserState{
        None("idle"),
        Matching("matching"),
        Playing("inRoom"),
        Offline("offline"); // 掉线

        private final String describe;
        UserState(String describe){
            this.describe = describe;
        }
        public String getDescribe(){
            return LocalizationMessage.getText(describe);
        }
    }
}
