#!/bin/sh
#cd `pwd`;
echo "start proto...";
protoc --java_out=./ ./protos/MiGongPB.proto;
python idcreator.py ./protos/MiGongPB.proto MiGongOpcode.java 12001
echo "end proto...";
read -n1 -p "Press any key to continue...";
