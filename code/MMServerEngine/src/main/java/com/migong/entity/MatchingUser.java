package com.migong.entity;

import com.mm.engine.framework.data.entity.session.Session;

public class MatchingUser {
    private Session session;
    private long beginTime;

    public MatchingUser(Session session,long beginTime){
        this.session = session;
        this.beginTime = beginTime;
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
}
