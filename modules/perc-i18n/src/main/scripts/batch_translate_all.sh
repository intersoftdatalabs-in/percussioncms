#!/bin/bash

# Script to batch translate all TMX files in the i18n directory
# This script processes each TMX file and translates content based on the locale in the filename

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

echo "Starting batch translation process..."

# Process each TMX file in the directory
for tmx_file in "$I18N_DIR"/*.tmx; do
    # Skip if no TMX files found
    [ -e "$tmx_file" ] || continue
    
    # Get filename without path and extension
    filename=$(basename "$tmx_file" .tmx)
    
    # Skip ResourceBundle.tmx as it's not a locale-specific file
    if [ "$filename" = "ResourceBundle" ]; then
        echo "Skipping $filename.tmx (not a locale file)"
        continue
    fi
    
    # Extract locale from filename (everything after the last underscore)
    # Handle special cases like ja-JP
    if [[ "$filename" =~ _([^_]+)$ ]]; then
        locale="${BASH_REMATCH[1]}"
    else
        # If no underscore, the whole filename is the locale
        locale="$filename"
    fi
    
    echo "Processing $filename.tmx for locale $locale"
    
    # Create a temporary file for processing
    temp_file=$(mktemp)
    
    # Process the TMX file line by line
    while IFS= read -r line; do
        # Update tuv xml:lang attribute
        if echo "$line" | grep -q "<tuv xml:lang="; then
            line=$(echo "$line" | sed "s#<tuv xml:lang=\"[^\"]*\"#<tuv xml:lang=\"${locale}\"#")
        fi
        
        # Check if this line contains a <seg> tag that needs translation
        if echo "$line" | grep -q "<seg>[^<]*</seg>" && ! echo "$line" | grep -q "<seg>{[0-9]}</seg>"; then
            # Extract the text to translate
            TEXT_TO_TRANSLATE=$(echo "$line" | sed -n 's#.*<seg>\([^<]*\)</seg>.*#\1#p')
            
            # Skip if text is empty or contains only placeholders like {0} or {0},{1}, etc.
            if [[ -n "$TEXT_TO_TRANSLATE" && ! "$TEXT_TO_TRANSLATE" =~ ^[[:space:]]*\{[0-9]+(\,[0-9]+)*\}[[:space:]]*$ ]]; then
                # Translate the text using trans command with stdin to avoid shell issues
                TRANSLATED_TEXT=$(echo "$TEXT_TO_TRANSLATE" | trans -b :$locale 2>/dev/null)
                
                # Check if translation succeeded
                if [ $? -eq 0 ] && [ -n "$TRANSLATED_TEXT" ]; then
                    # Remove locale prefix if present (like ":es")
                    if echo "$TRANSLATED_TEXT" | grep -q "^:[a-z][a-z]-[A-Z][A-Z]"; then
                        TRANSLATED_TEXT=$(echo "$TRANSLATED_TEXT" | sed 's/^:[a-z][a-z]-[A-Z][A-Z]//')
                    fi
                    # Replace the segment with translated text
                    line=$(echo "$line" | sed "s#$TEXT_TO_TRANSLATE#$TRANSLATED_TEXT#")
                else
                    echo "Warning: Translation failed for: $TEXT_TO_TRANSLATE"
                fi
            fi
        fi
        
        # Update the header language code
        if echo "$line" | grep -q "<prop type=\"supportedlanguage\">"; then
            line=$(echo "$line" | sed "s#<prop type=\"supportedlanguage\">[^<]*</prop>#<prop type=\"supportedlanguage\">${locale}</prop>#")
        fi
        
        # Write line to temp file
        echo "$line" >> "$temp_file"
        
    done < "$tmx_file"
    
    # Replace the original file with processed content
    mv "$temp_file" "$tmx_file"
    
    echo "Completed processing $filename.tmx"
done

echo "All TMX files processed successfully."