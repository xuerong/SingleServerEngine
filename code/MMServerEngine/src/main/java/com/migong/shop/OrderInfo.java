package com.migong.shop;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;
import java.sql.Timestamp;

/**
* id` int(11) NOT NULL,
 `token` varchar(255) DEFAULT NULL,
 `userId` varchar(255) DEFAULT NULL,
 `peckId` int(11) DEFAULT NULL,
 `num` int(11) DEFAULT NULL,
 `time` timestamp NULL DEFAULT NULL,
 `success` int(11) DEFAULT NULL,
 `money` int(11) DEFAULT NULL,
 `gold` int(11) DEFAULT NULL,
 `items` varchar(255) DEFAULT NULL,
**/
@DBEntity(tableName = "orderInfo",pks = {"id"})
public class OrderInfo implements Serializable{
    private int id;
    private String token;
    private String userId;
    private int peckId;
    private int num;
    private Timestamp time;
    private int success;
    private int money;
    private int gold;
    private String items;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPeckId() {
        return peckId;
    }

    public void setPeckId(int peckId) {
        this.peckId = peckId;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }
}
