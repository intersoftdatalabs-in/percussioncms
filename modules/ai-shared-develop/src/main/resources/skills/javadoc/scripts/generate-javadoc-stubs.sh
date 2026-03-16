#!/bin/bash
# generate-javadoc-stubs.sh
# Generates stub Javadoc comments for Java files missing documentation
# Usage: ./scripts/generate-javadoc-stubs.sh <java-file-or-directory>

set -e

JDK_VERSION="${JDK_VERSION:-21}"

if [ -z "$1" ]; then
    echo "Usage: $0 <java-file-or-directory> [output-file]"
    exit 1
fi

INPUT="$1"
OUTPUT="${2:-}"

# Detect JDK version from pom.xml if available
if [ -f "pom.xml" ]; then
    DETECTED_VERSION=$(grep -A1 '<source>' pom.xml 2>/dev/null | grep -oP '\d+' | head -1 || echo "21")
    if [ -n "$DETECTED_VERSION" ]; then
        JDK_VERSION="$DETECTED_VERSION"
    fi
fi

echo "Generating Javadoc stubs for JDK $JDK_VERSION..."

generate_stub() {
    local file="$1"
    local classname=$(basename "$file" .java)
    
    # Extract method signatures
    echo "/**"
    echo " * TODO: Add description for $classname"
    echo " *"
    
    # Get method signatures
    grep -E "^\s+(public|protected|private)\s+(static\s+)?(void|int|String|boolean|Object|<[^>]+>)\s+\w+\s*\(" "$file" 2>/dev/null | while read -r line; do
        method=$(echo "$line" | grep -oP '(void|int|String|boolean|Object|<[^>]+>)\s+\K\w+(?=\s*\()')
        params=$(echo "$line" | grep -oP '\(\K[^)]+(?=\))' | tr ',' '\n' | sed 's/^[[:space:]]*//' | sed 's/[[:space:]].*//')
        
        if [ -n "$method" ]; then
            echo " * @param $params"
        fi
        
        return_type=$(echo "$line" | grep -oP '(void|int|String|boolean|Object|<[^>]+>)\s+\w+\s*\(' | grep -oP '^\s*(void|int|String|boolean|Object|<[^>]+>)')
        if [ "$return_type" != "void" ]; then
            echo " * @return"
        fi
    done
    
    echo " */"
}

if [ -d "$INPUT" ]; then
    find "$INPUT" -name "*.java" -type f | while read -f; do
        if ! grep -q "^/\*\*" "$f"; then
            echo "Processing: $f"
            generate_stub "$f"
        fi
    done
elif [ -f "$INPUT" ]; then
    generate_stub "$INPUT"
else
    echo "Error: $INPUT not found"
    exit 1
fi
