@echo off
cd /d "%~dp0"
:: excel文件
set excel_file_name=PeckTable.xlsx


::..\python\python.exe table.py .\tables\%excel_file_name%
C:\Python27\python.exe table.py .\tables\%excel_file_name%

//pause