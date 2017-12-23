package com.migong;

import com.migong.entity.*;
import com.migong.item.Item;
import com.migong.item.ItemService;
import com.migong.item.PassRewardData;
import com.migong.map.CreateMap;
import com.migong.map.Element;
import com.migong.map.GetShortRoad;
import com.migong.shop.ShopService;
import com.mm.engine.framework.control.annotation.EventListener;
import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.annotation.Updatable;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.data.entity.account.Account;
import com.mm.engine.framework.data.entity.account.LogoutEventData;
import com.mm.engine.framework.data.entity.account.sendMessage.SendMessageService;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.data.sysPara.SysParaService;
import com.mm.engine.framework.data.tx.Tx;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.net.code.RetPacketImpl;
import com.mm.engine.framework.security.LocalizationMessage;
import com.mm.engine.framework.security.exception.ToClientException;
import com.mm.engine.framework.server.IdService;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.util.Util;
import com.protocol.MiGongOpcode;
import com.protocol.MiGongPB;
import com.sys.SysPara;
import com.table.ItemTable;
import com.table.MiGongPass;
import com.table.PeckTable;
import com.table.UnitTable;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * -source1.7问题可能到maven中设置
 *
 * 0fa7ef61f78d1ba3be659bd746e4aea8
 *
 * 功能：
 * 单人的：根据等级等获取地图，时间到的失败推送，结束的时候发送走过的路径校验，并记录
 * 多人的：请求匹配（放入匹配队列），匹配完成创建房间并推送，操作（移动，道具），位置同步和校验（全缓存），到达终点请求和同步，结束推送同步，房间心跳
 *
 * 同步方案：
 * 1、前端每次操作时，告诉服务器：（位置，方向，速度）
 * 2、服务器启动60帧每秒的循环，在循环中转发每个前端的操作：（位置，方向，速度，帧数）
 * 3、前端接收到服务器转发的各个前端操作的指令，独立计算
 * 4、开启其它线程用于操作校验
 * 5、开始和结束的时候也要发
 *
 * 道具：加速，瞬移，提示一定路段，
 *
 *
 *
 * 后续vip可以从如下几个点做：速度，体力，道具
 * 数据设计：
 * 系统配数：每日精力，四个星级的精力，无尽版速度，天梯速度，匹配等待时长，无尽版和匹配版的时长
 * 配数：
 * 关卡：id,size,time,speed,bean1,bean5,bean10,star1,star2,star3,star4,energy,reward(预留奖励道具)
 * 天梯：title,ladderScore
 * vip：speed,energy,item,energyRecovery
 *
 * 数据库：
 * 玩家：userId,unlimitedPass,unlimitedStar,starCount(启动的时候校验一下),vip,ladderScore,energy      ：无尽版排行按照unlimitedPass-unlimitedStar排行
 * 推图（已经过的）：userId,passId,star,useTime,score：重新打的时候，存储条件：星级大，或者星级相等分数大
 *
 *
 * 后面要做的（本着简单快速上线的原则）：
 * 一、游戏逻辑：
 * 要完成的：
 * 星星的数量改为3个
 * 1、优化：音乐，声音
 * 2、帮助
 * 3、无尽版规则，天梯积分规则
 * 4、英语的文字优化！
 * 5、打点统计，运营
 * 6、接google play支付和Facebook分享和Facebook登录
 * 以后要做的：
 * 3、vip系统
 * 4、微信等的登录
 * 5、好友系统，聊天系统
 * 6、多Vs多：自定义房间
 * 7、讨论群，网站，这个屌
 * 8、分享打的关卡，星级等。
 * 9、账号相关：名字和头像
 * 二、框架功能：
 * 1、服务器分配：要有一个main server用来分配服务器
 * 以后要做的
 * 1、断线重连
 * 三、接入相关：
 * 四、运营维护相关：
 * 1、部署服务器套件
 * 2、游戏更新：后端热更，前段热更
 * 3、数据监控：友盟？还是自己来？
 * 4、异常监控
 *
 *
 * 需要打点的数据：
 * 1玩家注册（新注册）
 * 2玩家登陆（日活跃，留存(1,3,7,14,30)）
 * 3玩家过的关卡，耗时，及其星数
 * 4无尽版，耗时，及其星数
 * 5点击参加匹配
 * 6进入对战
 *
 * 付费总值，金币获取和消费，付费用户，付费次数，付费率，ARPU，ARPPU，
 *
 *TODO 缓存问题，这里用的map做缓存，满了不会自动清除，这个要处理的
 *
 * 微信：
 * AppID：wx4441fcf39f0e24e0
 * AppSecret：ce51278665d747db197044ea5482d888
 *
 * 3344c1180ab5eed4f6aed6f2a002008b
 *
 *todo  listKey is Illegal : listKey = com.migong.entity.UserMiGong#list
 *
 * 关于主服务器：
 * 1、主服务器需要存在，且ip稳定，性能不足可以增加，但ip稳定
 * 2、主服务器主要功能如下：
 * 第一、作为玩家第一次登录注册，或者登录服务器数据被清理之后的登录
 * 第二、gm：包括对主服务器的gm和对每个服务器都要执行的gm
 * 第三、综合统计，包括主服务器本身要完成的，和主服务器要连接所有的数据库，然后做出的综合统计。
 * 第四、作为运营平台，可以连接任意一台服务器并执行gm，数据统计等。
 */
@Service(init = "init")
public class MiGongService {
    private static final Logger log = LoggerFactory.getLogger(MiGongService.class);
    static boolean debug = true;

    private MiGongRank miGongRank;
    private DataService dataService;
    private SendMessageService sendMessageService;
    private SysParaService sysParaService;
    private IdService idService;
    private ItemService itemService;
    private ShopService shopService;
    /**
     * 这个是玩家获取迷宫后缓存的该迷宫信息，在玩家过关的时候用来校验是否正常过关。同时在如下几种情况下要清除：
     * 1、玩家断开连接
     * 2、到点失败
     */
    ConcurrentHashMap<String,MiGongPassInfo> miGongPassInfoMap = new ConcurrentHashMap<>();
    /**
     * 匹配中的玩家，grade（段位）- （accountId-开始匹配时间）
     */
    ConcurrentHashMap<Integer,ConcurrentLinkedQueue<RoomUser>> matchingUsers = new ConcurrentHashMap<>();

