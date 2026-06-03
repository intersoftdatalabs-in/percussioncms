#!/bin/bash

# Simple script to translate TMX files using trans command
# Usage: ./translate_tmx_simple.sh [source_file] [target_locale]

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

echo "Basic file processing completed: $TARGET_FILE"

# Now process the content segments - this is tricky with sed, so we'll use a different approach
# Let's use awk to process the file and translate the content between <seg> and </seg> tags

# Create a temporary file
TEMP_FILE=$(mktemp)

# Process the file with awk
awk -v locale="$TARGET_LOCALE" '
BEGIN {
    # Initialize trans command availability
    if (system("command -v trans >/dev/null 2>&1") != 0) {
        print "Error: trans command not found" > "/dev/stderr"
        exit 1
    }
}

/<seg>[^<]*<\/seg>/ {
    # Extract the content between <seg> and </seg>
    match($0, /<seg>([^<]*)<\/seg>/, arr)
    if (arr[1] != "" && arr[1] !~ /^\{[0-9]+(\,[0-9]+)*\}$/) {
        # This is text that needs translation
        text_to_translate = arr[1]
        
        # Translate using trans command
        cmd = "trans -b :" locale " \"" text_to_translate "\""
        cmd | getline translated_text
        close(cmd)
        
        # Check if translation succeeded
        if ($? != 0) {
            translated_text = text_to_translate
            print "Warning: Translation failed for: " text_to_translate > "/dev/stderr"
        }
        
        # Replace the segment content
        $0 = substr($0, 1, RSTART-4) "<seg>" translated_text "</seg>" substr($0, RSTART+RLENGTH+4)
    }
    # If it's just placeholders like {0}, leave it unchanged
}

{ print }
' "$TARGET_FILE" > "$TEMP_FILE" && mv "$TEMP_FILE" "$TARGET_FILE"

echo "Translation completed: $TARGET_FILE"