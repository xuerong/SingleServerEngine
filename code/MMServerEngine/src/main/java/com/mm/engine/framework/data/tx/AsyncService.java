package com.mm.engine.framework.data.tx;

import com.mm.engine.framework.control.annotation.NetEventListener;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.netEvent.NetEventData;
import com.mm.engine.framework.control.netEvent.NetEventService;
import com.mm.engine.framework.data.OperType;
import com.mm.engine.framework.data.cache.CacheCenter;
import com.mm.engine.framework.data.cache.CacheEntity;
import com.mm.engine.framework.data.cache.KeyParser;
import com.mm.engine.framework.data.persistence.orm.DataSet;
import com.mm.engine.framework.data.persistence.orm.EntityHelper;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.server.Server;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.BeanHelper;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by apple on 16-8-14.
 * 更新数据库检测用一个线程，然后分配给其它线程去处理
 * 注意：原来同一个服务线程传过来的更新需求，要在同一个更新线程中按顺序处理
 */
@Service(init = "init",destroy = "destroy",destroyPriority = 5)
public class AsyncService {
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);
    // 异步更新队列
//    private static LinkedBlockingQueue<AsyncData> asyncDataQueue = new LinkedBlockingQueue<AsyncData>();
    // 另一个队列，key为对象的类的名字,只存储增加和删除，根据异步对象的类型进行存储，在REFRESHDBLIST中起作用：
    // 1防止漏掉数据：插入数据库之后才删它，而asyncDataQueue在插入数据库之前就会被删掉了，2提高查询效率
    private ConcurrentHashMap<String,List<AsyncData>> asyncDataMap = new ConcurrentHashMap<>();
    private final int threadCount = Runtime.getRuntime().availableProcessors()+1;
    private Random threadRand = new Random();
    private ThreadLocal<Integer> threadNum = new ThreadLocal<>();
    private Map<Integer,Worker> workerMap = new HashMap<>();
    // listKeys
    // key为对象的类的名字，值为其对应的listKeys
    private ConcurrentHashMap<String,Set<String>> listKeysMap = new ConcurrentHashMap<>();
    //
    private CacheCenter cacheCenter;
    private NetEventService netEventService;
    private LockerService lockerService;

    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        cacheCenter= BeanHelper.getFrameBean(CacheCenter.class);
        lockerService = BeanHelper.getServiceBean(LockerService.class);
        // 判断本服务器是否是异步服务器，如果是，则启动异步更新线程
        if(Server.getEngineConfigure().isAsyncServer()){
            startAsyncService();
        }
    }

    public void destroy(){
        stop();
    }

    private void startAsyncService(){
        for(int i=0;i<threadCount;i++){
            Worker worker = new Worker(i);
            workerMap.put(i,worker);
            worker.start();
        }
    }

    /**
     * 服务器停止之前别忘了调用该方法
     * 由于要等待各个线程处理完成，所以可能要等待一段时间
     */
    public void stop(){
        if(!Server.getEngineConfigure().isAsyncServer()){
            return;
        }
        CountDownLatch latch = new CountDownLatch(workerMap.size());
        for(Worker worker : workerMap.values()){
            worker.stop(latch);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new MMException(e);
        }
    }

    /**
     * 插入一个对象，
     *
     */
    public void insert(String key,Object entity){
        doAsyncData(key,entity,OperType.Insert);
    }
    /**
     * 更新一个对象，
     */
    public void update(String key,Object entity){
        doAsyncData(key,entity,OperType.Update);
    }
    /**
     * 删除一个实体
     * 由于要异步删除，缓存中设置删除标志位,所以，在缓存中是update
     */
    public void delete(String key,Object entity){
        doAsyncData(key,entity,OperType.Delete);
    }
    private void doAsyncData(String key,Object object,OperType operType){
        AsyncData asyncData = new AsyncData();
        asyncData.setOperType(operType);
        asyncData.setKey(key);
        asyncData.setObject(object);
        // 这个同一个服务线程的threadNum必须一样
        Integer t = threadNum.get();
        if(t == null){
            t = threadRand.nextInt(threadCount);
            threadNum.set(t);
        }
        asyncData.setThreadNum(t);
        receiveAsyncData(asyncData);
    }

    /**
     * 事务可以考虑用这个，这样，一个事务只需要一次网络访问
     */
    public void asyncData(List<AsyncData> asyncDataList){
        Integer t = threadNum.get();
        if(t == null){
            t = threadRand.nextInt(threadCount);
            threadNum.set(t);
        }
        for(AsyncData asyncData : asyncDataList){
            asyncData.setThreadNum(t);
        }
        receiveAsyncData(asyncDataList);
    }
    /**
     * 从异步服务器中获取满足条件的对象
     * 当从数据库中获取list之后，需要从异步服务器中获取满足该list且还没有更新到数据库中的对象，主要是插入和删除的
     * 同时将对应的listKey记录到异步服务器中，以便新数据插入时更新对应的list
     */
    public List<AsyncData> getAsyncDataBelongListKey(String listKey){
        return receiveRefreshDBList(listKey);
    }
    /// 上面的四个函数，处理其他服务器发送过来的异步数据库请求
    public void receiveAsyncData(Object object){
        if(object instanceof AsyncData){
            AsyncData asyncData = (AsyncData)object;
            doReceiveAsyncData(asyncData);
        }else if(object instanceof List){
            List<AsyncData> asyncDataList = (List<AsyncData>)object;
            for(AsyncData asyncData : asyncDataList){
                doReceiveAsyncData(asyncData);
            }
        }
    }

    // 其他服务器发送来的，异步服务器有的对应list的对象和更新状态
    public List<AsyncData> receiveRefreshDBList(String listKey){
        // 查看并插入listKeys插入
        String classKey = KeyParser.getClassNameFromListKey(listKey);
        Set<String> listKeys = listKeysMap.get(classKey);
        if(listKeys == null){
            listKeys = new ConcurrentHashSet<>();
            listKeysMap.putIfAbsent(classKey,listKeys);
            listKeys = listKeysMap.get(classKey);
        }
        listKeys.add(listKey);// 注意这里一定要先插入，再获取再插入，而不能创建-插入-放入listKeysMap，多线程下回出错
        // 从异步列表中获取相应的对象
        List<AsyncData> result = null;
        List<AsyncData> asyncDataList = asyncDataMap.get(classKey);
        if(asyncDataList != null){
            for(AsyncData asyncData : asyncDataList){
                if(KeyParser.isObjectBelongToList(asyncData.getObject(),listKey)){
                    if(result == null){
                        result = new ArrayList<>();
                    }
                    result.add(asyncData);
                }
            }
        }
        return result;
    }
    private void doReceiveAsyncData(AsyncData asyncData){
        if(asyncData.getOperType() == OperType.Insert || asyncData.getOperType() == OperType.Delete) {
            List<AsyncData> asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
            if (asyncDataList == null) {
                // TODO 这里用这个list怎么样呢，有没有更好的选择？因为后面有删除需求，这个删除在多线程会不会效率太低
                asyncDataList = Collections.synchronizedList(new LinkedList<AsyncData>());
                asyncDataMap.putIfAbsent(asyncData.getObject().getClass().getName(), asyncDataList);
                asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
            }
            asyncDataList.add(asyncData);
        }
        Worker worker = workerMap.get(asyncData.getThreadNum());
        boolean success = worker.addAsyncData(asyncData);
        // 插入可能存在的listKey
        if(asyncData.getOperType() == OperType.Insert || asyncData.getOperType() == OperType.Delete) {
            Set<String> listKeys = listKeysMap.get(asyncData.getObject().getClass().getName());
            if (listKeys != null && listKeys.size() > 0) {
                for (Iterator<String> iter = listKeys.iterator(); iter.hasNext(); ) {
                    String listKey = iter.next();
                    if (KeyParser.isObjectBelongToList(asyncData.getObject(), listKey)) {
                        if (!lockerService.lockKeys(listKey)) { // 要不要做成一起加锁， 解锁，增加效率？
                            throw new MMException("加锁失败,listKey = " + listKey);
                        }
                        // 从缓存中取数据
                        CacheEntity cacheEntity = cacheCenter.get(listKey);
                        if (cacheEntity == null) {
                            // 缓存中没有，删除掉这个listKey
                            iter.remove();
                            lockerService.unlockKeys(listKey);
                            continue;
                        }
                        List<String> keyList = (List<String>) cacheEntity.getEntity();
                        if (asyncData.getOperType() == OperType.Insert){
//                            if (!keyList.contains(asyncData.getKey())) // 这里先不判断，影响效率，或者使用Set，会影响排序
                            keyList.add(asyncData.getKey());
                        }else if (asyncData.getOperType() == OperType.Delete){
                            keyList.remove(asyncData.getKey());
                        }
                        cacheCenter.update(listKey,cacheEntity); // 由于加了锁之后获取的，所以不用担心版本问题，第一次放入缓存的地方也加了锁
                        lockerService.unlockKeys(listKey);
                    }
                }
            }
        }
        if(!success){
            throw new MMException("更新数据库队列满，异步服务器压力过大");
        }
    }
    public static void main(String[] args){
        Object[] aaas = {"33","2","1"};
        String[]  bbb = (String[])aaas;
        System.out.println(bbb.length);
    }

    public static class AsyncData{
        private String key;
        private OperType operType;
        private Object object;

        private int threadNum; // 更新用的线程编号，同一个服务中的更新必须是同一个threadNum

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public OperType getOperType() {
            return operType;
        }

        public void setOperType(OperType operType) {
            this.operType = operType;
        }

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public int getThreadNum() {
            return threadNum;
        }

        public void setThreadNum(int threadNum) {
            this.threadNum = threadNum;
        }
        @Override
        public String toString(){
            return new StringBuffer("key="+key).
                    append(",object="+object).
                    append(",operType="+operType).
                    append(",threadNum"+threadNum).
                    toString();
        }
    }
    public class Worker{
        private static final int MAXSWALLOWSTOPEXCEPTIONTIMES = 10; // 停止时最大吞掉异常的次数
        private static final int MAXWAITTIMES = 100; // 停止时最多等待次数
        private static final int WAITINTERVAL = 200; // 停止时每次等待时间
        private static final int MAXDBTIMES = 10; // 最大提交数据库次数
        private static final int WAITINTERVALDB = 10000; //提交失败等待时间

        private LinkedBlockingQueue<AsyncData> asyncDataQueue = new LinkedBlockingQueue<AsyncData>();
        private Thread dbThread = null;
        private int threadNum;
        private volatile boolean running = false;

        public boolean isRunning(){
            return running;
        }

        public Worker(int threadNum){
            this.threadNum = threadNum;
        }

        public boolean addAsyncData(AsyncData asyncData){
            if(running){
                return asyncDataQueue.offer(asyncData);
            }else{
                throw new MMException("异步服务器已经停止运行，或还没有运行,asyncData:"+asyncData.toString());
            }
        }

        public void start(){
            running = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (true){
                        AsyncData asyncData = null;
                        try{
                            asyncData = asyncDataQueue.take();
                            boolean success = false;
                            int dbTime = 0;
                            while(!success){
                                try {
                                    switch (asyncData.getOperType()){
                                        case Insert:
                                            DataSet.insert(asyncData.getObject());
                                            break;
                                        case Update:
                                            DataSet.update(asyncData.getObject(),EntityHelper.parsePkCondition(asyncData.getObject()));
                                            break;
                                        case Delete:
                                            DataSet.delete(asyncData.getObject(),EntityHelper.parsePkCondition(asyncData.getObject()));
                                            break;
                                    }
                                    success = true;
                                } catch (Exception e) {
                                    if(dbTime++ > MAXDBTIMES){
                                        log.error("提交数据库失败，共尝试次数："+dbTime);
                                        break;
                                    }
                                    log.error("提交数据库失败，"+WAITINTERVALDB+"毫秒后尝试再次提交，尝试次数："+dbTime);
                                    Thread.sleep(WAITINTERVALDB);
                                } finally {

                                }
                            }
                            if(success){ // 删除记录 TODO 这里删除是不是有点慢
                                List<AsyncData> asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
                                if(asyncDataList != null) {
                                    asyncDataList.remove(asyncData);
                                }
                            }
                        }catch (Throwable e){
                            if(e instanceof InterruptedException && asyncData == null && !running){
                                // stop发生了
                                log.info("async thread stop success");
                                break;
                            }
                            // 这里失败怎么办
                            asyncDbFail(asyncData);
                            e.printStackTrace();
                        }

                    }
                }
            };
            dbThread = new Thread(runnable);
            dbThread.start();
        }
        // TODO 更新失败的处理
        private void asyncDbFail(AsyncData asyncData){
            log.error("asyncDbFail,"+asyncData);
        }
        // 这里用毒丸方法如何？
        public void stop(final CountDownLatch countDownLatch){
            running = false; // 不再接收处理
            new Thread(){
                @Override
                public void run(){
                    int SwallowExceptionTime = 0;
                    int waitTime = 0;
                    int lastSize = asyncDataQueue.size();
                    if(lastSize > 0 && !dbThread.isAlive()){
                        log.info("restart async thread---");
                        dbThread.start(); // 重新启动更新线程
                    }
                    while(!asyncDataQueue.isEmpty()){
                        try {
                            int size = asyncDataQueue.size();
                            if(waitTime++>MAXWAITTIMES && size >= lastSize){
                                log.error("停止异步服务器出现异常，在指定时间内未处理完，并且至少"+WAITINTERVAL+"毫秒内没有处理数据，workerId = "+threadNum
                                        +",剩余未处理数据量："+asyncDataQueue.size());
                                while(asyncDataQueue.size() > 0){
                                    System.out.println(asyncDataQueue.take());
                                }
                                break;
                            }
                            lastSize = size;
                            Thread.sleep(WAITINTERVAL);
                        }catch (InterruptedException e){
                            if(SwallowExceptionTime++ < MAXSWALLOWSTOPEXCEPTIONTIMES){
                                continue;
                            }
                            log.error("停止异步服务器出现异常，多次出现打断异常，workerId = "+threadNum
                                    +",剩余未处理数据量："+asyncDataQueue.size());
                            break;
                        }
                    }
                    // 打断循环提交线程，如果能够被打断，是不是说明没有最后一个在处理的数据?
                    dbThread.interrupt();
                    countDownLatch.countDown();
                }
            }.start();
        }

    }
}