    /**
     * 匹配时间超时的消息推送
     */
    ExecutorService matchOutTimeExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new MiGongThreadFactory("matchOutTime"));
    /**
     * 匹配成功的房间创建
     */
    ExecutorService roomCreateExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new MiGongThreadFactory("roomCreate"));

    /**
     * 玩家-房间
     */
    ConcurrentHashMap<String,MultiMiGongRoom> userRooms = new ConcurrentHashMap<>();
    /**
     * 玩家状态：有状态的时候放入，无状态的时候移除（掉线，退出房间等）
     * 这里只针对匹配，放置多个匹配
     */
    ConcurrentHashMap<String,RoomUser> roomUsers = new ConcurrentHashMap<>();


    private final Map<Integer,MiGongPass> passMap = new HashMap<>();
    public void init(){
        for(MiGongPass miGongPass : MiGongPass.datas){
            passMap.put(miGongPass.getId(), miGongPass);
        }
    }
    @Request(opcode = MiGongOpcode.CSBaseInfo)
    public RetPacket getBaseInfo(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());
        MiGongPB.SCBaseInfo.Builder builder = MiGongPB.SCBaseInfo.newBuilder();
        builder.setEnergy(getEnergyByRefresh(userMiGong));
        for(Map.Entry<String,String> sysPara : SysPara.paras.entrySet()){
            MiGongPB.PBSysPara.Builder sysParaBuilder = MiGongPB.PBSysPara.newBuilder();
            sysParaBuilder.setKey(sysPara.getKey());
            sysParaBuilder.setValue(sysPara.getValue());
            builder.addSysParas(sysParaBuilder);
        }
        Map<Integer,Integer> guideMap = Util.split2Map(userMiGong.getNewUserGuide(),Integer.class,Integer.class);
        for(NewGuideType guideType : NewGuideType.values()){
            MiGongPB.PBNewGuide.Builder guideBuilder = MiGongPB.PBNewGuide.newBuilder();
            guideBuilder.setId(guideType.getId());
            Integer step = guideMap.get(guideBuilder.getId());
            guideBuilder.setStep(step == null?0:step);
            builder.addNewGuide(guideBuilder);
        }
        builder.setOpenPass(userMiGong.getPass());
        builder.setGold(userMiGong.getGold());
        // 道具配表
        for(ItemTable itemTable : ItemTable.datas){
            MiGongPB.PBItemTable.Builder itemBuilder = MiGongPB.PBItemTable.newBuilder();
            itemBuilder.setId(itemTable.getId());
            itemBuilder.setItemType(itemTable.getItemtype());
            itemBuilder.setPara1(itemTable.getPara1());
            itemBuilder.setPara2(itemTable.getPara2());
            itemBuilder.setPrice(itemTable.getPrice());
            builder.addItemTable(itemBuilder);
        }
        // 套装配表
        for(UnitTable unitTable : UnitTable.datas){
            MiGongPB.PBUnitTable.Builder unitBuilder = MiGongPB.PBUnitTable.newBuilder();
            unitBuilder.setId(unitTable.getId());
            unitBuilder.setItems(unitTable.getItems());
            unitBuilder.setLimit(unitTable.getLimit());
            unitBuilder.setName(unitTable.getName());
            unitBuilder.setPrice(unitTable.getPrice());
            builder.addUnitTable(unitBuilder);
        }
        // 礼包配表
        for(PeckTable peckTable : PeckTable.datas){
            MiGongPB.PBPeckTable.Builder peckBuilder = MiGongPB.PBPeckTable.newBuilder();
            peckBuilder.setId(peckTable.getId());
            peckBuilder.setItems(peckTable.getItems());
            peckBuilder.setLimit(peckTable.getLimit());
            peckBuilder.setName(peckTable.getName());
            peckBuilder.setPrice(peckTable.getPrice());
            peckBuilder.setGold(peckTable.getGold());
            builder.addPeckTable(peckBuilder);
        }
        return new RetPacketImpl(MiGongOpcode.SCBaseInfo, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSNewGuideFinish)
    public RetPacket newGuideFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSNewGuideFinish finish = MiGongPB.CSNewGuideFinish.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        NewGuideType.setNewGuide(userMiGong,finish.getId(),finish.getStep());
        MiGongPB.SCNewGuideFinish.Builder builder = MiGongPB.SCNewGuideFinish.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCNewGuideFinish, builder.build().toByteArray());
    }

    /**
     * 获取玩家精力
     * @param userMiGong
     * @return 玩家精力
     */
    public int getEnergyByRefresh(UserMiGong userMiGong){
        if(!DateUtils.isSameDay(new Timestamp(userMiGong.getEnergyUpdateTime()),new Timestamp(System.currentTimeMillis()))){
            userMiGong.setEnergy(sysParaService.getInt(SysPara.energyPerDay));
            userMiGong.setEnergyUpdateTime(System.currentTimeMillis());
            dataService.update(userMiGong);
        }
        return userMiGong.getEnergy();
    }
    @Request(opcode = MiGongOpcode.CSGetMiGongLevel)
    public RetPacket getMiGongLevel(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetMiGongLevel getMiGongLevel = MiGongPB.CSGetMiGongLevel.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        //
        MiGongPB.SCGetMiGongLevel.Builder builder = MiGongPB.SCGetMiGongLevel.newBuilder();
        List<UserPass> userPasses =  dataService.selectList(UserPass.class,"userId=?",session.getAccountId());
        if(userPasses != null) {
            for (UserPass userPass : userPasses) {
                builder.addStarInLevel(userPass.getStar());
            }
        }
        builder.setPassCount(MiGongPass.datas.length);
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSGetPassReward)
    public RetPacket getPassReward(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetPassReward getPassReward = MiGongPB.CSGetPassReward.parseFrom((byte[])clientData);
        MiGongPass miGongPass = passMap.get(getPassReward.getPass());
        miGongPass.getReward();

        UserMiGong userMiGong = get(session.getAccountId());
        //
        MiGongPB.SCGetPassReward.Builder builder = MiGongPB.SCGetPassReward.newBuilder();
        builder.setEnergy(miGongPass.getEnergy());
        PassRewardData passRewardData = PassRewardData.parse(miGongPass.getReward());
        if(passRewardData != null){
            for(int i=0;i<4;i++){
                if(i == 4 && miGongPass.getStar4()<=0){
                    continue;
                }
                MiGongPB.PBPassReward.Builder passRewardBuilder = MiGongPB.PBPassReward.newBuilder();
                passRewardBuilder.setEnergy(passRewardData.getStarGold()[i]);
                passRewardBuilder.setGold(passRewardData.getStarEnergy()[i]);
                if(passRewardData.getItemReward() != null){
                    for(Map.Entry<Integer, int[]> entry :passRewardData.getItemReward().entrySet()){
                        MiGongPB.PBItem.Builder psi = MiGongPB.PBItem.newBuilder();
                        psi.setItemId(entry.getKey());
                        psi.setCount(entry.getValue()[i]);
                        passRewardBuilder.addItem(psi);
                    }
                }
                if(i == 0){
                    builder.setPassRewardStar1(passRewardBuilder);
                }else if(i == 1){
                    builder.setPassRewardStar2(passRewardBuilder);
                }else if(i == 3){
                    builder.setPassRewardStar3(passRewardBuilder);
                }else{
                    builder.setPassRewardStar4(passRewardBuilder);
                }
            }
        }
        return new RetPacketImpl(MiGongOpcode.SCGetPassReward, builder.build().toByteArray());
    }



    @Tx()
    @Request(opcode = MiGongOpcode.CSGetMiGongMap)
    public RetPacket getMap(Object clientData, Session session) throws Throwable{
        MiGongPB.CSGetMiGongMap getMiGongMap = MiGongPB.CSGetMiGongMap.parseFrom((byte[])clientData);
        UserMiGong userMiGong = get(session.getAccountId());
        // 当前等级和关卡
        MiGongPass miGongPass = passMap.get(getMiGongMap.getPass());
        checkLevelAndPass(miGongPass,userMiGong);
        if(miGongPassInfoMap.containsKey(session.getAccountId())){
            miGongPassInfoMap.remove(session.getAccountId());
            log.error("do CSGetMiGongMap but miGongPassInfoMap has data,accountId = {}",session.getAccountId());
        }
        // 消耗精力，还有无线模式
        checkAndDecrEnergy(userMiGong,miGongPass.getEnergy());
        //
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setPass(getMiGongMap.getPass());
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo.setBeanCount(miGongPass.getBean1(),miGongPass.getBean5(),miGongPass.getBean10());
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo,miGongPass.getSize(),miGongPass.getTime(),miGongPass.getSpeed()); // 这个方法后面直接去掉
        MiGongPB.SCGetMiGongMap.Builder builder = MiGongPB.SCGetMiGongMap.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//地图

        Map<Integer,Integer> guideMap = Util.split2Map(userMiGong.getNewUserGuide(),Integer.class,Integer.class);
        if(guideMap == null || !guideMap.containsKey(NewGuideType.Pass)) {
            builder.setRoute(getRoute(myMap,miGongPassInfo.getSize()));
        }

        List<Integer> integers = new ArrayList<>(miGongPassInfo.getSize()*miGongPassInfo.getSize());
        for(byte[] aa : miGongPassInfo.getCreateMap().getMap()){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);
        builder.setTime(miGongPassInfo.getTime());
        builder.setSpeed(miGongPassInfo.getSpeed());
        builder.setStart(miGongPassInfo.getStart().toInt(miGongPassInfo.getSize()));
        for(Bean bean : miGongPassInfo.getBeans()){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setPos(bean.toInt(miGongPassInfo.getSize()));
            beanBuilder.setScore(bean.getScore());
            builder.addBeans(beanBuilder);
        }
        builder.setEnergy(getEnergyByRefresh(userMiGong)); // 这个是剩余精力
        builder.setPass(miGongPass.getId());
        builder.setStar1(miGongPass.getStar1());
        builder.setStar2(miGongPass.getStar2());
        builder.setStar3(miGongPass.getStar3());
        builder.setStar4(miGongPass.getStar4());

