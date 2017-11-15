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
    private static final String LADDER_RANK = "LADDER_RANK";

    private ConcurrentHashMap<String,Integer> uidToId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,String> idToUid = new ConcurrentHashMap<>();
    private IRankService rankService = new RankService();

    // 无尽版前10名
    private List<UserMiGong> unlimitedFrontCache = new ArrayList<>();
    // 天梯前10名
    private List<UserMiGong> ladderFrontCache = new ArrayList<>();

    private AtomicInteger idCreator  = new AtomicInteger(1);
    private DataService dataService;


    private Comparator<UserMiGong> unlimitedRankComparator  = (o1,o2)->o2.getUnlimitedPass() - o1.getUnlimitedPass();
    private Comparator<UserMiGong> ladderRankComparator  = (o1,o2)->o2.getLadderScore() - o1.getLadderScore();

    public void init(){
        rankService.createRank(UNLIMITED_RANK);
        rankService.createRank(LADDER_RANK);
        List<UserMiGong> userMiGongs = dataService.selectList(UserMiGong.class,"");
        for(UserMiGong userMiGong : userMiGongs){
            if(userMiGong.getUnlimitedPass() > 0) {
                int id = getIdByUserId(userMiGong.getUserId());
                rankService.put(UNLIMITED_RANK,id,userMiGong.getUnlimitedPass());
            }
            if(userMiGong.getLadderScore() > 0){
                int id = getIdByUserId(userMiGong.getUserId());
                rankService.put(LADDER_RANK,id,userMiGong.getLadderScore());
            }
        }
        unlimitedFrontCache = getFront(FRONT_CACHE_COUNT,UNLIMITED_RANK);
        ladderFrontCache = getFront(FRONT_CACHE_COUNT,LADDER_RANK);
    }

    /**
     * 无尽版的。。。。。
     */
    public void putUnlimited(UserMiGong userMiGong){
        String userId = userMiGong.getUserId();
        if(userMiGong.getUnlimitedPass() <= 0){
            return;
        }
        int id = getIdByUserId(userId);
        rankService.put(UNLIMITED_RANK,id,userMiGong.getUnlimitedPass());
        //
        synchronized (unlimitedFrontCache) {
            if (unlimitedFrontCache.size() < FRONT_CACHE_COUNT) {
                boolean has = false;
                for (UserMiGong um : unlimitedFrontCache) {
                    if (um.getUserId().equals(userMiGong.getUserId())) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    unlimitedFrontCache.add(userMiGong);
                }
                unlimitedFrontCache.sort(unlimitedRankComparator);
            } else if (unlimitedFrontCache.get(unlimitedFrontCache.size() - 1).getUnlimitedPass() < userMiGong.getUnlimitedPass()) {
                unlimitedFrontCache.remove(unlimitedFrontCache.size() - 1);
                unlimitedFrontCache.add(userMiGong);
                unlimitedFrontCache.sort(unlimitedRankComparator);
            }
        }
    }
    public int getUnlimitedRank(String userId){
        if(!uidToId.containsKey(userId)){
            return -1;
        }
        return rankService.getRankNum(UNLIMITED_RANK,uidToId.get(userId));
    }

    // 获取配置的钱X名
    public List<UserMiGong> getUnlimitedFront(){
        return unlimitedFrontCache;
    }

    /**
     * 天梯版的
     */
    public void putLadder(UserMiGong userMiGong){
        String userId = userMiGong.getUserId();
        if(userMiGong.getLadderScore() <= 0){
            return;
        }
        int id = getIdByUserId(userId);
        rankService.put(LADDER_RANK,id,userMiGong.getLadderScore());
        //
        synchronized (ladderFrontCache) {
            if (ladderFrontCache.size() < FRONT_CACHE_COUNT) {
                boolean has = false;
                for (UserMiGong um : ladderFrontCache) {
                    if (um.getUserId().equals(userMiGong.getUserId())) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    ladderFrontCache.add(userMiGong);
                }
                ladderFrontCache.sort(ladderRankComparator);
            }else if(ladderFrontCache.get(ladderFrontCache.size()-1).getUnlimitedPass() < userMiGong.getUnlimitedPass()){
                ladderFrontCache.remove(ladderFrontCache.size()-1);
                ladderFrontCache.add(userMiGong);
                ladderFrontCache.sort(ladderRankComparator);
            }
        }
    }
    public int getLadderRank(String userId){
        if(!uidToId.containsKey(userId)){
            return -1;
        }
        return rankService.getRankNum(LADDER_RANK,uidToId.get(userId));
    }
    // 获取配置的钱X名
    public List<UserMiGong> getLadderFront(){
        return ladderFrontCache;
    }
    //////////////////////////////////////////////////////////////////////////////
    private List<UserMiGong> getFront(int count,String rank){
        List<UserMiGong> ret = new ArrayList<>();
        List<RankData> rankDatas = rankService.getRankDatasByPage(rank,0,count);
        if(rankDatas != null && rankDatas.size() > 0) {
            for (RankData rankData : rankDatas) {
                String uid = idToUid.get(rankData.getId());
                UserMiGong userMiGong = dataService.selectObject(UserMiGong.class,"userId=?",uid);
                ret.add(userMiGong);
            }
        }
        return ret;
    }
    private int getIdByUserId(String userId){
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
        return id;
    }
}
