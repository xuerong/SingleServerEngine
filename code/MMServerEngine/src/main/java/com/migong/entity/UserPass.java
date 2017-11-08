package com.migong.entity;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/9/21.
 * userId,passId,star,useTime,score：重新打的时候，存储条件：星级大，或者星级相等分数大
 */
@DBEntity(tableName = "userPass",pks = {"userId,passId"})
public class UserPass implements Serializable {
    private String userId;
    private int passId; // 关卡id
    private int star; // 星数
    private int useTime; // 耗时
    private int score; // 分数

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPassId() {
        return passId;
    }

    public void setPassId(int passId) {
        this.passId = passId;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getUseTime() {
        return useTime;
    }

    public void setUseTime(int useTime) {
        this.useTime = useTime;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
