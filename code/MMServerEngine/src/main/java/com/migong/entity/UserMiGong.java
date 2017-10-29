package com.migong.entity;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/9/21.
 */
@DBEntity(tableName = "userMiGong",pks = {"userId"})
public class UserMiGong implements Serializable {
    private String userId;
    private int level; // 当前等级
    private int pass; // 当前关卡
    private int score;
    private int passUnlimited; // 无线关卡关数

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPass() {
        return pass;
    }

    public void setPass(int pass) {
        this.pass = pass;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getPassUnlimited() {
        return passUnlimited;
    }

    public void setPassUnlimited(int passUnlimited) {
        this.passUnlimited = passUnlimited;
    }
}
