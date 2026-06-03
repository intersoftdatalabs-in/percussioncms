#!/bin/bash

# Script to translate all TMX files in the i18n directory
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
        if [[ $line =~ <tuv xml:lang=\"[^\"]*\"> ]]; then
            line=$(echo "$line" | sed "s|<tuv xml:lang=\"[^\"]*\"|<tuv xml:lang=\"${locale}|")
        fi
        
        # Check if this line contains a <seg> tag that needs translation
        if [[ $line =~ <seg>([^<]*)</seg> ]]; then
            # Extract the text to translate
            TEXT_TO_TRANSLATE="${BASH_REMATCH[1]}"
            
            # Skip if text is empty or contains only placeholders like {0} or {0},{1}, etc.
            if [[ -n "$TEXT_TO_TRANSLATE" && ! "$TEXT_TO_TRANSLATE" =~ ^\{[0-9]+(\,[0-9]+)*\}$ ]]; then
                # Translate the text using trans command
                # Using :$locale to specify target language
                TRANSLATED_TEXT=$(trans -b :$locale "$TEXT_TO_TRANSLATE" 2>/dev/null)
                
                # If translation failed, keep original text
                if [ $? -ne 0 ]; then
                    TRANSLATED_TEXT="$TEXT_TO_TRANSLATE"
                    echo "Warning: Translation failed for: $TEXT_TO_TRANSLATE"
                fi
                
                # Replace the segment with translated text
                line=$(echo "$line" | sed "s|<seg>[^<]*</seg>|<seg>${TRANSLATED_TEXT}</seg>|")
            fi
        fi
        
        # Update the header language code
        if [[ $line =~ <prop type=\"supportedlanguage\">[^<]*</prop> ]]; then
            line=$(echo "$line" | sed "s|<prop type=\"supportedlanguage\">[^<]*</prop>|<prop type=\"supportedlanguage\">${locale}</prop>|")
        fi
        
        # Write line to temp file
        echo "$line" >> "$temp_file"
        
    done < "$tmx_file"
    
    # Replace the original file with processed content
    mv "$temp_file" "$tmx_file"
    
    echo "Completed processing $filename.tmx"
done

echo "All TMX files processed successfully."