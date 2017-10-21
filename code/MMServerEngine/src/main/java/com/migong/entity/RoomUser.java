package com.migong.entity;

import com.mm.engine.framework.data.entity.session.Session;

public class RoomUser {
    private Session session;
    private long beginTime;

    private RoomUserState roomUserState = new RoomUserState();

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
}
