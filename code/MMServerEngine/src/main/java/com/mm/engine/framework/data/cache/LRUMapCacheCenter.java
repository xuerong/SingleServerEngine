package com.mm.engine.framework.data.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;
import com.mm.engine.framework.server.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/6/29.
 */
public class LRUMapCacheCenter implements CacheCenter {
//    Concurrentlinkedh
    ConcurrentLinkedHashMap<String, CacheEntity> map = new ConcurrentLinkedHashMap.Builder<String, CacheEntity>()
        .maximumWeightedCapacity(Integer.parseInt(Server.getEngineConfigure().getString("maximumWeightedCapacity"))).weigher(Weighers.singleton())
        .build();


//    ConcurrentHashMap<String,CacheEntity> map = new ConcurrentHashMap<>();
    @Override
    public CacheEntity putIfAbsent(String key, CacheEntity entity) {
        return map.putIfAbsent(key,entity);
    }

    @Override
    public void putList(Map<String, CacheEntity> entityMap) {
        map.putAll(entityMap);
    }

    @Override
    public CacheEntity get(String key) {
        return map.get(key);
    }

    @Override
    public List<CacheEntity> getList(String... keys) {
        List<CacheEntity> result = new ArrayList<>();
        for(String key : keys){
            CacheEntity cacheEntity = map.get(key);
            if(cacheEntity == null){
                return null;
            }
            result.add(cacheEntity);
        }
        return result;
    }

    @Override
    public CacheEntity remove(String key) {
        return map.remove(key);
    }

    @Override
    public boolean update(String key, CacheEntity entity) {
        map.put(key,entity);
        return true;
    }
}
