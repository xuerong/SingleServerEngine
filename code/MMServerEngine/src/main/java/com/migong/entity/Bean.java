package com.migong.entity;

public class Bean {
    private int x;
    private int y;
    private int score;

    public Bean(){}
    public Bean(int x,int y,int score){
        this.x = x;
        this.y = y;
        this.score = score;
    }

    public int toInt(int size){
        return x*size+y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
