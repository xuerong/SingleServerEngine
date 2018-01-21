package com.migong.entity;


import com.mm.engine.framework.data.DataService;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/11/20.
 * 新手引导的类型
 */
public enum NewGuideType {
    Pass(0,1), // 闯关方式
    Unlimited(1,1),
    Pvp(2,1) // 人打人

    ;
    private final int id;
    private final int step;
    NewGuideType(int id,int step){
        this.id = id;
        this.step = step;
    }

    public int getId() {
        return id;
    }

    public int getStep() {
        return step;
    }

    public static void setNewGuide(UserMiGong userMiGong,int id,int step){
        Map<Integer,Integer> map = Util.split2Map(userMiGong.getNewUserGuide(),Integer.class,Integer.class);
        if(map == null){
            map = new HashMap<Integer,Integer>();
        }
        Integer curStep = map.get(id);
        if(curStep != null && curStep == step){
            return;
        }
        map.put(id,step);
        userMiGong.setNewUserGuide(Util.map2String(map));
        DataService dataService = BeanHelper.getServiceBean(DataService.class);
        dataService.update(userMiGong);
    }
}
