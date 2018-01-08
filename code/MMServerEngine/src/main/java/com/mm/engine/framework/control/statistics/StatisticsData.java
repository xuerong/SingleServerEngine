package com.mm.engine.framework.control.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsData {
    private List<String> heads;
    private List<List<String>> datas;
//    public Map<String,List<String>> datas = new HashMap<>();


    public List<String> getHeads() {
        return heads;
    }

    public void setHeads(List<String> heads) {
        this.heads = heads;
    }

    public List<List<String>> getDatas() {
        return datas;
    }

    public void setDatas(List<List<String>> datas) {
        this.datas = datas;
    }
}
