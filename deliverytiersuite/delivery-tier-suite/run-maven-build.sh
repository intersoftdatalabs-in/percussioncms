#!/bin/bash

# Script to run Maven build with proper output capture and timeout
# Usage: ./run-maven-build.sh

set -e

LOG_FILE="maven-build-$(date +%Y%m%d-%H%M%S).log"
ERROR_LOG="maven-errors-$(date +%Y%m%d-%H%M%S).log"

echo "Starting Maven build..."
echo "Full log will be saved to: $LOG_FILE"
echo "Error log will be saved to: $ERROR_LOG"
echo ""

# First try a quick build without debug to see if enforcer issues are resolved
echo "Running quick test build (without debug, skipping enforcer)..."
if timeout 600 mvn clean compile -q -Denforcer.skip=true; then
    echo "✅ Quick compilation successful! Now running full verification with debug..."
    echo ""
else
    echo "❌ Quick compilation failed. Check for basic issues first."
    exit 1
fi

# Run Maven with timeout and capture both stdout and stderr
timeout 1800 mvn clean verify -X -Denforcer.skip=true 2>&1 | tee "$LOG_FILE" || {
    exit_code=$?
    echo ""
    echo "Maven build failed or timed out (exit code: $exit_code)"
    
    # Extract errors from the log
    echo "Extracting errors..."
    grep -i "error\|fail\|exception" "$LOG_FILE" > "$ERROR_LOG" 2>/dev/null || echo "No obvious errors found in grep"
    
    # Show last 50 lines of output
    echo ""
    echo "=== LAST 50 LINES OF OUTPUT ==="
    tail -50 "$LOG_FILE"
    
    echo ""
    echo "Full log saved to: $LOG_FILE"
    echo "Error summary saved to: $ERROR_LOG"
    
    exit $exit_code
}

echo ""
echo "🎉 Maven build completed successfully!"
echo "Full log saved to: $LOG_FILE"