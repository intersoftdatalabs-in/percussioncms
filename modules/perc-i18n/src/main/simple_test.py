#!/usr/bin/env python3

import subprocess

# Simple test
text = "Hello world"
target_locale = "es"

print(f"Testing translation of '{text}' to {target_locale}")

try:
    result = subprocess.run(['trans', '-b', ':' + target_locale, text], 
                          capture_output=True, text=True, timeout=5)
    print(f"Return code: {result.returncode}")
    print(f"Stdout: '{result.stdout}'")
    print(f"Stderr: '{result.stderr}'")
    
    if result.returncode == 0:
        translated = result.stdout.strip()
        print(f"Stripped: '{translated}'")
        
        # Check for prefix
        prefix = ':' + target_locale
        print(f"Checking for prefix '{prefix}'")
        if translated.startswith(prefix + '\n'):
            translated = translated[len(prefix) + 1:]
            print(f"Removed prefix+newline: '{translated}'")
        elif translated.startswith(prefix):
            translated = translated[len(prefix):]
            print(f"Removed prefix: '{translated}'")
            
        translated = translated.strip()
        print(f"Final result: '{translated}'")
except Exception as e:
    print(f"Error: {e}")