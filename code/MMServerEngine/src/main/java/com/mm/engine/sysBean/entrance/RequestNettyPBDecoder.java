package com.mm.engine.sysBean.entrance;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created by a on 2016/9/19.
 */
public class RequestNettyPBDecoder extends ByteToMessageDecoder {
    private static final int headSize = 12; // body length + opcode
    int size = headSize;
    boolean isReadHead = false;
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
        ByteBuf b = null;
        try {
//        System.out.println(in.capacity()+";"+in.getClass());
            int readAble = in.readableBytes();
//        System.out.println("readAble:"+readAble);
            if (readAble < size) {
                return;
            }
            int opcode = 0, id = 0;
            if (!isReadHead) {
                size = in.readInt();
                opcode = in.readInt();
                id = in.readInt();

                isReadHead = true;
                if (size > readAble - headSize) {
                    return;
                }
            }
            b = in.readBytes(size); // 这里有data
            byte[] bbb = new byte[size];
            b.getBytes(0, bbb);
            // add之后好像in就被重置了
            NettyPBPacket nettyPBPacket = new NettyPBPacket();
            nettyPBPacket.setData(bbb);
            nettyPBPacket.setOpcode(opcode);
            nettyPBPacket.setId(id);
            list.add(nettyPBPacket);
            // 清理临时变量
            size = headSize;
            isReadHead = false;
        }catch (Throwable e){
            throw e;
        }finally {
            if(b != null){
                b.release();
            }
//            in.release();
        }
    }
}
