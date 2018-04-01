package com.mm.engine.framework.control.request;

import com.mm.engine.framework.control.ServiceHelper;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.mm.engine.framework.tool.helper.ClassHelper;
import com.protocol.MiGongOpcode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Administrator on 2015/11/17.
 */
@Service(init = "init")
public class RequestService {
    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private Set<Integer> donNotPrintOpcode = new HashSet<Integer>(){
        {
//            add(MiGongOpcode.CSCommon)
        }
    };

    private Map<Integer,String> opcodeNames = new HashMap<>();

    private Map<Integer,RequestHandler> handlerMap=new HashMap<Integer,RequestHandler>();
    public void init(){
        TIntObjectHashMap<Class<?>> requestHandlerClassMap = ServiceHelper.getRequestHandlerMap();
        requestHandlerClassMap.forEachEntry(new TIntObjectProcedure<Class<?>>(){
            @Override
            public boolean execute(int i, Class<?> aClass) {
                handlerMap.put(i, (RequestHandler)BeanHelper.getServiceBean(aClass));
                return true;
            }
        });
        List<Class<?>> classes = ClassHelper.getClassListEndWith("com.protocol","Opcode");
        for (Class<?> cls:classes) {

            Field[] fields = cls.getFields();
            for(Field field : fields){
                try {
                    opcodeNames.put(field.getInt(null),field.getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public RetPacket handle(int opcode,Object clientData, Session session) throws Exception{
        RequestHandler handler = handlerMap.get(opcode);
        if(handler == null){
            throw new MMException("can't find handler of "+opcode);
        }
        // 显示访问信息：玩家id，session，opcode
        log.info(
                new StringBuilder("cmd:").append(opcodeNames.get(opcode)).append("(").append(opcode).append(")|accountId:")
                .append(session.getAccountId()).toString()
        );
        // 如果属于加锁失败（事务中）导致的，在这里重新执行，这里只是确保用户访问的事务能够被重新执行
        RetPacket ret;
        int count = 0;
        while (true) {
            try {
                ret = handler.handle(opcode, clientData, session);
            } catch (MMException e) {
                if (e.getExceptionType() == MMException.ExceptionType.TxCommitFail) {
                    if(count++<2) {
                        continue;
                    }else {
                        log.error("tx commit fail after 3 times");
                        throw e;
                    }
                }else{
                    throw e;
                }
            }
            break;
        }
        return ret;
    }

    public Map<Integer, String> getOpcodeNames() {
        return opcodeNames;
    }
}
