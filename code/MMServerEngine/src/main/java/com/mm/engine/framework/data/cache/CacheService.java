package com.mm.engine.framework.data.cache;

import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.tool.helper.BeanHelper;

import java.util.List;
import java.util.Map;

/**
 * Created by a on 2016/9/6.
 */
@Service(init = "init")
public class CacheService{
    private CacheCenter cacheCenter;
    public void init(){
        cacheCenter = BeanHelper.getFrameBean(CacheCenter.class);
    }
    //---
    public CacheEntity putIfAbsent(String key, CacheEntity entity) {
        return cacheCenter.putIfAbsent(key,entity);
    }

    public void putList(Map<String, CacheEntity> entityMap) {
        cacheCenter.putList(entityMap);
    }

    public CacheEntity get(String key) {
        return cacheCenter.get(key);
    }

    public List<CacheEntity> getList(String... keys) {
        return cacheCenter.getList(keys);
    }

    public CacheEntity remove(String key) {
        return cacheCenter.remove(key);
    }

    public boolean update(String key, CacheEntity entity) {
        return cacheCenter.update(key,entity);
    }
}
