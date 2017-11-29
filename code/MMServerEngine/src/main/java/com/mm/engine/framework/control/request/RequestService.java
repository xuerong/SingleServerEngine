package com.mm.engine.framework.control.request;

import com.mm.engine.framework.control.ServiceHelper;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;
import com.mm.engine.framework.security.exception.MMException;
import com.mm.engine.framework.tool.helper.BeanHelper;
import com.protocol.MiGongOpcode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    }


    public RetPacket handle(int opcode,Object clientData, Session session) throws Exception{
        RequestHandler handler = handlerMap.get(opcode);
        if(handler == null){
            throw new MMException("can't find handler of "+opcode);
        }
        // 显示访问信息：玩家id，session，opcode
        log.info(
                new StringBuilder("cmd:").append(opcode).append("|accountId:")
                .append(session.getAccountId()).toString()
        );
        return handler.handle(opcode,clientData,session);
    }
}
