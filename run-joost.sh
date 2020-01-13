#!/bin/bash

bash ./clear.sh
echo "Compiling..."
export CLASSPATH=$CLASSPATH:$(find ~/Documents/Overig/lucene-8.3.0/ -name "lucene*.jar" | paste -sd ":" -):~/Documents/Overig/json-simple-1.1.1.jar
javac src/*.java
echo "Compiled!"
mv $(find src -name "*.class") .
java Main ~/Documents/UAntwerpen/M_DataScience/Information\ Retrieval/data/python_cpp
#java Main smallPosts