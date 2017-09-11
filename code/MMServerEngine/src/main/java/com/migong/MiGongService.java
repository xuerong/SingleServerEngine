package com.migong;

import com.mm.engine.framework.control.annotation.Request;
import com.mm.engine.framework.control.annotation.Service;
import com.mm.engine.framework.data.entity.session.Session;
import com.mm.engine.framework.net.code.RetPacket;

@Service
public class MiGongService {
    @Request(opcode = 10)
    public RetPacket getMap(Object clientData, Session session){
        return null;
    }
}
