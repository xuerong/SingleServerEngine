package com.migong.entity;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "unlimitedStarAward",pks = {"userId"})
public class UnlimitedStarAward implements Serializable {
    private String userId;
    private int star; // 今天得到的星数
    private long resetTime;
    private String award; // 领奖情况

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }

    public String getAward() {
        return award;
    }

    public void setAward(String award) {
        this.award = award;
    }
}
