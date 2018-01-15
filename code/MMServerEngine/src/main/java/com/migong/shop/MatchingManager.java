package com.migong.shop;

import com.migong.MiGongRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhengyuzhen on 2018/1/12.
 */
public class MatchingManager {
    static final boolean showMatch = false;
    private static Logger logger = LoggerFactory.getLogger(MatchingManager.class);
    public static final int MatchCount = MiGongRoom.USER_COUNT;
    public static final int MaxMatchTime = 20;
    // 匹配次数与距离的对应关系
    public static final int[] MatchTimeToScoreDis = new int[]{100,200,400,800,1600,3200,6400,Integer.MAX_VALUE};
    /**
     * 匹配工具：
     * 入口：传入玩家id，分数
     * 出口：构造函数传进来回调，玩家数的玩家
     *
     * 规则：
     * 1、每1秒钟匹配一次，每秒未匹配的扩大匹配范围
     * 2、
     */
    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    // 回调函数
    MatchCallBack matchCallBack;
    TimeoutCallBack timeoutCallBack;
    // 分数-id-玩家
    volatile Map<Integer,List<MatchUser>> matchUserMap = new TreeMap<>();
    volatile Map<String,MatchUser> uidToMatchUser = new HashMap<>();

    Object exchangeLock = new Object();
    volatile boolean matching;

    public MatchingManager(MatchCallBack matchCallBack,TimeoutCallBack timeoutCallBack){
        this.matchCallBack = matchCallBack;
        this.timeoutCallBack = timeoutCallBack;
        start();
    }

