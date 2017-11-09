package com.migong;

import com.migong.entity.UserMiGong;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.DataService;
import org.hq.rank.core.RankData;
import org.hq.rank.service.IRankService;
import org.hq.rank.service.RankService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2017/11/3.
 * // 迷宫的rank服务
 * 1 无尽关卡的rank
 */
@Service(init = "init")
public class MiGongRank {
    private static final int FRONT_CACHE_COUNT = 10;

    private static final String UNLIMITED_RANK = "UNLIMITED_RANK";
    private ConcurrentHashMap<String,Integer> uidToId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,String> idToUid = new ConcurrentHashMap<>();
    private IRankService rankService = new RankService();

    // 前10名
    private List<UserMiGong> frontCache = new ArrayList<>();

    private AtomicInteger idCreator  = new AtomicInteger(1);
    private DataService dataService;


    private Comparator<UserMiGong> unlimitedRankComparator  =new Comparator<UserMiGong>() {
        @Override
        public int compare(UserMiGong o1, UserMiGong o2) {
            return o2.getUnlimitedPass() - o1.getUnlimitedPass();
        }
    };

    public void init(){
        rankService.createRank(UNLIMITED_RANK);
        List<UserMiGong> userMiGongs = dataService.selectList(UserMiGong.class,"");
        for(UserMiGong userMiGong : userMiGongs){
            if(userMiGong.getUnlimitedPass() > 0) {
                int id = idCreator.getAndIncrement();
                uidToId.put(userMiGong.getUserId(), id);
                idToUid.put(id,userMiGong.getUserId());
                rankService.put(UNLIMITED_RANK,id,userMiGong.getUnlimitedPass());
            }
        }
        frontCache = getFront(FRONT_CACHE_COUNT);
    }

    public void putUnlimited(UserMiGong userMiGong){
        String userId = userMiGong.getUserId();
        if(userMiGong.getUnlimitedPass() <= 0){
            return;
        }
        Integer id = uidToId.get(userId);
        if(id == null){
            id = idCreator.getAndIncrement();
            Integer old = uidToId.putIfAbsent(userId,id);
            if(old == null){
                idToUid.put(id,userId);
            }else{
                id = old;
            }
        }
        rankService.put(UNLIMITED_RANK,id,userMiGong.getUnlimitedPass());
        //
        if(frontCache.size() < FRONT_CACHE_COUNT && !uidToId.containsKey(userId)){
            frontCache.add(userMiGong);
            frontCache.sort(unlimitedRankComparator);
        }else if(frontCache.get(frontCache.size()-1).getUnlimitedPass() < userMiGong.getUnlimitedPass()){
            frontCache.remove(frontCache.size()-1);
            frontCache.add(userMiGong);
            frontCache.sort(unlimitedRankComparator);
        }
    }

    public int getRank(String userId){
        if(!uidToId.containsKey(userId)){
            return -1;
        }
        return rankService.getRankNum(UNLIMITED_RANK,uidToId.get(userId));
    }

    public List<UserMiGong> getFront(int count){
        List<UserMiGong> ret = new ArrayList<>();
        List<RankData> rankDatas = rankService.getRankDatasByPage(UNLIMITED_RANK,0,count);
        if(rankDatas != null && rankDatas.size() > 0) {
            for (RankData rankData : rankDatas) {
                String uid = idToUid.get(rankData.getId());
                UserMiGong userMiGong = dataService.selectObject(UserMiGong.class,"userId=?",uid);
                ret.add(userMiGong);
            }
        }
        return ret;
    }
    // 获取配置的钱X名
    public List<UserMiGong> getFront(){
        return frontCache;
    }
}
