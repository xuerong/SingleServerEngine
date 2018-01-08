package com.mm.engine.framework.server.hotload;

import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.control.event.EventData;
import com.mm.engine.framework.control.event.EventService;
import com.mm.engine.framework.server.SysConstantDefine;
import com.mm.engine.framework.tool.helper.ClassHelper;
import com.sys.SysPara;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service(init = "init")
public class HotLoadService {
    private static final Logger log = LoggerFactory.getLogger(HotLoadService.class);

    ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("hot-load-thread");
            return thread;
        }
    });

    private Map<String,Field> tableFields = new HashMap<>();

    private EventService eventService;

    public void init(){
        List<Class<?>> classList = ClassHelper.getClassList("com.table");
        for(Class<?> cls : classList){
            try {
                tableFields.put(cls.getSimpleName(), cls.getField("datas"));
            }catch (NoSuchFieldException e){
                log.error("no such field,class name = "+cls.getName(),e);
            }
        }

        executorService.execute(new TableWatch());
        executorService.execute(new SysParaWatch());
    }

    class HotLoadClassLoader extends URLClassLoader {
        public HotLoadClassLoader(String path, ClassLoader parent) throws Exception {
            super(new URL[]   {   new File(path).toURI().toURL()   },parent);
        }
        public HotLoadClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
    class SysParaWatch implements Runnable{

        @Override
        public void run() {
            try {
                String pathStr = "com/sys";
                if(!new File(pathStr).exists()){
                    pathStr = "target/classes/com/sys";
                }
                // 获取文件系统的WatchService对象
                WatchService watchService = FileSystems.getDefault().newWatchService();

                Path path = Paths.get(pathStr);
                path.register(watchService
//                        , StandardWatchEventKinds.ENTRY_CREATE
                        , StandardWatchEventKinds.ENTRY_MODIFY
//                        , StandardWatchEventKinds.ENTRY_DELETE
                );

                while(true)
                {
                    // 获取下一个文件改动事件
                    WatchKey key = watchService.take();
                    List<WatchEvent<?>> list = key.pollEvents();
                    ClassLoader cl = null;
                    for (WatchEvent<?> event : list)
                    {
                        if(!event.context().toString().equals("SysPara.class")){
                            continue;
                        }
                        String className = event.context().toString().replace(".class","");
                        Field field = SysPara.class.getField("paras");
                        if(cl == null) {
                            cl = new HotLoadClassLoader(pathStr.replace("com/sys",""), null);
                        }

                        System.out.println(SysPara.paras.get("ccc"));
                        Class cls = cl.loadClass("com.sys."+className);
                        Map<String,String> newDatas = (Map<String,String>)cls.getField("paras").get(null);
                        field.set(newDatas,cls.getField("paras").get(null));

                        EventData eventData = new EventData(SysConstantDefine.Event_SysParaChange);
                        eventService.fireEvent(eventData);

                    }
                    // 重设WatchKey
                    boolean valid = key.reset();
                    // 如果重设失败，退出监听
                    int resetTime = 0;
                    while (!valid && resetTime<10)
                    {
                        valid = key.reset();
                    }
                    if(!valid){
                        log.error("reset watch fail");
                    }
                }
            }catch (Throwable e){
                e.printStackTrace();
            }
        }
    }
    class TableWatch implements Runnable{

        @Override
        public void run(){
            try {
                String pathStr = "com/table";
                if(!new File(pathStr).exists()){
                    pathStr = "target/classes/com/table";
                }
                // 获取文件系统的WatchService对象
                WatchService watchService = FileSystems.getDefault().newWatchService();

                Path path = Paths.get(pathStr);
                System.out.println(path.toAbsolutePath());
                path.register(watchService
//                        , StandardWatchEventKinds.ENTRY_CREATE
                        , StandardWatchEventKinds.ENTRY_MODIFY
//                        , StandardWatchEventKinds.ENTRY_DELETE
                );

                while(true)
                {
                    // 获取下一个文件改动事件
                    WatchKey key = watchService.take();
                    List<WatchEvent<?>> list = key.pollEvents();
                    System.out.println("------------------------------------table file num:"+list.size());
                    ClassLoader cl = null;
                    for (WatchEvent<?> event : list)
                    {
                        if(!event.context().toString().endsWith(".class")){
                            continue;
                        }
                        String className = event.context().toString().replace(".class","");
                        Field field = tableFields.get(className);
                        if(field == null){
                            log.warn("table is not exist,name = {}",className);
                            continue;
                        }
                        if(cl == null) {
                            cl = new HotLoadClassLoader(pathStr.replace("com/table",""), null);
                        }
                        long time1 = System.nanoTime();
                        Class cls = cl.loadClass("com.table."+className);
                        long time2 = System.nanoTime();
                        Object[] newDatas = (Object[])cls.getField("datas").get(null);
//                        field.set(null,cls.getField("datas").get(null)); // 不同类加载器加载的类不能赋值
                        // 下面只好用深度赋值的方式
                        Class<?> oldCls = field.getDeclaringClass();
                        Object[] temNewDatas = (Object[])Array.newInstance(oldCls,newDatas.length);
                        Map<Field,Field> newToOld = new HashMap<>();

                        Field[] fields = cls.getDeclaredFields();
                        int fieldCount = 0;
                        for(Field f : fields){
                            boolean isStatic = Modifier.isStatic(f.getModifiers());
                            if(!isStatic) {
                                f.setAccessible(true);
                                Field oldField = oldCls.getDeclaredField(f.getName());
                                oldField.setAccessible(true);
                                newToOld.put(f, oldField);
                                fieldCount++;
                            }
                        }
                        Constructor<?> constructor = oldCls.getDeclaredConstructors()[0];
                        int dataCount = 0;
                        for(Object newData : newDatas){
                            Object[] args = new Object[fieldCount];
                            int i=0;
                            for(Field f : fields){
                                boolean isStatic = Modifier.isStatic(f.getModifiers());
                                if(!isStatic) {
                                    Object fieldOb = f.get(newData);
                                    args[i++] = fieldOb;
                                }
                            }
                            temNewDatas[dataCount++] = constructor.newInstance(args);

                        }
                        field.set(null,temNewDatas);

                        long time3 = System.nanoTime();

                        EventData eventData = new EventData(SysConstantDefine.Event_TableChange);
                        eventData.setData(oldCls);
                        eventService.fireEvent(eventData);

                        log.info("hot load table {} success,use time:load class = {},parse = {}",className,time2-time1,time3 - time2);
                    }
                    // 重设WatchKey
                    boolean valid = key.reset();
                    // 如果重设失败，退出监听
                    int resetTime = 0;
                    while (!valid && resetTime<10)
                    {
                        valid = key.reset();
                    }
                    if(!valid){
                        log.error("reset watch fail");
                    }
                }

            }catch (Throwable e){
                e.printStackTrace();
            }
        }
    }
}