    void start(){
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                doMarch();
            }
        },0,1, TimeUnit.SECONDS);
    }

    public void destroy(){
        scheduledExecutorService.shutdownNow();
    }

    void doMarch(){
        if(matching){
            logger.error("marching = {} while doMatch,matchUserMap.size = {}",matching,matchUserMap.size());
            return;
        }
        long beginTime = System.currentTimeMillis();
        matching = true;
        try {
            Map<Integer, List<MatchUser>> tem;
            synchronized (exchangeLock) {
                // 更换引用
                tem = matchUserMap;
                matchUserMap = new TreeMap<>();
                uidToMatchUser = new HashMap<>(); // 这时候不能移除了
            }
            if(tem.size() == 0){
                return;
            }

            // 所有的玩家按照分数排行放入list
            List<MatchUser> allUser = new LinkedList<>();
            for (List<MatchUser> list : tem.values()) {
                for (MatchUser matchUser : list) {
                    allUser.add(matchUser);
                }
            }
            if(showMatch) {
                for (MatchUser matchUser : allUser) {
                    System.out.print(matchUser + "|");
                }
                System.out.print("----------------->");
            }
            List<List<MatchUser>> matchSuccess = new ArrayList<>(); // 匹配成功的
            List<MatchUser> marchFail = new ArrayList<>(); // 匹配失败的

            Iterator<MatchUser> it = allUser.iterator();
            while (it.hasNext()) {
                MatchUser matchUser1 = it.next();
                it.remove();
                if (matchUser1.isHasMatch()) { // 已经匹配成功了
                    continue;
                }
                int matchDis = matchDis(matchUser1);
                List<MatchUser> hasMatch = new ArrayList<>(); // 本玩家匹配成功的

                for (MatchUser matchUser2 : allUser) { // 这些玩家的分数都大于等于matchUser1
                    if (matchUser2.isHasMatch()) { // 这个玩家之前已经匹配成功，所以不匹配他
                        continue;
                    }
                    if (matchUser2.getScore() - matchUser1.getScore() > matchDis) { // 超过玩家matchUser1的距离距离：后面的一定都大于，所以,玩家matchUser1匹配失败
                        break;
                    }
                    if (matchUser2.getScore() - matchUser1.getScore() > matchDis(matchUser2)) { // 超过玩家matchUser2的距离距离，继续下一个
                        continue;
                    }
                    // 与已经匹配好的玩家是否匹配
                    boolean fail = false;
                    for (MatchUser has : hasMatch) {
                        if (!isMatch(has, matchUser2)) {
                            fail = true;
                            break;
                        }
                    }
                    if(fail){
                        continue;
                    }
                    hasMatch.add(matchUser2); // matchUser2匹配成功
                    if (hasMatch.size() == MatchCount - 1) {// 如果到人数，结束匹配
                        break;
                    }
                }

                if (hasMatch.size() == MatchCount -1) {
                    hasMatch.add(matchUser1);
                    matchSuccess.add(hasMatch);
                    // 设置匹配陈宫，后面会移除出队列
                    for (MatchUser matchUser : hasMatch) {
                        matchUser.setHasMatch(true);
                        if(showMatch) {
                            System.out.print(matchUser + "|");
                        }
                    }
                    if(showMatch) {
                        System.out.print("--------------");
                    }
                } else {
                    marchFail.add(matchUser1);
                }
            }
            if(showMatch) {
                System.out.println();
            }
            // 匹配成功的开房间
            for (final List<MatchUser> list : matchSuccess) {
                executorService.execute(() -> {
                    List<String> matchUserUids = new ArrayList<>(list.size());
                    for(MatchUser matchUser : list){
                        matchUserUids.add(matchUser.getUid());
                    }
                    matchCallBack.matchSuccess(matchUserUids);
                });
            }
            // 匹配失败的放回队列，等待下一轮匹配
            synchronized (exchangeLock) {
                for (MatchUser matchUser : marchFail) {
                    matchUser.setMatchTime(matchUser.getMatchTime()+1);
                    if(matchUser.getMatchTime() > MaxMatchTime){
                        timeoutCallBack.timeout(matchUser.getUid());
                        continue;
                    }
                    List<MatchUser> list = matchUserMap.get(matchUser.getScore());
                    if (list == null) {
                        list = new ArrayList<>();
                        matchUserMap.put(matchUser.getScore(), list);
                    }
                    list.add(matchUser);
                    uidToMatchUser.put(matchUser.getUid(),matchUser);
                }
            }
        }catch (Throwable e){
            throw e;
        }finally {
            matching = false;
            long endTime = System.currentTimeMillis();
            if(endTime-beginTime>300) {
                System.out.println("matching use time:" +( endTime - beginTime));
            }
        }
    }

    /**
     * 添加玩家
     * @param uid
     * @param score
     */
    public void addMatchUser(String uid, int score){
        synchronized (exchangeLock) {
            if(uidToMatchUser.containsKey(uid)){
                logger.warn("user is exit in matching list");
                return;
            }
            List<MatchUser> list = matchUserMap.get(score);
            if(list == null){
                list = new LinkedList<>();
                matchUserMap.put(score,list);
            }
            MatchUser matchUser = new MatchUser(uid,score);
            list.add(matchUser);
            uidToMatchUser.put(matchUser.getUid(),matchUser);
        }
    }

    /**
     * 移除玩家
     * @param uid
     */
    public void removeMatchUser(String uid){
        synchronized (exchangeLock) {
            MatchUser matchUser = uidToMatchUser.remove(uid);
            if(matchUser == null){
                return;
            }
            List<MatchUser> list = matchUserMap.get(matchUser.getScore());
            if(list == null){
                return;
            }
            list.remove(matchUser);
        }
    }

    int matchDis(MatchUser matchUser){
        if(matchUser.getMatchTime() > MatchTimeToScoreDis.length - 1){
            return Integer.MAX_VALUE;
        }
        return MatchTimeToScoreDis[matchUser.getMatchTime()];
    }

    /**
     * 两个玩家是否能匹配：
     * 匹配距离是否包含彼此
     * @param matchUser1
     * @param matchUser2
     * @return
     */
    boolean isMatch(MatchUser matchUser1,MatchUser matchUser2){
        int dis = Math.abs(matchUser1.getScore() - matchUser2.getScore());
        return matchDis(matchUser1)>=dis && matchDis(matchUser2)>=dis ;
    }

    public static interface MatchCallBack{
        public void matchSuccess(List<String> matchUsers);
    }
    public static interface TimeoutCallBack{
        public void timeout(String matchUser);
    }

    class MatchUser{
        private String uid;
        private int score;
        private int matchTime;
        private boolean hasMatch;

        public MatchUser(String uid,int score){
            this.uid = uid;
            this.score = score;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getMatchTime() {
            return matchTime;
        }

        public void setMatchTime(int matchTime) {
            this.matchTime = matchTime;
        }

        public boolean isHasMatch() {
            return hasMatch;
        }

        public void setHasMatch(boolean hasMatch) {
            this.hasMatch = hasMatch;
        }
        @Override
        public String toString(){
            return new StringBuilder(uid).append(",").append(score).append(",").append(matchTime).toString();
        }
    }

    public static void main(String[] args){
        MatchingManager matchingService = new MatchingManager(new MatchCallBack() {
            @Override
            public void matchSuccess(List<String> matchUsers) {
//                for(String matchUser : matchUsers) {
//                    int aa = Integer.parseInt(matchUser)%4;
//                    System.out.print("("+matchUser+","+(aa==0?4:aa)+")");
//                }
//                System.out.println();
            }
        },null);
        AtomicInteger ai = new AtomicInteger(1);
        //
        Random random = new Random();

        for(int i=0;i<10000;i++) {
            matchingService.addMatchUser(String.valueOf(ai.getAndIncrement()), 100);
            matchingService.addMatchUser(String.valueOf(ai.getAndIncrement()), 300);
            matchingService.addMatchUser(String.valueOf(ai.getAndIncrement()), 600);
//            matchingService.addMatchUser(String.valueOf(ai.getAndIncrement()), 400);
            try{
                Thread.sleep(random.nextInt(1300));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