//        System.out.println("miGongPassInfo.getEnd():"+miGongPassInfo.getEnd());
        builder.setEnd(miGongPassInfo.getEnd().toInt(miGongPassInfo.getSize()));

        List<Item> items = itemService.getSkillItems(session.getAccountId());
        for(Item item :items){
            MiGongPB.PBItem.Builder itemBuilder = MiGongPB.PBItem.newBuilder();
            itemBuilder.setItemId(item.getItemId());
            itemBuilder.setCount(item.getCount());
            builder.addItems(itemBuilder);
        }

        miGongPassInfoMap.put(session.getAccountId(),miGongPassInfo);
        byte[] sendData = builder.build().toByteArray();
        return new RetPacketImpl(MiGongOpcode.SCGetMiGongMap, sendData);
    }
    private String getRoute(CreateMap myMap,int size){
        Element[] road = new GetShortRoad(myMap, false).getRoad();
        StringBuilder sb = new StringBuilder();
        String sp = "";
        for(Element element : road){
            sb.append(sp).append(element.toInt(size));
            sp=";";
        }
        return sb.toString();
    }
    private void checkAndDecrEnergy(UserMiGong userMiGong,int delta){
        if(!debug) {
            int energy = getEnergyByRefresh(userMiGong);
            int after = energy - delta;
            if (after < 0) {
                throw new ToClientException(LocalizationMessage.getText("energyNotEnough"));
            }
            userMiGong.setEnergy(after);
            dataService.update(userMiGong);
        }
    }
    public UserMiGong get(String userId){
        UserMiGong userMiGong = dataService.selectObject(UserMiGong.class,"userId=?",userId);
        if(userMiGong == null){
            userMiGong = new UserMiGong();
            userMiGong.setUserId(userId);
            dataService.insert(userMiGong);
        }
        return userMiGong;
    }
    // 玩家使用道具
    public String useSkillItem(String userId, Item.ItemType itemType , ItemTable itemTable,String args){
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.get(userId);
        if(miGongPassInfo == null){
            log.warn("not in room,but use item ,item type = {},item id = {},userId = {}",itemType.ordinal(),itemTable.getId(),userId);
            return null;
        }
        String ret = null;
        switch (itemType){
            case AddTime:
                miGongPassInfo.setTime(miGongPassInfo.getTime()*(100+itemTable.getPara1())/100);
                break;
            case MulBean:
                miGongPassInfo.setMulBean(itemTable.getPara1());
                miGongPassInfo.setUseMulBeanStep(Integer.parseInt(args));
                break;
            case ShowRoute:
                ret = getRoute(miGongPassInfo.getCreateMap(),miGongPassInfo.getSize());
                break;
        }
        if(miGongPassInfo.getUseItems() == null){
            miGongPassInfo.setUseItems(new ArrayList<ItemTable>());
        }
        miGongPassInfo.getUseItems().add(itemTable);
        return ret;
    }
    @Tx()
    @Request(opcode = MiGongOpcode.CSPassFinish)
    public RetPacket passFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSPassFinish passFinish = MiGongPB.CSPassFinish.parseFrom((byte[])clientData);
        // 校验关卡
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.remove(session.getAccountId());
        if(miGongPassInfo == null || miGongPassInfo.getPass() != passFinish.getPass()){
            throw new ToClientException(LocalizationMessage.getText("InvalidParams"));
        }
        UserMiGong userMiGong = get(session.getAccountId());
        MiGongPass miGongPass = passMap.get(miGongPassInfo.getPass());
        checkLevelAndPass(miGongPass,userMiGong);
        boolean isSuccess = false;
        if(passFinish.getSuccess() > 0){
            // 校验
            List<Integer> routeList = passFinish.getRouteList();
            isSuccess = miGongPassInfo.getCreateMap().checkRouteWithoutSkill(routeList);
            if(!isSuccess){
                // 失败的时候打印路径和地图
//                log.error();
                StringBuilder sb = new StringBuilder();
                for(byte[] bytes : miGongPassInfo.getCreateMap().getMap()){
                    for(byte b :bytes){
                        sb.append(b).append(",");
                    }
                    sb.append("\n");
                }
                System.out.println(sb.toString());
                sb = new StringBuilder();
                int size = miGongPassInfo.getSize();
                for(Integer pos : routeList){
                    sb.append("(").append(pos/size).append(",").append(pos%size).append(")");
                }
                System.out.println(sb.toString());
            }
        }
        MiGongPB.SCPassFinish.Builder builder = MiGongPB.SCPassFinish.newBuilder();
        //
        if(isSuccess){
            // 根据路径和配置获取星级，，是否插入？是否更新？，更新userMiGong星级和关卡，
            int allScore = calScore(passFinish.getRouteList(),miGongPassInfo);
            int star = calStar(allScore,miGongPass);
            if(star > 0){
                MiGongPB.PBPassReward.Builder passReward = MiGongPB.PBPassReward.newBuilder();
                PassRewardData passRewardData = PassRewardData.parse(miGongPass.getReward());
                if(miGongPassInfo.getPass() > userMiGong.getPass()){
                    createAndInsertUserPass(session.getAccountId(),miGongPassInfo.getPass(),star,allScore,
                            (int)(System.currentTimeMillis() - miGongPassInfo.getStartTime().getTime())/1000);
                    userMiGong.setPass(passFinish.getPass());
                    userMiGong.setStarCount(userMiGong.getStarCount() + star);
                    dataService.update(userMiGong);
                    // 奖励：直接把对应星级的奖励给了
                    if(passRewardData != null) {
                        passReward.setGold(passRewardData.getStarGold()[star - 1]);
                        passReward.setEnergy(passRewardData.getStarEnergy()[star - 1]);
                        if(passRewardData.getItemReward() != null){
                            for(Map.Entry<Integer,int[]> entry : passRewardData.getItemReward().entrySet()){
                                MiGongPB.PBItem.Builder passRewardItem = MiGongPB.PBItem.newBuilder();
                                passRewardItem.setItemId(entry.getKey());
                                passRewardItem.setCount(entry.getValue()[star - 1]);
                                passReward.addItem(passRewardItem);
                            }
                        }
                        builder.setPassReward(passReward);
                    }
                }else{
                    UserPass userPass = dataService.selectObject(UserPass.class,"userId=? and passId=?",session.getAccountId(),miGongPassInfo.getPass());
                    if(userPass == null){
                        log.error("user pass is not exit ,userId  ={},passId = {}",session.getAccountId(),miGongPassInfo.getPass());
                        userPass = createAndInsertUserPass(session.getAccountId(),miGongPassInfo.getPass(),star,allScore,
                                (int)(System.currentTimeMillis() - miGongPassInfo.getStartTime().getTime())/1000);
                        // 查出所有的userpass，把所有的star加起来，加入玩家
                    }else {
                        if (star > userPass.getStar()) {
                            userMiGong.setStarCount(userMiGong.getStarCount() + (star - userPass.getStar()));
                            dataService.update(userMiGong);
                        }
                        if (star > userPass.getStar() || (star == userPass.getStar() && allScore > userPass.getScore())) {
                            userPass.setScore(allScore);
                            userPass.setStar(star);
                            dataService.update(userPass);
                        }
                        if (star > userPass.getStar()) {
                            // 奖励：直接把对应星级的奖励给了
                            if(passRewardData != null) {
                                passReward.setGold(passRewardData.getStarGold()[star - 1] - passRewardData.getStarGold()[userPass.getStar() - 1]);
                                passReward.setEnergy(passRewardData.getStarEnergy()[star - 1] -  passRewardData.getStarEnergy()[userPass.getStar() - 1]);
                                if(passRewardData.getItemReward() != null){
                                    for(Map.Entry<Integer,int[]> entry : passRewardData.getItemReward().entrySet()){
                                        MiGongPB.PBItem.Builder passRewardItem = MiGongPB.PBItem.newBuilder();
                                        passRewardItem.setItemId(entry.getKey());
                                        passRewardItem.setCount(entry.getValue()[star - 1] - entry.getValue()[userPass.getStar() - 1]);
                                        passReward.addItem(passRewardItem);
                                    }
                                }
                                builder.setPassReward(passReward);
                            }
                            // ------
                        }
                    }
                }
                // 添加进玩家的身上
                if(passReward.getGold() > 0){
                    shopService.addGold(userMiGong.getUserId(),passReward.getGold());
                }
                if(passReward.getEnergy() > 0){
                    userMiGong.setEnergy(userMiGong.getEnergy() + passReward.getEnergy());
                    dataService.update(userMiGong);
                }
                if(passReward.getItemCount() > 0){
                    for(MiGongPB.PBItem passRewardItem : passReward.getItemList()){
                        itemService.addItem(session.getAccountId(),passRewardItem.getItemId(),passRewardItem.getCount());
                    }
                }
                //

            }else{
                isSuccess = false;
            }
        }

        builder.setOpenLevel(1);
        builder.setOpenPass(userMiGong.getPass());
        builder.setSuccess(isSuccess?1:0);

        return new RetPacketImpl(MiGongOpcode.SCGetMiGongLevel, builder.build().toByteArray());
    }
    private UserPass createAndInsertUserPass(String userId,int passId,int star,int allScore,int useTime){
        UserPass userPass = new UserPass();
        userPass.setUserId(userId);
        userPass.setPassId(passId);
        userPass.setStar(star);
        userPass.setScore(allScore);
        userPass.setUseTime(useTime);
        dataService.insert(userPass);
        return userPass;
    }
    private int calScore(List<Integer> routes,MiGongPassInfo miGongPassInfo){
        Map<Integer,Integer> posForBean = new HashedMap();
        int step=0;
        for(Integer pos : routes){
            posForBean.putIfAbsent(pos,step++);
        }
        int size = miGongPassInfo.getSize();
        int allScore = 0;
        for(Bean bean : miGongPassInfo.getBeans()){
            Integer s = posForBean.get(bean.getX() * size + bean.getY());
            if(s != null) {
                if(miGongPassInfo.getMulBean()>0 && s >= miGongPassInfo.getUseMulBeanStep()) {
                    allScore += bean.getScore() * (1+miGongPassInfo.getMulBean()); // 技能，都的加倍
                }else{
                    allScore += bean.getScore();
                }
            }
        }
        return allScore;
    }
    private int calStar(int score,MiGongPass miGongPass){
        return calStar(score,miGongPass.getStar1(),miGongPass.getStar2(),miGongPass.getStar3(),miGongPass.getStar4());
    }
    private int calStar(int score,int star1,int star2,int star3,int star4){
        if(score < star1){
            return 0;
        }else if(star2<=0 || score < star2){
            return 1;
        }else if(star3<=0 || score < star3){
            return 2;
        }else if(star4<=0 || score < star4){
            return 3;
        }
        return 4;
    }

    @Request(opcode = MiGongOpcode.CSUnlimitedInfo)
    public RetPacket unlimitedInfo(Object clientData, Session session) throws Throwable{
        // 判断无尽模式是否打开，
        UserMiGong userMiGong = get(session.getAccountId());
        if(userMiGong.getPass() < sysParaService.getInt(SysPara.openUnlimited)){
            throw new ToClientException(LocalizationMessage.getText("unlimitedNotOpen",sysParaService.getInt(SysPara.openUnlimited)));
        }
        MiGongPB.SCUnlimitedInfo.Builder builder = MiGongPB.SCUnlimitedInfo.newBuilder();
        builder.setPass(userMiGong.getUnlimitedPass());
        builder.setStar(userMiGong.getUnlimitedStar());
        builder.setRank(miGongRank.getUnlimitedRank(userMiGong.getUserId()));// 排行系统

        List<UserMiGong> rank = miGongRank.getUnlimitedFront();
        int i = 1;
        for(UserMiGong rankMiGong : rank){
            MiGongPB.PBUnlimitedRankInfo.Builder info = MiGongPB.PBUnlimitedRankInfo.newBuilder();
            info.setPass(rankMiGong.getUnlimitedPass());
            info.setStar(rankMiGong.getUnlimitedStar());
            info.setRank(i++);
            info.setUserId(rankMiGong.getUserId());
            info.setUserName(dataService.selectObject(Account.class,"id=?",rankMiGong.getUserId()).getName());
            builder.addUnlimitedRankInfo(info);
        }
        return new RetPacketImpl(MiGongOpcode.SCUnlimitedInfo, builder.build().toByteArray());
    }
    @Request(opcode = MiGongOpcode.CSUnlimitedGo)
    public RetPacket unlimitedGo(Object clientData, Session session) throws Throwable{
        // 判断无尽模式是否打开，
        UserMiGong userMiGong = get(session.getAccountId());
        if(userMiGong.getPass() < sysParaService.getInt(SysPara.openUnlimited)){
            throw new ToClientException(LocalizationMessage.getText("unlimitedNotOpen",sysParaService.getInt(SysPara.openUnlimited)));
        }
        // 消耗精力
        checkAndDecrEnergy(userMiGong,sysParaService.getInt(SysPara.unlimitedEnergy));
        MiGongPassInfo miGongPassInfo = new MiGongPassInfo();
        miGongPassInfo.setBeanCount(40,5,1);
        miGongPassInfo.setStartTime(new Timestamp(System.currentTimeMillis()));
        miGongPassInfo = miGongParamsByDifficulty(miGongPassInfo,10,400,sysParaService.getInt(SysPara.unlimitedSpeed));
        MiGongPB.SCUnlimitedGo.Builder builder = MiGongPB.SCUnlimitedGo.newBuilder();

        CreateMap myMap=miGongPassInfo.getCreateMap();							//地图

        List<Integer> integers = new ArrayList<>(miGongPassInfo.getSize()*miGongPassInfo.getSize());
        for(byte[] aa : miGongPassInfo.getCreateMap().getMap()){
            for(byte bb : aa){
                integers.add((int)bb);
            }
        }
        builder.addAllMap(integers);
        builder.setSpeed(miGongPassInfo.getSpeed());
        builder.setEnd(miGongPassInfo.getEnd().toInt(miGongPassInfo.getSize()));
        builder.setStart(miGongPassInfo.getStart().toInt(miGongPassInfo.getSize()));
        builder.setTime(miGongPassInfo.getTime());
        for(Bean bean : miGongPassInfo.getBeans()){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setScore(bean.getScore());
            beanBuilder.setPos(bean.toInt(miGongPassInfo.getSize()));
            builder.addBeans(beanBuilder);
        }
        builder.setStar1(10);// todo 4Stars
        builder.setStar2(20);
        builder.setStar3(30);
        builder.setStar4(40);
        builder.setPass(userMiGong.getUnlimitedPass() + 1);
        builder.setEnergy(getEnergyByRefresh(userMiGong));// 精力，系统参数

        miGongPassInfoMap.put(session.getAccountId(),miGongPassInfo);

        return new RetPacketImpl(MiGongOpcode.SCUnlimitedGo, builder.build().toByteArray());
    }

    @Request(opcode = MiGongOpcode.CSUnlimitedFinish)
    public RetPacket unlimitedFinish(Object clientData, Session session) throws Throwable{
        MiGongPB.CSUnlimitedFinish unlimitedFinish = MiGongPB.CSUnlimitedFinish.parseFrom((byte[])clientData);
        // 校验关卡
        MiGongPassInfo miGongPassInfo = miGongPassInfoMap.remove(session.getAccountId());
        if(miGongPassInfo == null){
            throw new ToClientException(LocalizationMessage.getText("InvalidParams"));
        }
        UserMiGong userMiGong = get(session.getAccountId());

        boolean isSuccess = false;
        if(unlimitedFinish.getSuccess() > 0){
            // 校验
            List<Integer> routeList = unlimitedFinish.getRouteList();
            isSuccess = miGongPassInfo.getCreateMap().checkRouteWithoutSkill(routeList);
        }
        //
        if(isSuccess){
            int allScore = calScore(unlimitedFinish.getRouteList(),miGongPassInfo);
            int star = calStar(allScore,10,20,30,40);
            if(star > 0){
                userMiGong.setUnlimitedPass(userMiGong.getUnlimitedPass()+1);
                userMiGong.setUnlimitedStar(userMiGong.getUnlimitedStar() + star);
                dataService.update(userMiGong);
                miGongRank.putUnlimited(userMiGong);
            }else{
                isSuccess = false;
            }
        }
        MiGongPB.SCUnlimitedFinish.Builder builder = MiGongPB.SCUnlimitedFinish.newBuilder();
        builder.setOpenPass(userMiGong.getUnlimitedPass());
        builder.setSuccess(isSuccess?1:0);
        return new RetPacketImpl(MiGongOpcode.SCUnlimitedFinish, builder.build().toByteArray());
    }

    /**
     * 每分钟检查一次，去掉过期的MiGongPassInfo
     * 注意：要过期一段时间，否则，有些是延时20s
     * @param interval
     */
    @Updatable(isAsynchronous = true,cycle = 60000)
    public void checkMiGongPassInfo(int interval){
        long currentTime = System.currentTimeMillis();
        for(Map.Entry<String,MiGongPassInfo> entry : miGongPassInfoMap.entrySet()){
            MiGongPassInfo miGongPassInfo = entry.getValue();
            if((miGongPassInfo.getTime()+20)*1000 < currentTime - miGongPassInfo.getStartTime().getTime()){
                miGongPassInfoMap.remove(entry.getKey());
                log.warn("mi gong info remove by checkMiGongPassInfo,uid = {}",entry.getKey());
            }
        }
    }
    /**
     * 检车对应关卡是否可以进入
     * @param userMiGong
     * @throws Throwable
     */
    private void checkLevelAndPass(MiGongPass miGongPass,UserMiGong userMiGong) throws Throwable{
        if(miGongPass == null || miGongPass.getId() < 1){
            throw new ToClientException(LocalizationMessage.getText("InvalidParams"));
        }
        if(miGongPass.getId() - userMiGong.getPass() > 1){
            throw new ToClientException(LocalizationMessage.getText("InvalidParams"));
        }
    }
    /**
     * 根据难度，获取迷宫的大小和开门数，额，后面做成配置也好
     * 大小和开门数相对于难度的系数
     * 难度与时间的系数
     * 速度
     * 1-15,2-20,
     * @param miGongPassInfo
     * @return size,door,time(s),speed
     */
    private MiGongPassInfo miGongParamsByDifficulty(MiGongPassInfo miGongPassInfo,int size,int time,int speed){
        size+=1;
        int door = 0;
        miGongPassInfo.setSize(size);
        miGongPassInfo.setDoor(door);
        Element startElement = new Element(1,1); // todo 开始和结束可以考虑改成随机
        miGongPassInfo.setStart(startElement);
        Element endElement = new Element(size-1,size-1);
        miGongPassInfo.setEnd(endElement);
        CreateMap createMap = new CreateMap(size-1,size-1,startElement,endElement);
        miGongPassInfo.setCreateMap(createMap);
        byte[][] map=createMap.getMap();											//获取地图数组
        // 拆墙
        for(int i=0;i<door;i++){
            int x=(int)(Math.random()*(size-2))+1;					//1---tr-1
            int y=(int)(Math.random()*(size-2))+1;					//1---td-1
            map[x][y]=0;
        }
        miGongPassInfo.setTime(time);
        miGongPassInfo.setSpeed(speed);

        //生成豆子的位置
        Bean[] beans = createBeans(size,miGongPassInfo.getBean1(),miGongPassInfo.getBean5(),miGongPassInfo.getBean10()); // todo 创建豆子pass和unlimited要根据配置
        miGongPassInfo.setBeans(beans);

        return miGongPassInfo;
    }

    /**
     * 生成豆子的逻辑，目前定为，中间一个10分的，四边四个五分的，40个1分的
     * @return
     */
    private Bean[] createBeans(int size,int bean1,int bean5,int bean10){
        // TODO 这里暂时没有处理bean1和bean5，因为还包含随机的问题
        int bean10Count = 1;
        int bean5Count = 4;
        int bean1Count = bean1;
        Bean[] ret = new Bean[bean10Count + bean5Count + bean1Count];
        ret[0] = new Bean(size/2,size/2,10);

        ret[1] = new Bean(size/2,1,5);
        ret[2] = new Bean(size/2,size-1,5);
        ret[3] = new Bean(1,size/2,5);
        ret[4] = new Bean(size-1,size/2,5);

        Random random = new Random(System.currentTimeMillis());
        Set<Integer> hasCreatePos = new HashSet<>(ret.length);
        hasCreatePos.add((ret[0].getX()-1)*(size - 1) + (ret[0].getY()-1));
        hasCreatePos.add((ret[1].getX()-1)*(size - 1) + (ret[1].getY()-1));
        hasCreatePos.add((ret[2].getX()-1)*(size - 1) + (ret[2].getY()-1));
        hasCreatePos.add((ret[3].getX()-1)*(size - 1) + (ret[3].getY()-1));
        hasCreatePos.add((ret[4].getX()-1)*(size - 1) + (ret[4].getY()-1));

        if(bean1Count >= (size-1)*(size-1) - 9){ // bean1数量太多
            log.error("bean1 is too much , bean1 count = {},size = {},use bean1 = {}",bean1Count,size,(size-1)*(size-1)-10);
            bean1Count = (size-1)*(size-1)-10;
        }

        for(int i =0;i<bean1Count;i++){
            int posInt = random.nextInt((size - 1)*(size-1));

            if(hasCreatePos.contains(posInt)){ // 已经生成过的
                i--;
            }else {
                int x = posInt/(size - 1);
                int y = posInt%(size - 1);
                if((x == 0 && (y == 0 || y == (size - 2))) || (x == (size - 2) && (y == 0 || y == (size - 2)))){ // 不能在四个角上，因为角上会是出口和入口
                    i--;
                    continue;
                }
                ret[i+5] = new Bean(x + 1,y+1,1);
                hasCreatePos.add(posInt);
            }
        }
        return ret;
    }

    @Request(opcode = MiGongOpcode.CSSendWalkingRoute)
    public RetPacket walkingRoute(Object clientData, Session session) throws Throwable{
        MiGongPB.CSSendWalkingRoute csSendWalkingRoute = MiGongPB.CSSendWalkingRoute.parseFrom((byte[])clientData);
        List<Integer> list = csSendWalkingRoute.getRouteList();

        // TODO 校验

        MiGongPB.SCSendWalkingRoute.Builder builder = MiGongPB.SCSendWalkingRoute.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCSendWalkingRoute, builder.build().toByteArray());
    }

    @EventListener(event = SysConstantDefine.Event_AccountLogout)
    public void onAccountLogout(EventData data){
        // 清理缓存的miGongPassInfoMap   清理信息
        final LogoutEventData logoutEventData = (LogoutEventData)data.getData();
        miGongPassInfoMap.remove(logoutEventData.getSession().getAccountId());
        // todo 从Matching的queue中移除，如果是房间最后一个，移除房间
        RoomUser roomUser = roomUsers.remove(logoutEventData.getSession().getAccountId());
        if(roomUser != null){
            // 不用移除queue，那个session已经失效了
        }
        MultiMiGongRoom room = userRooms.remove(logoutEventData.getSession().getAccountId());
        if(room != null) {
            room.userLogout(logoutEventData.getSession().getAccountId());
        }
    }
    /////////////////////////////////////////////////////////////联网对战
    /**
     * 获取天梯信息
     */
    @Request(opcode = MiGongOpcode.CSGetOnlineInfo)
    public RetPacket getOnlineInfo(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());

        if(userMiGong.getPass() < sysParaService.getInt(SysPara.openPvp)){
            throw new ToClientException(LocalizationMessage.getText("onlineNotOpen",sysParaService.getInt(SysPara.openPvp)));
        }

        LadderTitle ladderTitle = LadderTitle.getLadderByScore(userMiGong.getLadderScore());

        MiGongPB.SCGetOnlineInfo.Builder builder = MiGongPB.SCGetOnlineInfo.newBuilder();
        builder.setScore(userMiGong.getLadderScore());
        builder.setRank(miGongRank.getLadderRank(session.getAccountId()));
        builder.setTitle(ladderTitle.getTitle());

        List<UserMiGong> userMiGongs = miGongRank.getLadderFront();
        int rank = 1;
        for(UserMiGong um : userMiGongs){
            MiGongPB.PBOnlineRankInfo.Builder rankInfo = MiGongPB.PBOnlineRankInfo.newBuilder();
            LadderTitle lt = LadderTitle.getLadderByScore(um.getLadderScore());
            rankInfo.setTitle(lt.getTitle());
            rankInfo.setRank(rank++);
            rankInfo.setScore(um.getLadderScore());
            rankInfo.setName(dataService.selectObject(Account.class,"id=?",um.getUserId()).getName()); // // TODO:  后面要把玩家名字也存储在UserMiGong中，修改通过事件
            rankInfo.setUserId(um.getUserId());
            builder.addRankInfos(rankInfo);
        }
        return new RetPacketImpl(MiGongOpcode.SCGetOnlineInfo, builder.build().toByteArray());
    }

    /**
     *匹配对战 ：把玩家放入匹配队列，查看队列大小，大于匹配数则开新线程开房间，
     */
    @Request(opcode = MiGongOpcode.CSMatching)
    public RetPacket matching(Object clientData, Session session) throws Throwable{
        UserMiGong userMiGong = get(session.getAccountId());
        // todo 该玩家是否开启匹配模式
        if(userMiGong.getPass() < sysParaService.getInt(SysPara.openPvp)){
            throw new ToClientException(LocalizationMessage.getText("onlineNotOpen",sysParaService.getInt(SysPara.openPvp)));
        }
        // todo 该玩家没有在排队也没有在房间
        RoomUser roomUser = roomUsers.get(session.getAccountId());
        if(roomUser == null){
            roomUser = new RoomUser(session,System.currentTimeMillis());
            roomUser.setUserState(RoomUser.UserState.Matching);
            roomUsers.put(session.getAccountId(), roomUser);
            ConcurrentLinkedQueue<RoomUser> queue = getMatchingQueue(scoreToGrade(userMiGong.getLadderScore()));
            queue.offer(roomUser);
        }else if(roomUser.getUserState() == RoomUser.UserState.Cancel || roomUser.getUserState() == RoomUser.UserState.None){
            roomUser.setUserState(RoomUser.UserState.Matching);
        }else{
            throw new ToClientException(LocalizationMessage.getText("canNotMatch",roomUser.getUserState().getDescribe()));
        }
        MiGongPB.SCMatching.Builder builder = MiGongPB.SCMatching.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCMatching, builder.build().toByteArray());
    }
    /**
     *匹配对战 ：把玩家放入匹配队列，查看队列大小，大于匹配数则开新线程开房间，
     */
    @Request(opcode = MiGongOpcode.CSCancelMatching)
    public RetPacket cancelMatching(Object clientData, Session session) throws Throwable{
        RoomUser roomUser = roomUsers.get(session.getAccountId());
        if(roomUser != null){
            roomUser.setUserState(RoomUser.UserState.Cancel);
        }
        MiGongPB.SCCancelMatching.Builder builder = MiGongPB.SCCancelMatching.newBuilder();

        return new RetPacketImpl(MiGongOpcode.SCCancelMatching, builder.build().toByteArray());
    }

    /**
     * 玩家移动
     */
    @Request(opcode = MiGongOpcode.CSMove)
    public RetPacket move(Object clientData, Session session) throws Throwable{
        MiGongPB.CSMove move = MiGongPB.CSMove.parseFrom((byte[])clientData);
//        int dir = move.getDir();
        int speed = move.getSpeed();
        // 操作
        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException(LocalizationMessage.getText("roomNotExist"));
        }
        room.userMove(session.getAccountId(),move.getPosX(),move.getPosY(),move.getDirX(),move.getDirY(),speed);

        MiGongPB.SCMove.Builder builder = MiGongPB.SCMove.newBuilder();
        return new RetPacketImpl(MiGongOpcode.CSMove, builder.build().toByteArray());
    }

    /**
     * 玩家吃豆
     */
    @Request(opcode = MiGongOpcode.CSEatBean)
    public RetPacket eatBean(Object clientData, Session session) throws Throwable{

        MiGongPB.CSEatBean eatBean = MiGongPB.CSEatBean.parseFrom((byte[])clientData);
        int pos = eatBean.getBeanPos();

        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException(LocalizationMessage.getText("roomNotExist"));
        }
        room.eatBean(session.getAccountId(),pos);
