package com.migong.item;

import com.mm.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/7.
 * 道具
 */
@DBEntity(tableName = "item",pks = {"userId","itemId"})
public class Item implements Serializable{
    private String userId;
    private int itemId;
    private int count;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public enum ItemType{
        Energy(false), // 精力瓶
        AddSpeed(true),// 加速：参数为速度，分别为5,10,15等
        AddTime(true),// 加时间，参数为时间，分别为：50%，100%，200%等
        MulBean(true),// 吃豆翻倍，*1,*2,*3
        ShowRoute(true),// 显示路线（这个可以！）：直接显示，不退却
        ;
        private final boolean isSkill;
        ItemType(boolean isSkill){
            this.isSkill = isSkill;
        }

        public boolean isSkill() {
            return isSkill;
        }
    }
}
