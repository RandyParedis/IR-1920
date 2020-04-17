#!/bin/bash

#for i in {}
for i in {51053551,52389692,47856273,42273302,34404465,50490944,7003832}
do
	google-chrome question$i.xml > /dev/null
done
