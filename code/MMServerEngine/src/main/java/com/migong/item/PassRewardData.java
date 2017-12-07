package com.migong.item;

import com.mm.engine.framework.tool.util.Util;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/12/7.
 * 关卡的奖励解析
 */
public class PassRewardData {

    private static ConcurrentHashMap<String,PassRewardData> cache = new ConcurrentHashMap<>();

    private int[] starGold;

    private int[] starEnergy;

    private Map<Integer,int[]> itemReward;


    public static PassRewardData parse(String rewardId){
        PassRewardData passRewardData = cache.get(rewardId);
        if(passRewardData != null){
            return passRewardData;
        }
        if(StringUtils.isEmpty(rewardId)){
            return null;
        }
        PassRewardData ret = new PassRewardData();
        List<String> rewardStrs = Util.split2List(rewardId,String.class,"|");
        int i=0;
        for(String rewardStr : rewardStrs){
            if(i == 0){ // 金币
                List<Integer> list = Util.split2List(rewardStr,Integer.class);
                ret.setStarGold(new int[]{list.get(0),list.get(1),list.get(2),list.get(3)});
            }else if(i == 1){ // energy
                List<Integer> list = Util.split2List(rewardStr,Integer.class);
                ret.setStarEnergy(new int[]{list.get(0),list.get(1),list.get(2),list.get(3)});
            }else{
                List<Integer> list = Util.split2List(rewardStr,Integer.class);

                Map map = ret.getItemReward();
                if(map == null){
                    map = new HashMap<>();
                    ret.setItemReward(map);
                }
                map.put(list.get(0),new int[]{list.get(1),list.get(2),list.get(3),list.get(4)});
            }
            i++;
        }
        cache.putIfAbsent(rewardId,ret);
        return ret;
    }

    public int[] getStarGold() {
        return starGold;
    }

    public void setStarGold(int[] starGold) {
        this.starGold = starGold;
    }

    public int[] getStarEnergy() {
        return starEnergy;
    }

    public void setStarEnergy(int[] starEnergy) {
        this.starEnergy = starEnergy;
    }

    public Map<Integer, int[]> getItemReward() {
        return itemReward;
    }

    public void setItemReward(Map<Integer, int[]> itemReward) {
        this.itemReward = itemReward;
    }
}

