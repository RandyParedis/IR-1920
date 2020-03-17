#!/bin/bash
input="./questions.txt"
echo "$input"

while IFS= read -r line
do
  for i in $(echo $line | sed "s/,/ /g")
  do
    cp `find /media/joost/Data0/python_cpp -name "question$i.txt"` ~/Documents/UA/IR-1920/smallPosts
  done
done < "$input"
