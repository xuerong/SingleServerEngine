package com.migong.entity;

/**
 * Created by Administrator on 2017/10/9.
 * 玩家状态
 */
public class RoomUserState {
    private float posX;
    private float posY;
    private int dir;
    private int speed;

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

    public int getDir() {
        return dir;
    }

    public void setDir(int dir) {
        this.dir = dir;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public RoomUserState clone(){
        RoomUserState roomUserState = new RoomUserState();
        roomUserState.setPosX(this.posX);
        roomUserState.setPosY(this.posY);
        roomUserState.setSpeed(this.speed);
        roomUserState.setDir(this.dir);
        return roomUserState;
    }
}
