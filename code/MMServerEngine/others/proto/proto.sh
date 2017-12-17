#!/bin/sh
#cd `pwd`;
#echo "start proto...";
##csharp_out
#protoc --csharp_out=./ ./protos/AccountPB.proto;
##protoc --java_out=./ ./protos/MiGongPB.proto;
##python idcreator.py ./protos/MiGongPB.proto MiGongOpcode.java 12001
#python ./csharp/idcreator.py ./protos/AccountPB.proto AccountOpcode.cs 10001 AccountBuild.cs
#echo "end proto...";
#read -n1 -p "Press any key to continue...";


proto_file_name=MiGongPB.proto
start_index=12001

opcode_class_name=${proto_file_name/PB.proto/Opcode.java}
#opcode_class_name=%proto_file_name:PB.proto=Opcode.java%
java_class_name=${proto_file_name/proto/java}
#java_class_name=%proto_file_name:proto=java%

wine protoc.exe --java_out=./ ./protos/${proto_file_name}
cp ./com/protocol/${java_class_name} ../../src/main/java/com/protocol/${java_class_name}

./pythonmac/python3.6 idcreator.py ./protos/${proto_file_name} ${opcode_class_name} ${start_index}
cp ./com/protocol/${opcode_class_name} ../../src/main/java/com/protocol/${opcode_class_name}

#pause