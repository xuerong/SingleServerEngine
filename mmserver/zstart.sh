#!/bin/bash

java -server -cp "./lib/*:./:./*:./WEB-INF/*" -Xmx512m -Xms512m -XX:PermSize=128M -Dfile.encoding=UTF-8 com.mm.engine.framework.server.Server | cronolog ./logs/dynasty2.out.%Y%m%d%H
