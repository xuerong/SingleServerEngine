#!/bin/sh

proto_file_name=AccountPB.proto
start_index=10001

dir=csharpfile\

opcode_class_name=csharpfile/${proto_file_name/PB.proto/Opcode.cs}
csharp_class_name=csharpfile/${proto_file_name/proto/cs}
builder_class_name=csharpfile/${proto_file_name/.proto/Builder.cs}

wine CodeEngine.exe -i:../protos/${proto_file_name} -o:${csharp_class_name} -c:csharp
wine ./csharp-tools/CodeGenerator.exe ../protos/${proto_file_name} --output=${csharp_class_name}

../pythonmac/python3.6 idcreator.py ../protos/${proto_file_name} ${opcode_class_name} ${start_index} ${builder_class_name}
