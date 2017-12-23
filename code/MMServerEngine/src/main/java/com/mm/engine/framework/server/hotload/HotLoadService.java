package com.mm.engine.framework.server.hotload;

import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.tool.helper.ClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service(init = "init")
public class HotLoadService {
    private static final Logger log = LoggerFactory.getLogger(HotLoadService.class);

    ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("hot-load-thread");
            return thread;
        }
    });

    private Map<String,Field> tableFields = new HashMap<>();

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
    }

    class HotLoadClassLoader extends URLClassLoader {
        public HotLoadClassLoader(String path, ClassLoader parent) throws Exception {
            super(new URL[]   {   new File(path).toURI().toURL()   },parent);
        }
        public HotLoadClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
    class TableWatch implements Runnable{

        @Override
        public void run(){
            try {
                // 获取文件系统的WatchService对象
                WatchService watchService = FileSystems.getDefault().newWatchService();

                Path path = Paths.get("./com/table");
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
                    System.out.println("------------------------------------change file num:"+list.size());
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
                            cl = new HotLoadClassLoader("./", null);
                        }
                        Class cls = cl.loadClass("com.table."+className);
                        field.set(null,cls.getField("datas").get(null));
                    }
                    // 重设WatchKey
                    boolean valid = key.reset();
                    // 如果重设失败，退出监听
                    if (!valid)
                    {
                        break;
                    }
                }

            }catch (Throwable e){
                e.printStackTrace();
            }
        }
    }
}
