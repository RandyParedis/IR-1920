#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/

mvn clean
mvn install

java -jar target/searcher.jar --type custom -s true -n 7
