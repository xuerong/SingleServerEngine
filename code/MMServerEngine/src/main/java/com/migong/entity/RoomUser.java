package com.migong.entity;

import com.migong.map.Element;
import com.mm.engine.framework.data.entity.session.Session;

import java.util.ArrayList;
import java.util.List;

public class RoomUser {
    private Session session;
    private long beginTime;

    // 入口和出口
    private Element in;
    private Element out;

    private List<RoomUserState> history = new ArrayList<>();
    private RoomUserState roomUserState = new RoomUserState();
    private List<Bean> eatBeanNotSend = new ArrayList<>();
    private List<Bean> eatBean = new ArrayList<>();

    private volatile boolean isChange;

    private int roomRank = -1;

    public RoomUser(Session session, long beginTime){
        this.session = session;
        this.beginTime = beginTime;
    }

    public synchronized void setState(float posX,float posY,int dir,int speed){
        roomUserState.setDir(dir);
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

    public boolean checkState(float x,float y,int dir,int speed){
        // todo 检验位置
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
}
