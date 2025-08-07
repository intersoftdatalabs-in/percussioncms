#!/bin/bash

# Script to debug Maven build issues
# This will capture both stdout and stderr, and highlight error patterns

echo "Starting Maven build debug analysis..."
echo "Build started at: $(date)"
echo "Java version: $(java -version 2>&1 | head -1)"
echo "Maven version: $(mvn -version | head -1)"
echo "Working directory: $(pwd)"
echo "=================================="

# Run maven with debug output and capture everything
mvn clean verify -X 2>&1 | tee full-debug-output.txt

# Check the exit code
EXIT_CODE=$?
echo ""
echo "=================================="
echo "Build completed at: $(date)"
echo "Exit code: $EXIT_CODE"

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "BUILD FAILED - Extracting error information..."
    echo "=================================="
    
    # Extract error patterns from the output
    echo "ERRORS found:"
    grep -n "ERROR\|FAILED\|Exception\|Error" full-debug-output.txt | tail -20
    
    echo ""
    echo "BUILD FAILURE patterns:"
    grep -n -A 5 -B 5 "BUILD FAILURE\|FAILED\|Exception.*:" full-debug-output.txt | tail -50
    
    echo ""
    echo "Last 100 lines of output:"
    tail -100 full-debug-output.txt
else
    echo "BUILD SUCCESSFUL"
fi