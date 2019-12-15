#!/bin/bash

bash ./clear.sh
echo "Compiling..."
export CLASSPATH=$CLASSPATH:$(find ~/Software/lucene-8.3.0/ -name "lucene*.jar" | paste -sd ":" -)
javac src/*.java
echo "Compiled!"
mv $(find src -name "*.class") .
java Main ../LuceneData
#java Main smallPosts
