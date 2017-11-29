#!/bin/bash

#kill
./shutdown.sh

nohup java -cp "./lib/*:./:./*:./WEB-INF/*" -Xmx512m -Xms512m -XX:PermSize=128M -Dfile.encoding=UTF-8 com.mm.engine.framework.server.Server | cronolog /usr/local/migong/logs/dynasty2.out.%Y%m%d%H >/dev/null 2>&1 &
tail -f /usr/local/migong/logs/dynasty2.out.$(date "+%Y%m%d%H")