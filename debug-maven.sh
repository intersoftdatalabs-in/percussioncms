#!/bin/bash

# Script to debug Maven build issues
cd /home/nate/projects/percussioncms/deliverytiersuite/delivery-tier-suite

echo "Capturing Maven build output with full error details..."

# Run Maven with detailed error reporting
mvn clean verify -X -e 2>&1 | tee full-maven-output.log

# Extract just the error/failure information
echo -e "\n\n=== EXTRACTING ERRORS AND FAILURES ==="
grep -A 10 -B 5 "\[ERROR\]" full-maven-output.log > maven-errors.log
grep -A 10 -B 5 "FAILURE" full-maven-output.log >> maven-errors.log
grep -A 10 -B 5 "Failed" full-maven-output.log >> maven-errors.log

echo "Full output saved to: full-maven-output.log"
echo "Errors extracted to: maven-errors.log"

# Show the last part of the output to see the final error
echo -e "\n=== LAST 50 LINES OF OUTPUT ==="
tail -50 full-maven-output.log