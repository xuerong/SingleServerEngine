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
import com.mm.engine.framework.server.IdService;
import com.mm.engine.framework.tool.util.Util;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.table.ItemTable;
import com.table.PeckTable;
import com.table.UnitTable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service(init = "init")
public class ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopService.class);
    private MiGongService miGongService;
    private DataService dataService;
    private ItemService itemService;
    private IdService idService;

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
    @Tx
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
        MiGongPB.CSMoneyBuyBefore moneyBuyBefore = MiGongPB.CSMoneyBuyBefore.parseFrom((byte[])clientData);
        PeckTable peckTable = peckTableMap.get(moneyBuyBefore.getId());
        if(peckTable == null){
            throw new ToClientException("goods is not exist");
        }
        if(peckTable.getLimit()> 0){
            // 购买次数限制
        }
        log.info("money buy before , peck id = {},num= {} ",moneyBuyBefore.getId(),moneyBuyBefore.getNum());
//        goldBuy.getId()

        MiGongPB.SCMoneyBuyBefore.Builder builder = MiGongPB.SCMoneyBuyBefore.newBuilder();
        builder.setIsOk(1);
        return new RetPacketImpl(MiGongOpcode.SCMoneyBuyBefore, builder.build().toByteArray());
    }
    @Tx
    @Request(opcode = MiGongOpcode.CSMoneyBuy)
    public RetPacket moneyBuy(Object clientData, Session session) throws Throwable{
        MiGongPB.CSMoneyBuy moneyBuy = MiGongPB.CSMoneyBuy.parseFrom((byte[])clientData);
        PeckTable peckTable = peckTableMap.get(moneyBuy.getId());
        boolean success = false;
        int money = 0;
        try {
            if (peckTable == null) {
                throw new ToClientException("goods is not exist");
            }
            if (moneyBuy.getNum() <= 0) {
                throw new ToClientException("invalid param,num");
            }
            if (StringUtils.isEmpty(moneyBuy.getToken())) {
                throw new ToClientException("invalid param,token");
            }

            // TODO 去支付方验证token，并获取实际支付的钱，赋值给money
            boolean result = true;
            if (!result) {
                throw new ToClientException("order check fail");
            }
            money = peckTable.getPrice(); // 这个改为实际支付的钱
            if(money != peckTable.getPrice()){
                //
                log.error("money error! order money = {},peck price = {}",money,peckTable.getPrice());
            }
            if(money <= 0){
                throw new ToClientException("order check fail");
            }

            log.info("order check success,money = "+money);

            // 发放商品
            // 金币
            int gold = addGold(session.getAccountId(), peckTable.getGold() * moneyBuy.getNum());
            // 道具
            Map<Integer, Integer> map = Util.split2Map(peckTable.getItems(), Integer.class, Integer.class);
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                itemService.addItem(session.getAccountId(), entry.getKey(), entry.getValue() * moneyBuy.getNum());
            }
            success = true;
        }catch (Throwable e){
            throw e;
        }finally {
            // 添加订单记录
            Order order = new Order();
            order.setId(idService.acquireInt(Order.class));
            order.setGold(peckTable.getGold() * moneyBuy.getNum());
            order.setSuccess(success?1:0);
            order.setUserId(session.getAccountId());
            order.setItems(peckTable.getItems());
            order.setMoney(money);
            order.setNum(moneyBuy.getNum());
            order.setPeckId(peckTable.getId());
            order.setTime(new Timestamp(System.currentTimeMillis()));
            order.setToken(moneyBuy.getToken());
            dataService.insert(order);
        }
        //

        MiGongPB.SCMoneyBuy.Builder builder = MiGongPB.SCMoneyBuy.newBuilder();
        UserMiGong userMiGong = miGongService.get(session.getAccountId());
        builder.setGold(userMiGong.getGold());
        builder.setSuccess(success?1:0);
        return new RetPacketImpl(MiGongOpcode.SCMoneyBuy, builder.build().toByteArray());
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
