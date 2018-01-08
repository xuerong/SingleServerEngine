package com.mm.engine.framework.net.entrance.http;

import com.mm.engine.framework.net.entrance.Entrance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by a on 2016/8/8.
 */
public class StatisticsEntranceJetty extends Entrance{
    private static final Logger log = LoggerFactory.getLogger(StatisticsEntranceJetty.class);
    /**
     * 这个目录是服务的根目录，即浏览器访问的时候要添加这个目录
     * 而web.xml过滤的目录都是建立在该目录下面的，如：
     * 浏览器访问：http://localhost:8081/gm/index.jsp
     * 如果要过滤，只需要/*,而不是/gm/*
     *
     * 一些jar文件要移到jdk中
     * 鉴others/jdkJar
     */
    private String contextPath = "/statistics";
    private String resourceBase = "./target/mmserverengine";
    private String descriptor = "./target/mmserverengine/WEB-INF/webStatistics.xml";




    private Server server;

    public StatisticsEntranceJetty(){}

    @Override
    public void start() throws Exception{
        try {
            // 服务器的监听端口
            server = new Server(port);

            // 关联一个已经存在的上下文
            WebAppContext context = new WebAppContext();

            // 设置描述符位置
            context.setDescriptor(descriptor);

            // 设置Web内容上下文路径
            context.setResourceBase(resourceBase);

            // 设置上下文路径
            context.setContextPath(contextPath);
            context.setParentLoaderPriority(true);

            server.setHandler(context);

            // 启动
            server.start();

            System.out.println("statistics模块启动,使用url:http://localhost:"+port+contextPath+" 进行访问");

//            server.join();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    private void fire(){

    }
    @Override
    public void stop() throws Exception{
        if(server != null){
            server.stop();
        }
    }
}
