#!/bin/bash

# Script to debug Maven build failures
# This will capture both stdout and stderr, and show the failure point

echo "Running Maven clean verify with debug output..."
echo "This may take several minutes..."

# Run Maven and capture all output
mvn clean verify -X -e 2>&1 | tee full-debug-output.txt

# Check the exit code
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo ""
    echo "============================================"
    echo "BUILD FAILED - Extracting error information"
    echo "============================================"
    
    # Extract the last few hundred lines around any ERROR messages
    echo ""
    echo "--- ERRORS FOUND ---"
    grep -n -i "error\|failed\|exception" full-debug-output.txt | tail -20
    
    echo ""
    echo "--- BUILD FAILURE SUMMARY ---"
    # Get the reactor summary section
    awk '/BUILD FAILURE/,/Total time:/' full-debug-output.txt
    
    echo ""
    echo "--- LAST 50 LINES OF OUTPUT ---"
    tail -50 full-debug-output.txt
    
else
    echo "BUILD SUCCESSFUL!"
fi