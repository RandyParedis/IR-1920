#!/bin/sh
# $1 : class.to.run
# $2 : config file
java -Xmx4g -Djava.library.path="/usr/local/lib" -cp target/ir-utils-0.0.1-SNAPSHOT.jar:./target/classes/* $1 $2
