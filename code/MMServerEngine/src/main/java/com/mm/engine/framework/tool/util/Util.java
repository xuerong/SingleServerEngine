package com.mm.engine.framework.tool.util;

import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.Server;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.RemoteEndpoint;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/11/16.
 */
public final class Util {
    // 获取http访问的ip
    public static String getIp(HttpServletRequest request) {
        // We look if the request is forwarded
        // If it is not call the older function.
        String ip = request.getHeader("X-Pounded-For");
        if (ip != null) {
            return ip;
        }
        ip = request.getHeader("x-forwarded-for");

        if (ip == null) {
            return request.getRemoteAddr();
        } else {
            // Process the IP to keep the last IP (real ip of the computer on
            // the net)
            StringTokenizer tokenizer = new StringTokenizer(ip, ",");

            // Ignore all tokens, except the last one
            for (int i = 0; i < tokenizer.countTokens() - 1; i++) {
                tokenizer.nextElement();
            }
            ip = tokenizer.nextToken().trim();
            if (ip.equals("")) {
                ip = null;
            }
        }
        // If the ip is still null, we insert 0.0.0.0 to avoid null values
        if (ip == null) {
            ip = "0.0.0.0";
        }

        return ip;
    }
    // 获取websocket的ip
    //the socket object is hidden in WsSession, so you can use reflection to got the ip address.
    // the execution time of this method is about 1ms. this solution is not prefect but useful.
    public static String getIp(javax.websocket.Session session){
        RemoteEndpoint.Async async = session.getAsyncRemote();
        InetSocketAddress addr = (InetSocketAddress) getFieldInstance(async,
                "base#sos#socketWrapper#socket#sc#remoteAddress");
        if(addr == null){
            return "127.0.0.1";
        }
        return addr.getAddress().getHostAddress();
    }
    private static Object getFieldInstance(Object obj, String fieldPath) {
        String fields[] = fieldPath.split("#");
        for(String field : fields) {
            obj = getField(obj, obj.getClass(), field);
            if(obj == null) {
                return null;
            }
        }
        return obj;
    }

    private static Object getField(Object obj, Class<?> clazz, String fieldName) {
        for(;clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field field;
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static boolean isLocalHost(String host){
        if(host.equals("localhost") || host.equals("127.0.0.1")){
            return true;
        }
        String localHost = getHostAddress();
        if(host.equals(localHost)){
            return true;
        }
        return false;
    }
    private static String hostAddress = null;
    public static String getHostAddress(){
        if(hostAddress == null){
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            }catch (UnknownHostException e){
                throw new MMException(e);
            }
        }
//        System.out.println(hostAddress);
        return hostAddress;
    }
    public static String getLocalNetEventAdd(){
        return getHostAddress()+""+ Server.getEngineConfigure().getNetEventEntrance();
    }
    public static boolean isIP(String addr)
    {
        if(addr.length() < 7 || addr.length() > 15 || "".equals(addr))
        {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        return ipAddress;
    }


    public static <K> List<K> split2List(String content,Class<K> cls){
        List<K> ret = new LinkedList<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, ";");
        for (String entry : entryArray) {
            ret.add(convert(cls, entry));
        }
        return ret;
    }

    public static <K,V> Map<K,V> split2Map(String content,Class<K> kCls,Class<V> vCls){
        Map<K, V> ret = new HashMap<>();
        if (StringUtils.isBlank(content)) {
            return ret;
        }
        String[] entryArray = StringUtils.splitByWholeSeparator(content, "|");
        if (entryArray != null && entryArray.length != 0) {
            for (String entry : entryArray) {
                String[] keyValueArray = StringUtils.splitByWholeSeparator(entry, ";");
                if (keyValueArray.length == 2) {
                    ret.put(convert(kCls, keyValueArray[0]), convert(vCls, keyValueArray[1]));
                }
            }
        }
        return ret;
    }

    public static <K,V> String map2String(Map<K,V> map){
        if(map == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String seperator = "";
        for(Map.Entry entry : map.entrySet()){
            sb.append(seperator).append(entry.getKey().toString()).append(";").append(entry.getValue().toString());
            seperator = "|";
        }
        return sb.toString();
    }

    public static <K> String list2String(List<K> list) {
        if (list == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("");
        String seperator = "";
        for (K entry : list) {
            sb.append(seperator).append(entry.toString());
            seperator = ";";
        }
        return sb.toString();
    }

    public static <T> T convert(Class<T> clazz, String content) {
        if (clazz.isAssignableFrom(Integer.class)) {
            return clazz.cast(Integer.parseInt(content));
        } else if (clazz.isAssignableFrom(Long.class)) {
            return clazz.cast(Long.parseLong(content));
        } else if (clazz.isAssignableFrom(Short.class)) {
            return clazz.cast(Short.parseShort(content));
        } else if (clazz.isAssignableFrom(Byte.class)) {
            return clazz.cast(Byte.parseByte(content));
        } else if (clazz.isAssignableFrom(Boolean.class)) {
            return clazz.cast(Boolean.parseBoolean(content));
        } else if (clazz.isAssignableFrom(Double.class)) {
            return clazz.cast(Double.parseDouble(content));
        } else if (clazz.isAssignableFrom(Float.class)) {
            return clazz.cast(Float.parseFloat(content));
        } else if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(content);
        } else {
            throw new RuntimeException("不支持的类型");
        }
    }

    /** 获取服务器的utc时间的long值  单位ms**/
    public static long getSystemUtcTime(){
        return Calendar.getInstance().getTimeInMillis();
    }
}
