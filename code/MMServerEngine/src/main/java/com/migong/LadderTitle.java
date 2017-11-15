package com.migong;

/**
 * Created by Administrator on 2017/11/15.
 * 天梯称号，对应积分和名称
 */
public enum LadderTitle {
    Ladder1(0,"ladder1"),
    Ladder2(10,"ladder2"),
    Ladder3(20,"ladder3"),
    Ladder4(40,"ladder4");


    public static LadderTitle getLadderByScore(int score){
        LadderTitle result = LadderTitle.Ladder1;
        for(LadderTitle ladderTitle : LadderTitle.values()){
            if(ladderTitle.getScore() <= score){
                result = ladderTitle;
            }else{
                break;
            }
        }
        return result;
    }


    private final int score;
    private final String title;
    LadderTitle(int score,String title){
        this.score = score;
        this.title = title;
    }

    public int getScore() {
        return score;
    }

    public String getTitle() {
        return title;
    }
}
