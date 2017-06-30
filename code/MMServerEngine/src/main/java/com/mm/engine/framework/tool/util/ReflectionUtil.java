package com.mm.engine.framework.tool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/11/13.
 * 反射工具
 */
public final class ReflectionUtil {
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtil.class);

    /**
     * 创建实例
     * **/
    public static <T> T newInstance(Class<T> cls){
        T instance=null;
        try {
            instance= cls.newInstance();
        }catch (Exception e){
            log.error("new instance failure",e);
            throw  new RuntimeException(e);
        }
        return instance;
    }

    public static Class[] getParamsTypes(Object... params){
        if(params == null || params.length == 0){
            return null;
        }
        Class[] result = new Class[params.length];
        for(int i = 0;i<params.length;i++){
            result[i] = params[i].getClass();
        }
        return result;
    }
}
