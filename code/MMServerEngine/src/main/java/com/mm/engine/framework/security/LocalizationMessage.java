package com.mm.engine.framework.security;

import com.mm.engine.framework.tool.util.PropsUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Administrator on 2017/11/30.
 */
public final class LocalizationMessage {
    private static final Logger log = LoggerFactory.getLogger(LocalizationMessage.class);

    static final String DefaultLocalization = "English";

    static ThreadLocal<String> threadLocalization = new ThreadLocal<>();
    static Map<String,Properties> messages = new HashMap<>();

    static{
        messages = PropsUtil.loadDirProps("localization");
    }

    public static String getText(String key){
        String localization = threadLocalization.get();
        if(localization == null){
            log.error("localization is not in threadLocalization,check it !");
            localization = DefaultLocalization;
        }
        Properties properties = messages.get(localization);
        if(properties == null){
            properties = messages.get(DefaultLocalization);
        }
        return properties.getProperty(key);
    }
    public static String getText(String key,Object... args){
        String localization = threadLocalization.get();
        if(localization == null){
            log.error("localization is not in threadLocalization,check it !");
            localization = DefaultLocalization;
        }
        Properties properties = messages.get(localization);
        if(properties == null){
            properties = messages.get(DefaultLocalization);
        }
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(properties.getProperty(key),args);
        return formattingTuple.getMessage();
    }

    public static void setThreadLocalization(String localization){
        threadLocalization.set(localization);
    }

    public static void removeThreadLocalization(){
        threadLocalization.remove();
    }
}
