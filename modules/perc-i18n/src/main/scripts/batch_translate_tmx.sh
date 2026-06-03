#!/bin/bash

# Script to batch translate all TMX files in the i18n directory
# Usage: ./batch_translate_tmx.sh [source_locale]

# Check if trans command is available
if ! command -v trans &> /dev/null; then
    echo "Error: trans command not found. Please install translate-shell."
    exit 1
fi

# Directory containing TMX files
I18N_DIR="/home/nate/projects/percussioncms/modules/perc-i18n/src/main/resources/i18n"

# Check if directory exists
if [ ! -d "$I18N_DIR" ]; then
    echo "Error: Directory '$I18N_DIR' not found."
    exit 1
fi

# Default source locale is en_US
SOURCE_LOCALE="${1:-en_US}"

# Check if source file exists
SOURCE_FILE="${I18N_DIR}/${SOURCE_LOCALE}.tmx"
if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: Source file '$SOURCE_FILE' not found."
    exit 1
fi

# List of target locales to generate
TARGET_LOCALES=("en_GB" "es_ES" "es_MX" "es_CL" "pt_BR" "pt_PT" "fr_FR" "fr_CA" "de_DE" "ja-JP" "hi_IN" "it_IT" "nl_NL" "tr_TR")

echo "Starting batch translation from $SOURCE_LOCALE to ${#TARGET_LOCALES[@]} target locales..."

# Process each target locale
for TARGET_LOCALE in "${TARGET_LOCALES[@]}"; do
    # Skip if target is same as source
    if [ "$SOURCE_LOCALE" = "$TARGET_LOCALE" ]; then
        echo "Skipping $TARGET_LOCALE (same as source)"
        continue
    fi
    
    TARGET_FILE="${I18N_DIR}/${TARGET_LOCALE}.tmx"
    
    # Skip if target file already exists
    if [ -f "$TARGET_FILE" ]; then
        echo "Skipping $TARGET_LOCALE (file already exists)"
        continue
    fi
    
    echo "Processing $TARGET_LOCALE..."
    
    # Call the translation script
    python3 "${I18N_DIR}/../scripts/translate_tmx.py" "$SOURCE_FILE" "$TARGET_LOCALE"
    
    if [ $? -eq 0 ]; then
        echo "✓ Completed $TARGET_LOCALE"
    else
        echo "✗ Failed $TARGET_LOCALE"
    fi
done

echo "Batch translation process completed."