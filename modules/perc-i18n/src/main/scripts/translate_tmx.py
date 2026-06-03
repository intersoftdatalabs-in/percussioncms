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
        # Use trans command to translate with text passed via stdin to avoid shell escaping issues
        proc = subprocess.Popen(
            ['trans', '-b', ':' + target_locale],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        stdout, stderr = proc.communicate(input=text.encode('utf-8'), timeout=10)
        
        if proc.returncode == 0:
            # Get the translated text
            translated = stdout.decode('utf-8')
            
            # The trans command sometimes outputs a prefix like ":es" followed by the actual translation
            # We need to remove this prefix if it exists
            lines = translated.split('\n')
            if lines and lines[0].startswith(':' + target_locale):
                # Remove the first line if it's just the locale prefix
                if len(lines[0]) == len(':' + target_locale):
                    lines = lines[1:]
                else:
                    # The prefix is part of the line, remove it
                    lines[0] = lines[0][len(':' + target_locale):]
            
            # Join the lines back
            translated = '\n'.join(lines)
            
            # Also remove any leading/trailing whitespace
            translated = translated.strip()
            
            # If we got a non-empty result, use it
            if translated:
                return translated
            else:
                # If translation resulted in empty string, return original
                return text
        else:
            # Translation failed
            error_msg = stderr.decode('utf-8', errors='replace').strip()
            print(f"Warning: Translation failed for '{text}' (exit code {proc.returncode}): {error_msg}, keeping original")
            return text
    except subprocess.TimeoutExpired:
        print(f"Warning: Translation timeout for '{text}', keeping original")
        return text
    except Exception as e:
        print(f"Warning: Translation error for '{text}': {e}")
        return text

def process_tmx_file(source_file, target_locale):
    """Process TMX file to translate content and update locale codes"""
    
    # Read the source file
    with open(source_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Process each line
    processed_lines = []
    for line in lines:
        # Update header language code
        if '<prop type="supportedlanguage">' in line:
            line = re.sub(
                r'<prop type="supportedlanguage">[^<]*</prop>',
                f'<prop type="supportedlanguage">{target_locale}</prop>',
                line
            )
        
        # Update tuv xml:lang attributes
        if '<tuv xml:lang=' in line:
            line = re.sub(
                r'<tuv xml:lang="[^"]*"',
                f'<tuv xml:lang="{target_locale}"',
                line
            )
        
        # Translate content in <seg> tags
        if '<seg>' in line and '</seg>' in line:
            # Check if it's a placeholder like {0} that shouldn't be translated
            match = re.search(r'<seg>([^<]*)</seg>', line)
            if match:
                seg_content = match.group(1)
                # Skip if content is only placeholders like {0}, {0},{1}, etc.
                if not re.match(r'^[\s]*\{[0-9]+(\,[0-9]+)*\}[\s]*$', seg_content):
                    # Translate the content
                    translated = translate_text(seg_content, target_locale)
                    # Replace the content
                    line = line.replace(f'<seg>{seg_content}</seg>', f'<seg>{translated}</seg>')
        
        processed_lines.append(line)
    
    # Generate target file path
    source_dir = os.path.dirname(source_file)
    source_base = os.path.basename(source_file).replace('.tmx', '')
    target_file = os.path.join(source_dir, f"{source_base}_{target_locale}.tmx")
    
    # Write the target file
    with open(target_file, 'w', encoding='utf-8') as f:
        f.writelines(processed_lines)
    
    print(f"Translation completed: {target_file}")
    return target_file

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python translate_tmx.py <source_tmx_file> <target_locale>")
        print("Example: python translate_tmx.py en_US.tmx en_GB")
        sys.exit(1)
    
    source_file = sys.argv[1]
    target_locale = sys.argv[2]
    
    if not os.path.exists(source_file):
        print(f"Error: Source file '{source_file}' not found.")
        sys.exit(1)
    
    process_tmx_file(source_file, target_locale)