#!/bin/sh
#cd `pwd`;
echo "start proto...";
#csharp_out
#protoc --csharp_out=./ ./protos/MiGongPB.proto;
#protoc --java_out=./ ./protos/MiGongPB.proto;
#python idcreator.py ./protos/MiGongPB.proto MiGongOpcode.java 12001
python ./csharp/idcreator.py ./protos/MiGongPB.proto MiGongOpcode.cs 12001 MiGongBuild.cs
echo "end proto...";
read -n1 -p "Press any key to continue...";
