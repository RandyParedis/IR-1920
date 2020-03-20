#!/bin/bash

bash ./clear.sh
echo "Compiling..."
mkdir -vp .build
export CLASSPATH=$CLASSPATH:$(find ~/Software/lucene-8.3.0/ -name "lucene*.jar" | paste -sd ":" -):packages/json-simple-1.1.1.jar
javac -d .build/ src/com/stackoverflow/*/*.java src/com/stackoverflow/*.java
cd .build
echo "Main-Class: com.stackoverflow.Main" >> ./manifest.mf
echo "Class-Path: packages/json-simple-1.1.1.jar" >> ./manifest.mf
echo -e "  $(find ~/Software/lucene-8.3.0/ -name "lucene*.jar" | paste -sd "+" - | sed 's/+/\
  /g')" >> ./manifest.mf
jar -cmf ./manifest.mf project.jar com
echo "Compiled!"
cd ..
java -jar .build/project.jar
