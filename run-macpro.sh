#!/bin/bash

bash ./clear.sh
echo "Compiling..."
export CLASSPATH=$CLASSPATH:$(find ~/Documents/lucene-8.3.0/ -name "lucene*.jar" | paste -sd ":" -):./packages/json-simple-1.1.1.jar
javac src/*.java
echo "Compiled!"
mv $(find src -name "*.class") .
java Main /media/joost/Data_0/python_cpp
#java Main smallPosts
