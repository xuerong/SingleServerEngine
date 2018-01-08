package com.migong.item;

import com.migong.MiGongService;
import com.migong.entity.UserMiGong;
import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.util.Util;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.ItemTable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    @EventListener(event = SysConstantDefine.Event_TableChange)
    public void onTableChange(EventData data){
        if(data.getData() == ItemTable.class){
            Map<Integer,ItemTable> itemTableMap = new HashMap<>();
            for(ItemTable itemTable : ItemTable.datas){
                itemTableMap.put(itemTable.getId(),itemTable);
            }
            this.itemTableMap = itemTableMap;
        }
    }

    public List<Item> getSkillItems(String userId){
        List<Item> list = dataService.selectList(Item.class,"userId=?",userId);
        List<Item> ret = new ArrayList<>();
        if(list != null) {
            for (Item item : list) {
                ItemTable itemTable = itemTableMap.get(item.getItemId());
                if (itemTable != null) {
                    if (Item.ItemType.values()[itemTable.getItemtype()].isSkill()) {
                        ret.add(item);
                    }
                }
            }
        }
        return ret;
    }
    @Tx
    public void addItems(String userId,String rewardString){
        Map<Integer, Integer> map = Util.split2Map(rewardString, Integer.class, Integer.class);
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            addItem(userId, entry.getKey(), entry.getValue());
        }
    }
    @Tx
    public void addItems(String userId,Map<Integer, Integer> map){
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            addItem(userId, entry.getKey(), entry.getValue());
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
    @Request(opcode = MiGongOpcode.CSGetItems)
    public RetPacket getItems(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetItems getItems = MiGongPB.CSGetItems.parseFrom((byte[])clientData);

        List<Item> items = dataService.selectList(Item.class,"userId=?",session.getAccountId());
        MiGongPB.SCGetItems.Builder builder = MiGongPB.SCGetItems.newBuilder();
        if(items != null){
            for(Item item : items){
                MiGongPB.PBItem.Builder itemBuilder = MiGongPB.PBItem.newBuilder();
                itemBuilder.setItemId(item.getItemId());
                itemBuilder.setCount(item.getCount());
                builder.addItems(itemBuilder);
            }
        }

        return new RetPacketImpl(MiGongOpcode.SCGetItems, builder.build().toByteArray());
    }
    @Tx()
    @Request(opcode = MiGongOpcode.CSUseItem)
    public RetPacket useItem(Object clientData, Session session) throws Throwable{
        MiGongPB.CSUseItem useItem = MiGongPB.CSUseItem.parseFrom((byte[])clientData);
        ItemTable itemTable = itemTableMap.get(useItem.getItem().getItemId());
        if(itemTable == null){
            throw new ToClientException("item is not exist!");
        }
        UserMiGong userMiGong = miGongService.get(session.getAccountId());
        decItem(session.getAccountId(),useItem.getItem().getItemId(),useItem.getItem().getCount());

        Item.ItemType itemType = Item.ItemType.values()[itemTable.getItemtype()];
        String ret = null;
        switch (itemType){
            case Energy:
                miGongService.checkAndAddEnergy(userMiGong,itemTable.getPara1());
                dataService.update(userMiGong);
                ret = String.valueOf(miGongService.getEnergyByRefresh(userMiGong)+";"+userMiGong.getEnergyUpdateTime());
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
            ret = miGongService.useSkillItem(session.getAccountId(), itemType, itemTable,useItem.getArgs());
        }

        MiGongPB.SCUseItem.Builder builder = MiGongPB.SCUseItem.newBuilder();
        if(ret != null){
            builder.setRet(ret);
        }
        return new RetPacketImpl(MiGongOpcode.SCUseItem, builder.build().toByteArray());
    }
}
