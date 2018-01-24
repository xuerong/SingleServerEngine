package com.mm.engine.framework.data.entity;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/11/13.
 */
@DBEntity(tableName = "serverInfo",pks = {"id"})
public class ServerInfo implements Serializable {
    public static final int FullAccountCount = 100000;
    public static final int MaxAccountCount = 200000;

    private int id;
    private String ip;
    private int port;
    private int accountCount;
    private int hot; // 火爆程度，根据最近的登陆情况计算
    private int state; // 状态

    public boolean isFull(){
        return accountCount>FullAccountCount;
    }

    public boolean isMax(){
        return accountCount>MaxAccountCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getHot() {
        return hot;
    }

    public void setHot(int hot) {
        this.hot = hot;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public static enum  ServerState{
        Ok,
        Fixing;

        public static ServerState getStateByInt(int state){
            return values()[state];
        }
    }
}
