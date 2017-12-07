package com.migong.entity;

import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.table.ItemTable;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Administrator on 2017/9/22.
 * 这个不进行存储了，就是暂存玩家正在玩的关卡信息
 */
public class MiGongPassInfo {
    private CreateMap createMap;
    private Element start;
    private Element end;
    private int speed;
    private int time;
    private int size;
    private int door;
    private int pass;
    private Bean[] beans; // 豆子
    private Timestamp startTime; // 开始时间
    private int bean1;
    private int bean5;
    private int bean10;
    private int mulBean = 1; // 豆子的倍数
    private List<ItemTable> useItems ; // 使用的item


    public CreateMap getCreateMap() {
        return createMap;
    }

    public void setCreateMap(CreateMap createMap) {
        this.createMap = createMap;
    }

    public Element getStart() {
        return start;
    }

    public void setStart(Element start) {
        this.start = start;
    }

    public Element getEnd() {
        return end;
    }

    public void setEnd(Element end) {
        this.end = end;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }


    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDoor() {
        return door;
    }

    public void setDoor(int door) {
        this.door = door;
    }


    public int getPass() {
        return pass;
    }

    public void setPass(int pass) {
        this.pass = pass;
    }

    public Bean[] getBeans() {
        return beans;
    }

    public void setBeans(Bean[] beans) {
        this.beans = beans;
    }


    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public int getBean1() {
        return bean1;
    }

    public void setBean1(int bean1) {
        this.bean1 = bean1;
    }

    public int getBean5() {
        return bean5;
    }

    public void setBean5(int bean5) {
        this.bean5 = bean5;
    }

    public int getBean10() {
        return bean10;
    }

    public void setBean10(int bean10) {
        this.bean10 = bean10;
    }

    public int getMulBean() {
        return mulBean;
    }

    public void setMulBean(int mulBean) {
        this.mulBean = mulBean;
    }

    public List<ItemTable> getUseItems() {
        return useItems;
    }

    public void setUseItems(List<ItemTable> useItems) {
        this.useItems = useItems;
    }

    public void setBeanCount(int bean1, int bean5, int bean10){
        this.bean1 = bean1;
        this.bean5 = bean5;
        this.bean10 = bean10;
    }
}
