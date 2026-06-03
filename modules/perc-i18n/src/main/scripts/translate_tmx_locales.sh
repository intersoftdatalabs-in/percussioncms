#!/bin/bash

# Script to translate TMX files to their respective locales
# Usage: ./translate_tmx_locales.sh [directory]
# If directory is not provided, uses the current directory.

# Check if trans command is available
if ! command -v trans &> /dev/null; then
    echo "Error: trans command not found. Please install translate-shell."
    exit 1
fi

# Set the directory to process
if [ "$#" -eq 1 ]; then
    I18N_DIR="$1"
else
    I18N_DIR="$(pwd)"
fi

# Check if directory exists
if [ ! -d "$I18N_DIR" ]; then
    echo "Error: Directory '$I18N_DIR' not found."
    exit 1
fi

echo "Starting translation of TMX files in '$I18N_DIR' to their respective locales..."

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
    
    # The target locale is the filename (e.g., en_GB, es_ES, etc.)
    TARGET_LOCALE="$filename"
    
    echo "Processing $filename.tmx for locale $TARGET_LOCALE"
    
    # Create a temporary file for processing
    temp_file=$(mktemp)
    
    # Process the TMX file line by line
    while IFS= read -r line; do
        # Update tuv xml:lang attribute
        if echo "$line" | grep -q "<tuv xml:lang="; then
            line=$(echo "$line" | sed "s#<tuv xml:lang=\"[^\"]*\"#<tuv xml:lang=\"${TARGET_LOCALE}\"#")
        fi
        
        # Check if this line contains a <seg> tag that needs translation
        if echo "$line" | grep -q "<seg>[^<]*</seg>" && ! echo "$line" | grep -q "<seg>{[0-9]}</seg>"; then
            # Extract the text to translate
            TEXT_TO_TRANSLATE=$(echo "$line" | sed -n 's#.*<seg>\([^<]*\)</seg>.*#\1#p')
            
            # Skip if text is empty or contains only placeholders like {0} or {0},{1}, etc.
            if [[ -n "$TEXT_TO_TRANSLATE" && ! "$TEXT_TO_TRANSLATE" =~ ^[[:space:]]*\{[0-9]+(\,[0-9]+)*\}[[:space:]]*$ ]]; then
                # Translate the text using trans command with stdin to avoid shell issues
                TRANSLATED_TEXT=$(echo "$TEXT_TO_TRANSLATE" | trans -b :$TARGET_LOCALE 2>/dev/null)
                
                # Check if translation succeeded
                if [ $? -eq 0 ] && [ -n "$TRANSLATED_TEXT" ]; then
                    # Remove locale prefix if present (like ":es" or ":fr_FR")
                    # The trans command sometimes outputs a prefix like ":es" followed by the actual translation
                    # We need to remove this prefix if it exists at the start of the line
                    if echo "$TRANSLATED_TEXT" | grep -q "^:[a-z][a-z]\(-[A-Z][A-Z]\)\?$"; then
                        # The output is just the locale prefix, meaning translation failed or empty
                        # Keep original text
                        echo "Warning: Translation resulted in only locale prefix for: $TEXT_TO_TRANSLATE"
                    else
                        # Remove any locale prefix that might be at the start of a line
                        TRANSLATED_TEXT=$(echo "$TRANSLATED_TEXT" | sed -e 's/^:[a-z][a-z]\(-[A-Z][A-Z]\)\?//')
                        # Replace the segment with translated text
                        line=$(echo "$line" | sed "s#$TEXT_TO_TRANSLATE#$TRANSLATED_TEXT#")
                    fi
                else
                    echo "Warning: Translation failed for: $TEXT_TO_TRANSLATE"
                fi
            fi
        fi
        
        # Update the header language code
        if echo "$line" | grep -q "<prop type=\"supportedlanguage\">"; then
            line=$(echo "$line" | sed "s#<prop type=\"supportedlanguage\">[^<]*</prop>#<prop type=\"supportedlanguage\">${TARGET_LOCALE}</prop>#")
        fi
        
        # Write line to temp file
        echo "$line" >> "$temp_file"
        
    done < "$tmx_file"
    
    # Replace the original file with processed content
    mv "$temp_file" "$tmx_file"
    
    echo "Completed processing $filename.tmx"
done

echo "All TMX files processed successfully."