//        RoomUser roomUser = room.getRoomUser(session.getAccountId());
        // todo 开启一个新线程进行校验操作正误
//        roomUser.addBean(pos);

        MiGongPB.SCEatBean.Builder builder = MiGongPB.SCEatBean.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCEatBean, builder.build().toByteArray());
    }

    /**
     * 玩家抵达终点，要判断游戏结束
     */
    @Request(opcode = MiGongOpcode.CSArrived)
    public RetPacket arrived(Object clientData, Session session) throws Throwable{
        MiGongPB.CSArrived arrived = MiGongPB.CSArrived.parseFrom((byte[])clientData);
        int pos = arrived.getPos();
        // todo 校验pos是否正确，并校验玩家情况
        MultiMiGongRoom room = userRooms.get(session.getAccountId());
        if(room == null){
            throw new ToClientException(LocalizationMessage.getText("roomNotExist"));
        }
        room.userArrived(session.getAccountId());

        MiGongPB.SCArrived.Builder builder = MiGongPB.SCArrived.newBuilder();
        return new RetPacketImpl(MiGongOpcode.SCEatBean, builder.build().toByteArray());
    }

    /**
     * 每秒执行一次匹配，如果小于匹配数量，检查等待时间
     * @param interval
     */
    @Updatable(isAsynchronous = true,cycle = 1000)
    public void doMatching(int interval){
        if(matchingUsers.size() > 1000){
            log.warn("grade count is too much! size = {}",matchingUsers.size());
        }
        long currentTime = System.currentTimeMillis();
        for(Map.Entry<Integer,ConcurrentLinkedQueue<RoomUser>> entry : matchingUsers.entrySet()){
            ConcurrentLinkedQueue<RoomUser> queue = entry.getValue();
            List<RoomUser> roomUserList = new ArrayList<>();
            while (!queue.isEmpty()){
                RoomUser roomUser = queue.poll();
                if((currentTime - roomUser.getBeginTime())/1000 > sysParaService.getInt(SysPara.matchWaitTime)){
                    roomUsers.remove(roomUser.getSession().getAccountId());
                    matchOutTimeExecutor.execute(new SendMatchFailRunnable(roomUser));
                }else if(!roomUser.getSession().isAvailable()){
                    log.warn("session is not available , user maybe logout");
                    roomUsers.remove(roomUser.getSession().getAccountId());
                }else{
                    RoomUser.UserState userState = roomUsers.get(roomUser.getSession().getAccountId()).getUserState();
                    if(userState == RoomUser.UserState.Matching) {
                        // 加入匹配
                        roomUserList.add(roomUser);
                        if (roomUserList.size() >= MiGongRoom.USER_COUNT) {
                            // 创建房间
                            roomCreateExecutor.execute(new CreateRoomRunnable(entry.getKey(), roomUserList));
                            // 创建新的容器
                            roomUserList = new ArrayList<>();
                        }
                    }else{
                        roomUsers.remove(roomUser.getSession().getAccountId());
//                        log.error("user state is not matching,user state = "+userState);
                    }
                }
            }
            if(roomUserList.size() > 0){
                for(RoomUser roomUser : roomUserList){
                    queue.offer(roomUser); // 放回去
                }
            }
        }
    }
    class SendMatchFailRunnable implements Runnable{
        RoomUser roomUser;
        protected SendMatchFailRunnable(RoomUser roomUser){
            this.roomUser = roomUser;
        }
        @Override
        public void run() {
            MiGongPB.SCMatchingFail.Builder builder = MiGongPB.SCMatchingFail.newBuilder();
            sendMessageService.sendMessage(roomUser.getSession().getAccountId(), MiGongOpcode.SCMatchingFail,builder.build().toByteArray());
        }
    }
    class CreateRoomRunnable implements Runnable{
        private int grade;
        private List<RoomUser> roomUserList;
        protected  CreateRoomRunnable(int grade,List<RoomUser> roomUserList){
            this.grade = grade;
            this.roomUserList = roomUserList;
        }
        @Override
        public void run() {
            // 创建房间
            createRoom(grade, roomUserList);
        }
    }
    /**
     * 根据段位创建迷宫房间
     * @return
     */
    private void createRoom(int grade,final List<RoomUser> roomUserList){
        int size = sysParaService.getRandomInt(SysPara.ladderSize);
        int door = 0;
        Element startElement = new Element(1,1); // 这个在这里没有用
        Element endElement = new Element(size-1,size-1);
        //
        CreateMap createMap = new CreateMap(size-1,size-1,startElement,endElement);

        MultiMiGongRoom multiMiGongRoom = new MultiMiGongRoom(grade,createMap, size,
                sysParaService.getRandomInt(SysPara.ladderTime),sysParaService.getRandomInt(SysPara.ladderSpeed),roomUserList,this);

//        List<MiGongPB.PBOtherInfo> otherInfoList = new ArrayList<>();
//        for(RoomUser roomUser : roomUserList){
//            MiGongPB.PBOtherInfo.Builder ob = MiGongPB.PBOtherInfo.newBuilder();
//            ob.setStart(startElement.toInt(size));
//            ob.setEnd(endElement.toInt(size));
//            ob.setUserId(roomUser.getSession().getAccountId());
//            Account account = dataService.selectObject(Account.class,"id=?", roomUser.getSession().getAccountId());
//            ob.setUserName(account.getName());
//            otherInfoList.add(ob.build());
//        }
        MiGongPB.SCMatchingSuccess.Builder builder = MiGongPB.SCMatchingSuccess.newBuilder();

        for(byte[] aa : createMap.getMap()){
            for(byte b : aa){
                builder.addMap((int)b);
            }
        }
        // 豆子
        Bean[] beans = createBeans(size,sysParaService.getRandomInt(SysPara.ladderStar1),4,10);
        Map<Integer,Bean> beanMap = new HashMap<>();
        for(Bean bean : beans){
            beanMap.put(bean.toInt(size),bean);
        }
        multiMiGongRoom.setBeans(beanMap);
        for(Bean bean : beans){
            MiGongPB.PBBeanInfo.Builder beanBuilder = MiGongPB.PBBeanInfo.newBuilder();
            beanBuilder.setPos(bean.toInt(size));
            beanBuilder.setScore(bean.getScore());
            builder.addBeans(beanBuilder);
        }
        builder.setTime(multiMiGongRoom.getTime());
        builder.setSpeed(multiMiGongRoom.getSpeed()); // 速度
        int index = 0;
        for(RoomUser roomUser : roomUserList){
            userRooms.put(roomUser.getSession().getAccountId(),multiMiGongRoom);
            //
            roomUser.setUserState(RoomUser.UserState.Playing);
            // 推送

            List<MiGongPB.PBOtherInfo> otherInfoList = new ArrayList<>();
            for(RoomUser roomUser2 : roomUserList){
                if(roomUser2 == roomUser){
                    builder.setStart(roomUser.getIn().toInt(size));
                    builder.setEnd(roomUser.getOut().toInt(size));
                    continue;
                }
                MiGongPB.PBOtherInfo.Builder ob = MiGongPB.PBOtherInfo.newBuilder();
                ob.setStart(roomUser2.getIn().toInt(size));
                ob.setEnd(roomUser2.getOut().toInt(size));
                ob.setUserId(roomUser2.getSession().getAccountId());
                Account account = dataService.selectObject(Account.class,"id=?", roomUser2.getSession().getAccountId());
                ob.setUserName(account.getName());
                otherInfoList.add(ob.build());
            }

//            MiGongPB.PBOtherInfo otherInfo = otherInfoList.remove(index++);
            builder.clearOtherInfos();
            builder.addAllOtherInfos(otherInfoList);
//            otherInfoList.add(otherInfo);
//            System.out.println("send matchingsuccess");
            roomUser.getSession().getMessageSender().sendMessage(MiGongOpcode.SCMatchingSuccess,builder.build().toByteArray());
        }
        multiMiGongRoom.start();
    }

    // 房间结束的时候回回调这个函数，来清除房间信息
    public void multiRoomOver(MultiMiGongRoom room){
        for(RoomUser roomUser : room.getRoomUsers().values()){
            // todo 调整天梯积分:还要更改积分规则
            if(roomUser.isSuccess()){
                UserMiGong userMiGong = dataService.selectObject(UserMiGong.class,"userId=?",roomUser.getSession().getAccountId());
                userMiGong.setLadderScore(userMiGong.getLadderScore() + MultiMiGongRoom.USER_COUNT - roomUser.getRoomRank());
                dataService.update(userMiGong);
                // 修改排名
                miGongRank.putLadder(userMiGong);
            }
            // 移除玩家
            if(roomUser.getUserState() != RoomUser.UserState.Offline) {
                userRooms.remove(roomUser.getSession().getAccountId());
                roomUsers.remove(roomUser.getSession().getAccountId());
            }else{
                // 断线的时候已经移除了，这样确保可以断线的人断线期间再开
                // 也可以做断线重连，但是RoomUser中用的session，这个要注意
            }
        }
        // 保存对战记录
        PvpRecord pvpRecord = new PvpRecord();
        pvpRecord.setId(idService.acquireLong(PvpRecord.class));
        pvpRecord.setGrade(room.getGrade());
        pvpRecord.setTime(new Timestamp(System.currentTimeMillis()));
        pvpRecord.setRecord(room.toInfoString());
        dataService.insert(pvpRecord);
    }

    private ConcurrentLinkedQueue<RoomUser> getMatchingQueue(int grade){
        ConcurrentLinkedQueue<RoomUser> queue = matchingUsers.get(grade);
        if(queue == null){
            ConcurrentLinkedQueue<RoomUser> _queue = new ConcurrentLinkedQueue<RoomUser>();
            matchingUsers.putIfAbsent(grade,_queue);
            queue = matchingUsers.get(grade);
        }
        return queue;
    }
    // todo 分数转段位，首先段位不能太多
    private int scoreToGrade(int score){
        return 1;
    }


}





































