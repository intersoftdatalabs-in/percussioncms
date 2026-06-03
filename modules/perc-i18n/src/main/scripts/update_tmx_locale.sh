#!/bin/bash

# Script to update TMX file locale codes
# Usage: ./update_tmx_locale.sh [source_file] [target_locale]

# Check arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <source_tmx_file> <target_locale>"
    echo "Example: $0 en_US.tmx en_GB"
    exit 1
fi

SOURCE_FILE="$1"
TARGET_LOCALE="$2"

# Check if source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: Source file '$SOURCE_FILE' not found."
    exit 1
fi

# Extract directory and filename
SOURCE_DIR=$(dirname "$SOURCE_FILE")
SOURCE_BASE=$(basename "$SOURCE_FILE" .tmx)
TARGET_FILE="${SOURCE_DIR}/${SOURCE_BASE}_${TARGET_LOCALE}.tmx"

# Create target file by copying source
cp "$SOURCE_FILE" "$TARGET_FILE"

# Update the header language code
sed -i "s|<prop type=\"supportedlanguage\">[^<]*</prop>|<prop type=\"supportedlanguage\">${TARGET_LOCALE}</prop>|" "$TARGET_FILE"

# Update tuv xml:lang attributes
sed -i "s|<tuv xml:lang=\"[^\"]*\"|<tuv xml:lang=\"${TARGET_LOCALE}\"|g" "$TARGET_FILE"

echo "Locale updated: $TARGET_FILE"