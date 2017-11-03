package com.migong.entity;

import com.mm.engine.framework.data.entity.session.Session;

import java.util.ArrayList;
import java.util.List;

public class RoomUser {
    private Session session;
    private long beginTime;

    private RoomUserState roomUserState = new RoomUserState();
    private List<Bean> eatBeanNotSend = new ArrayList<>();
    private List<Bean> eatBean = new ArrayList<>();

    private volatile boolean isChange;

    public RoomUser(Session session, long beginTime){
        this.session = session;
        this.beginTime = beginTime;
    }

    public synchronized void setState(float posX,float posY,int dir,int speed){
        roomUserState.setDir(dir);
        roomUserState.setPosX(posX);
        roomUserState.setPosY(posY);
        roomUserState.setSpeed(speed);
        this.isChange = true;
    }

    public synchronized RoomUserState clearAndGetRoomUserState() {
        this.isChange = false;
        return roomUserState.clone();
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
}
