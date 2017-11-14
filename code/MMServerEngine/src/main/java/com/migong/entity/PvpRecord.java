package com.migong.entity;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017/11/14.
 */
@DBEntity(tableName = "pvpRecord",pks = {"id"})
public class PvpRecord implements Serializable {
    private long id;
    private Timestamp time;
    private int grade;
    private String record;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }
}
