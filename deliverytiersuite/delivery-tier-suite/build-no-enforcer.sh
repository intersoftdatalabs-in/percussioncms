#!/bin/bash
# Build script that skips the enforcer plugin to isolate build issues

echo "Running Maven build without enforcer plugin..."
mvn clean verify -Denforcer.skip=true -X 2>&1 | tee build-no-enforcer.log

echo "Build completed. Check build-no-enforcer.log for details."