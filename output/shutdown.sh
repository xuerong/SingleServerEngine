#!/bin/bash

#kill
pid=`ps -ef |grep "com.mm.engine.framework.server.Server"|grep -v grep|awk '{print $2}'`
if [[ -z $pid ]] ; then
    echo "server is not running before"
else
    echo $pid
    kill -15 $pid
    echo "kill server before start,wait for shut down"
    for i in {1..100}
    do
       sleep 1
       pid=`ps -ef |grep "com.mm.engine.framework.server.Server"|awk '{print $2}'`
       if [[ -z $pid ]] ; then
            echo "server shut down !!!"
            break
        else
            echo "wait for server shut down "$i"s";
        fi
    done
fi