#!/bin/bash

echo "Clearing..."
rm -f $(find . -name "*.class")
rm -f $(find src -name "*.class")
echo "Cleared!"
