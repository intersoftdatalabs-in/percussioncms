#!/bin/bash

# Script to clean Maven output of ANSI color codes and extract key information

if [ $# -eq 0 ]; then
    echo "Usage: $0 <maven_output_file>"
    echo "Example: $0 output.txt"
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="${INPUT_FILE%.txt}_clean.txt"

if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: File $INPUT_FILE not found"
    exit 1
fi

echo "Cleaning Maven output from $INPUT_FILE..."

# Remove ANSI color codes and clean up the output
sed -r "s/\x1B\[[0-9;]*[mK]//g" "$INPUT_FILE" > "$OUTPUT_FILE"

echo "Cleaned output saved to: $OUTPUT_FILE"

# Extract key information
echo ""
echo "=== BUILD SUMMARY ==="

# Look for build failures
if grep -q "BUILD FAILURE" "$OUTPUT_FILE"; then
    echo "❌ BUILD FAILED"
    echo ""
    echo "Error messages:"
    grep -A 5 -B 2 "ERROR\|FAILURE\|Failed to execute goal" "$OUTPUT_FILE" | head -20
else
    echo "✅ No build failure found in current output"
fi

# Show reactor build order
echo ""
echo "=== REACTOR BUILD ORDER ==="
grep -A 20 "Reactor Build Order:" "$OUTPUT_FILE" | head -15

# Show current progress
echo ""
echo "=== CURRENT PROGRESS ==="
grep "Building.*\[.*\]" "$OUTPUT_FILE" | tail -5

# Check for warnings
echo ""
echo "=== WARNINGS ==="
grep "WARNING" "$OUTPUT_FILE" | head -10

echo ""
echo "Full cleaned output is in: $OUTPUT_FILE"