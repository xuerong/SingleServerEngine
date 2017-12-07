package com.migong.entity;

import com.mm.engine.framework.control.ServiceHelper;
import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.sys.SysPara;
import org.apache.commons.lang.time.DateUtils;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017/9/21.
 * userId,unlimitedPass,unlimitedStar,starCount(启动的时候校验一下),vip,ladderScore
 */
@DBEntity(tableName = "userMiGong",pks = {"userId"})
public class UserMiGong implements Serializable {
    private String userId;
    private int unlimitedPass; // 无线关卡关数
    private int unlimitedStar; // 无线关卡星数
    private int pass; // 推图关卡
    private int starCount; // 推图星数
    private int vip; // vip等级
    private int ladderScore; // 天梯分数
    private int energy; // 精力
    private long energyUpdateTime; // 精力刷新时间
    private String newUserGuide; // 新手引导相关，结构：id;step;id;step
    private int gold; // 金币

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getUnlimitedPass() {
        return unlimitedPass;
    }

    public void setUnlimitedPass(int unlimitedPass) {
        this.unlimitedPass = unlimitedPass;
    }

    public int getUnlimitedStar() {
        return unlimitedStar;
    }

    public void setUnlimitedStar(int unlimitedStar) {
        this.unlimitedStar = unlimitedStar;
    }

    public int getPass() {
        return pass;
    }

    public void setPass(int pass) {
        this.pass = pass;
    }

    public int getStarCount() {
        return starCount;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public int getVip() {
        return vip;
    }

    public void setVip(int vip) {
        this.vip = vip;
    }

    public int getLadderScore() {
        return ladderScore;
    }

    public void setLadderScore(int ladderScore) {
        this.ladderScore = ladderScore;
    }

    public long getEnergyUpdateTime() {
        return energyUpdateTime;
    }

    public void setEnergyUpdateTime(long energyUpdateTime) {
        this.energyUpdateTime = energyUpdateTime;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public String getNewUserGuide() {
        return newUserGuide;
    }

    public void setNewUserGuide(String newUserGuide) {
        this.newUserGuide = newUserGuide;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }
}
