#!/bin/bash
# Environment setup script for Linux/macOS
# Sets JAVA_HOME to JAVA_HOME_21 for JDK 21 compatibility
# Run as: ./mvn-env.sh <maven-args>

set -e

# Check if JAVA_HOME_21 is set
if [[ -z "${JAVA_HOME_21}" ]]; then
    echo "Error: JAVA_HOME_21 environment variable is not set."
    echo "Please set JAVA_HOME_21 to the path of your JDK 21 installation."
    echo "Example: export JAVA_HOME_21=/usr/lib/jvm/java-21-openjdk-amd64"
    exit 1
fi

# Verify the JDK 21 path exists
if [[ ! -d "${JAVA_HOME_21}" ]]; then
    echo "Error: JDK 21 not found at ${JAVA_HOME_21}"
    echo "Please ensure JAVA_HOME_21 points to a valid JDK 21 installation."
    exit 1
fi

# Set JAVA_HOME
export JAVA_HOME="${JAVA_HOME_21}"

echo "Using JDK 21 at ${JAVA_HOME}"

# Run Maven wrapper with all arguments
exec ./mvnw "$@"