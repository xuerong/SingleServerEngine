#!/bin/sh

proto_file_name=AccountPB.proto
start_index=10001

opcode_class_name=${proto_file_name/PB.proto/Opcode.java}
java_class_name=${proto_file_name/proto/java}

wine protoc.exe --java_out=./ ./protos/${proto_file_name}
cp ./com/protocol/${java_class_name} ../../src/main/java/com/protocol/${java_class_name}

./pythonmac/python3.6 idcreator.py ./protos/${proto_file_name} ${opcode_class_name} ${start_index}
cp ./com/protocol/${opcode_class_name} ../../src/main/java/com/protocol/${opcode_class_name}