#!/bin/sh
#cd `pwd`;
echo "start proto...";
#csharp_out
protoc --csharp_out=./ ./protos/AccountPB.proto;
#protoc --java_out=./ ./protos/MiGongPB.proto;
#python idcreator.py ./protos/MiGongPB.proto MiGongOpcode.java 12001
python ./csharp/idcreator.py ./protos/AccountPB.proto AccountOpcode.cs 12001 AccountBuild.cs
echo "end proto...";
read -n1 -p "Press any key to continue...";
