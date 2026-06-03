#!/usr/bin/env python3

import sys
import os
import re
import subprocess

def translate_text(text, target_locale):
    """Translate text using trans command"""
    if not text or re.match(r'^[\s]*\{[0-9]+(\,[0-9]+)*\}[\s]*$', text):
        return text
    
    try:
        # Use trans command to translate with proper encoding handling
        # Use Popen with pipes to better control encoding
        proc = subprocess.Popen(
            ['trans', '-b', ':' + target_locale, text],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        stdout, stderr = proc.communicate(timeout=10)
        
        if proc.returncode == 0:
            # Decode with utf-8
            translated = stdout.decode('utf-8').strip()
            # Remove prefix like ":en_GB\n" from the beginning
            prefix = ':' + target_locale
            if translated.startswith(prefix + '\n'):
                translated = translated[len(prefix) + 1:]  # Remove ":xx\n"
            elif translated.startswith(prefix):
                translated = translated[len(prefix):]  # Remove ":xx"
            # Also remove trailing newlines and spaces
            translated = translated.strip()
            if translated:
                return translated
            else:
                # If translation resulted in empty string, return original
                return text
        else:
            # Try to decode stderr
            error_msg = stderr.decode('utf-8', errors='replace').strip()
            print(f"Warning: Translation failed for '{text}' (exit code {proc.returncode}): {error_msg}, keeping original")
            return text
    except Exception as e:
        print(f"Warning: Translation error for '{text}': {e}")
        return text

def process_tmx_file(source_file, target_locale):
    """Process TMX file to translate content and update locale codes"""
    
    # Read the source file
    with open(source_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Update header language code
    content = re.sub(
        r'<prop type="supportedlanguage">[^<]*</prop>',
        f'<prop type="supportedlanguage">{target_locale}</prop>',
        content
    )
    
    # Update tuv xml:lang attributes
    content = re.sub(
        r'<tuv xml:lang="[^"]*"',
        f'<tuv xml:lang="{target_locale}"',
        content
    )
    
    # Find and translate content between <seg> tags
    def replace_seg_content(match):
        seg_content = match.group(1)
        # Skip if content is only placeholders like {0}
        if re.match(r'^[\s]*\{[0-9]+(\,[0-9]+)*\}[\s]*$', seg_content):
            return match.group(0)  # Return unchanged
        
        # Translate the content
        translated = translate_text(seg_content, target_locale)
        return f'<seg>{translated}</seg>'
    
    content = re.sub(r'<seg>([^<]*)</seg>', replace_seg_content, content)
    
    # Generate target file path
    source_dir = os.path.dirname(source_file)
    source_base = os.path.basename(source_file).replace('.tmx', '')
    target_file = os.path.join(source_dir, f"{source_base}_{target_locale}.tmx")
    
    # Write the target file
    with open(target_file, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Translation completed: {target_file}")
    return target_file

if __name__ == "__main":
    if len(sys.argv) != 3:
        print("Usage: python final_translate.py <source_tmx_file> <target_locale>")
        print("Example: python final_translate.py en_US.tmx en_GB")
        sys.exit(1)
    
    source_file = sys.argv[1]
    target_locale = sys.argv[2]
    
    if not os.path.exists(source_file):
        print(f"Error: Source file '{source_file}' not found.")
        sys.exit(1)
    
    process_tmx_file(source_file, target_locale)