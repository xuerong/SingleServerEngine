package com.mm.engine.framework.control.event;

import com.mm.engine.framework.control.ServiceHelper;
import com.mm.engine.framework.control.annotation.NetEventListener;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.netEvent.NetEventData;
import com.mm.engine.framework.control.netEvent.NetEventService;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.BeanHelper;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.procedure.TShortObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2015/11/20.
 *
 * 事件分为同步执行和异步执行，
 * 同步执行的：事件监听者完成处理才返回：需要监控
 * 一步执行的：通过线程池分配线程执行触发各个事件，确保两点：
 * 1、事件线程有最大值，超过最大值，事件要排队
 * 2、事件队列有最大值，超过最大值，抛出服务器异常：可在某个比较大的值抛出警告
 */
@Service(init = "init")
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final int poolWarningSize=20;
    private static final int queueWarningSize=1000;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10,100,3000, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>(),
            new RejectedExecutionHandler(){
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 拒绝执行
        }
    });


    private final TShortObjectHashMap<Set<EventListenerHandler>> handlerMap=new TShortObjectHashMap<>();

    private NetEventService netEventService;

    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        TShortObjectHashMap<Set<Class<?>>> handlerClassMap= ServiceHelper.getEventListenerHandlerClassMap();
        handlerClassMap.forEachEntry(new TShortObjectProcedure<Set<Class<?>>>() {
            @Override
            public boolean execute(short i, Set<Class<?>> classes) {
                Set<EventListenerHandler> handlerSet=new HashSet<EventListenerHandler>();
                for(Class<?> cls : classes){
                    handlerSet.add((EventListenerHandler)BeanHelper.getServiceBean(cls));
                }
                handlerMap.put(i,handlerSet);
                return true;
            }
        });
    }
    // 最后用一个系统的检测服务update进行系统所有的监测任务
    public String getMonitorData(){
        BlockingQueue<Runnable> queue = executor.getQueue();
        int poolSize=executor.getPoolSize();
        if(poolSize>poolWarningSize || queue.size()>queueWarningSize){
            return "event thread executor pool is too big poolSize:"+poolSize +",queueSize:"+queue.size();
        }
        return "ok";
    }

    /**
     * 事件是异步的
     * TODO 发出事件改为四种:同步|异步*广播|不广播
     * **/
    public void fireEvent(final EventData eventData){
        fireEvent(eventData,false);
    }
    public void fireEvent(final EventData eventData, final boolean broadcast){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                fireEventSyn(eventData);
                if(broadcast){
                    NetEventData netEventData = new NetEventData(SysConstantDefine.broadcastEvent);
                    netEventData.setParam(eventData);
                    netEventService.broadcastNetEvent(netEventData,false);
                }
            }
        });
    }
    // 接受到其它服务器发送的事件
    @NetEventListener(netEvent = SysConstantDefine.broadcastEvent)
    public NetEventData receiveEventData(NetEventData netEventData){
        EventData eventData = (EventData)netEventData.getParam();
        fireEvent(eventData,false);
        return null;
    }
    /**
     * 同步触发事假，即事件完成方可返回
     * */
    // TODO 看看是否可以都换成这个，就像netEvent是否可以换成remoteCall
    public void fireEventSyn(Object data,int event){
        EventData eventData = new EventData(event);
        eventData.setData(data);
        fireEventSyn(eventData);
    }
    public void fireEventSyn(EventData event){
        try {
            Set<EventListenerHandler> handlerSet = handlerMap.get(event.getEvent());
            if (handlerSet == null || handlerSet.size() == 0) {
                log.warn("event:" + event.getEvent() + " has no listener");
                return;
            }
            for (EventListenerHandler handler : handlerSet) {
                handler.handle(event);
            }
        }catch (Throwable e){
            e.printStackTrace();
            log.error("exception happened while fire event :"+event.getEvent());
        }
    }
}
