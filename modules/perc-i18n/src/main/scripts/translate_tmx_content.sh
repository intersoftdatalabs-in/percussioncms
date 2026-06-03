#!/bin/bash

# Script to translate TMX file content using trans command
# Usage: ./translate_tmx_content.sh [source_file] [target_locale]

# Check if trans command is available
if ! command -v trans &> /dev/null; then
    echo "Error: trans command not found. Please install translate-shell."
    exit 1
fi

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

# Create a temporary file for processing content
TEMP_FILE=$(mktemp)

# Process the file to translate content between <seg> tags
while IFS= read -r line; do
    # Check if this line contains a <seg> tag with content to translate
    if echo "$line" | grep -q "<seg>[^<]*</seg>" && ! echo "$line" | grep -q "<seg>{[0-9]}</seg>"; then
        # Extract the content between <seg> and </seg> using awk for safety
        CONTENT=$(echo "$line" | awk -F'<seg>' '{print $2}' | awk -F'</seg>' '{print $1}')
        
        # Skip if content is empty or contains only numbers/placeholders like {0}, {0},{1}, etc.
        if [[ -n "$CONTENT" && ! "$CONTENT" =~ ^[[:space:]]*\{[0-9]+(\,[0-9]+)*\}[[:space:]]*$ ]]; then
            # Translate the content using trans command
            TRANSLATED_CONTENT=$(trans -b :$TARGET_LOCALE "$CONTENT" 2>/dev/null)
            
            # Check if translation succeeded
            if [ $? -eq 0 ] && [ -n "$TRANSLATED_CONTENT" ]; then
                # Replace the content with translated content using awk
                line=$(echo "$line" | awk -v content="$CONTENT" -v translated="$TRANSLATED_CONTENT" '{gsub(content, translated); print}')
            else
                echo "Warning: Translation failed for '$CONTENT', keeping original"
            fi
        fi
    fi
    
    # Write line to temp file
    echo "$line" >> "$TEMP_FILE"
    
done < "$SOURCE_FILE"

# Replace the target file with processed content
mv "$TEMP_FILE" "$TARGET_FILE"

echo "Translation completed: $TARGET_FILE"