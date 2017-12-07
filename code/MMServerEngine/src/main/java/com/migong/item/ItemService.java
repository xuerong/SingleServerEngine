package com.migong.item;

import com.migong.MiGongService;
import com.migong.entity.UserMiGong;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.ItemTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/12/7.
 * 道具，这里面主要有如下：
 * 一、四种技能道具：
 * 1、加速：参数为速度，分别为5,10,15等
 * 2、加时间，参数为时间，分别为：50%，100%，200%等
 * 3、吃豆翻倍，*1,*2,*3
 * 4、显示路线（这个可以！）：直接显示，不退却
 *
 * 二、体力瓶
 *  1点体力
 *  5点体力
 *  30点体力
 *
 * 钱-金币-单个道具
 *   -礼包-[金币][道具][体力瓶]
 */
@Service(init = "init")
public class ItemService {


    private DataService dataService;
    private MiGongService miGongService;

    Map<Integer,ItemTable> itemTableMap = new HashMap<>();

    public void init(){
        for(ItemTable itemTable : ItemTable.datas){
            itemTableMap.put(itemTable.getId(),itemTable);
        }
    }

    @Tx
    public void addItem(String userId,int itemId,int count){
        if(count <= 0){
            return;
        }
        Item item = dataService.selectObject(Item.class,"userId = ? and itemId=?",userId,itemId);
        if(item == null){
            item = new Item();
            item.setUserId(userId);
            item.setItemId(itemId);
            item.setCount(count);
            dataService.insert(item);
        }else{
            item.setCount(item.getCount() + count);
            dataService.update(item);
        }
    }
    @Tx
    public void decItem(String userId,int itemId,int count){
        if(count <= 0){
            return;
        }
        Item item = dataService.selectObject(Item.class,"userId = ? and itemId=?",userId,itemId);
        if(item == null || item.getCount() < count){
            throw new ToClientException("item count is not enough");
        }
        item.setCount(item.getCount() - count);
        dataService.update(item);
    }

    @Tx()
    @Request(opcode = MiGongOpcode.CSUseItem)
    public RetPacket useItem(Object clientData, Session session) throws Throwable{
        MiGongPB.CSUseItem useItem = MiGongPB.CSUseItem.parseFrom((byte[])clientData);
        ItemTable itemTable = itemTableMap.get(useItem.getItemId());
        if(itemTable == null){
            throw new ToClientException("item is not exist!");
        }
        UserMiGong userMiGong = miGongService.get(session.getAccountId());
        decItem(session.getAccountId(),useItem.getItemId(),useItem.getCount());

        Item.ItemType itemType = Item.ItemType.values()[itemTable.getItemtype()];
        switch (itemType){
            case Energy:
                userMiGong.setEnergy(userMiGong.getEnergy() + itemTable.getPara1());
                dataService.update(userMiGong);
                break;
            case AddSpeed:
                break;
            case AddTime:
                break;
            case MulBean:
                break;
            case ShowRoute:
                break;
        }
        if(itemType.isSkill()) {
            miGongService.useSkillItem(session.getAccountId(), itemType, itemTable);
        }

        MiGongPB.SCUseItem.Builder builder = MiGongPB.SCUseItem.newBuilder();
//        builder.setRet()
        return new RetPacketImpl(MiGongOpcode.SCUseItem, builder.build().toByteArray());
    }
}
