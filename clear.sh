#!/bin/bash

echo "Clearing..."
rm -rf .build
rm -f $(find . -name "*.class")
rm -f $(find src/com/stackoverflow -name "*.class")
rm -f $(find data -name "*.json")
echo "Cleared!"
