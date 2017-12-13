package com.migong.shop;

import com.migong.MiGongService;
import com.migong.entity.NewGuideType;
import com.migong.entity.UserMiGong;
import com.migong.item.ItemService;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.LocalizationMessage;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.tool.util.Util;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.ItemTable;
import com.table.PeckTable;
import com.table.UnitTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service(init = "init")
public class ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopService.class);
    private MiGongService miGongService;
    private DataService dataService;
    private ItemService itemService;

    private Map<Integer,ItemTable> itemTableMap = new HashMap<>();
    private Map<Integer,UnitTable> unitTableMap = new HashMap<>();
    private Map<Integer,PeckTable> peckTableMap = new HashMap<>();
    public void init(){
        for(ItemTable itemTable : ItemTable.datas){
            itemTableMap.put(itemTable.getId(),itemTable);
        }
        for(UnitTable unitTable : UnitTable.datas){
            unitTableMap.put(unitTable.getId(),unitTable);
        }
        for(PeckTable peckTable : PeckTable.datas){
            peckTableMap.put(peckTable.getId(),peckTable);
        }
    }

    @Request(opcode = MiGongOpcode.CSGoldBuy)
    public RetPacket goldBuy(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGoldBuy goldBuy = MiGongPB.CSGoldBuy.parseFrom((byte[])clientData);

        ShopType shopType = ShopType.values()[goldBuy.getType()];
        int gold = 0;
        switch (shopType){
            case Item:
                ItemTable itemTable = itemTableMap.get(goldBuy.getId());
                int needGold = itemTable.getPrice() * goldBuy.getNum();
                // 减金币
                gold = decGold(session.getAccountId(),needGold);
                // 加道具
                itemService.addItem(session.getAccountId(),itemTable.getId(),goldBuy.getNum());
                break;
            case Unit:
                UnitTable unitTable = unitTableMap.get(goldBuy.getId());
                needGold = unitTable.getPrice() * goldBuy.getNum();
                // 减金币
                gold = decGold(session.getAccountId(),needGold);
                // 加道具
                Map<Integer,Integer> map = Util.split2Map(unitTable.getItems(),Integer.class,Integer.class);
                for(Map.Entry<Integer,Integer> entry : map.entrySet()){
                    itemService.addItem(session.getAccountId(),entry.getKey(),entry.getValue() * goldBuy.getNum());
                }
                break;
            default:
                throw new MMException("shop type is error ,shop type = "+shopType);
        }


        MiGongPB.SCGoldBuy.Builder builder = MiGongPB.SCGoldBuy.newBuilder();
        builder.setGold(gold);
        builder.setSuccess(1);

        return new RetPacketImpl(MiGongOpcode.SCGoldBuy, builder.build().toByteArray());
    }

    @Request(opcode = MiGongOpcode.CSMoneyBuyBefore)
    public RetPacket moneyBuyBefore(Object clientData, Session session) throws Throwable{
        MiGongPB.CSMoneyBuyBefore goldBuy = MiGongPB.CSMoneyBuyBefore.parseFrom((byte[])clientData);
        log.info("");
//        goldBuy.getId()

        MiGongPB.SCMoneyBuyBefore.Builder builder = MiGongPB.SCMoneyBuyBefore.newBuilder();
        builder.setIsOk(1);
        return new RetPacketImpl(MiGongOpcode.SCMoneyBuyBefore, builder.build().toByteArray());
    }

    @Tx
    public int addGold(String userId,int delta){
        UserMiGong userMiGong = miGongService.get(userId);
        if(delta <= 0){
            return userMiGong.getGold();
        }

        userMiGong.setGold(userMiGong.getGold() + delta);
        dataService.update(userMiGong);
        return userMiGong.getGold();
    }
    @Tx
    public int decGold(String userId,int delta){
        UserMiGong userMiGong = miGongService.get(userId);
        if(delta <= 0){
            return userMiGong.getGold();
        }

        if(userMiGong.getGold() < delta){
            throw new ToClientException(LocalizationMessage.getText("goldNotEnough"));
        }
        userMiGong.setGold(userMiGong.getGold() - delta);
        dataService.update(userMiGong);
        return userMiGong.getGold();
    }

    public enum ShopType{
        Item, // 道具
        Unit, // 套装
        Peck // 礼包
    }
}
