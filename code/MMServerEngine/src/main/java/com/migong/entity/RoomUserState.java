package com.migong.entity;

/**
 * Created by Administrator on 2017/10/9.
 * 玩家状态
 */
public class RoomUserState {
    private float posX;
    private float posY;
    private float dirX;
    private float dirY;
    private int speed;
    private long time; // 成为这个状态的时间

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getDirX() {
        return dirX;
    }

    public void setDirX(float dirX) {
        this.dirX = dirX;
    }

    public float getDirY() {
        return dirY;
    }

    public void setDirY(float dirY) {
        this.dirY = dirY;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public RoomUserState clone(){
        RoomUserState roomUserState = new RoomUserState();
        roomUserState.setPosX(this.posX);
        roomUserState.setPosY(this.posY);
        roomUserState.setSpeed(this.speed);
        roomUserState.setDirX(this.dirX);
        roomUserState.setDirY(this.dirY);
        roomUserState.setTime(this.time);
        return roomUserState;
    }
}
