echo off
mkdir beans
dir /s /B *.java > sourcefiles
javac -cp jars\* -s src\ -d beans\ @sourcefiles
