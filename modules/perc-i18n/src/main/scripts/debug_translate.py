#!/usr/bin/env python3

import subprocess

text = "An unknown error occurred while processing the request submitted by session id {0}."
target_locale = "es"

print(f"Translating: '{text}' to {target_locale}")

try:
    # Use trans command to translate
    result = subprocess.run(['trans', '-b', ':' + target_locale, text], 
                          capture_output=True, text=True, timeout=10)
    print(f"Return code: {result.returncode}")
    print(f"Stdout: '{result.stdout}'")
    print(f"Stderr: '{result.stderr}'")
    
    if result.returncode == 0:
        # Remove the locale prefix that trans adds (like ":en_GB\n")
        translated = result.stdout.strip()
        print(f"Stripped stdout: '{translated}'")
        # Remove prefix like ":en_GB\n" from the beginning
        prefix = ':' + target_locale
        if translated.startswith(prefix + '\n'):
            translated = translated[len(prefix) + 1:]  # Remove ":xx\n"
            print(f"After removing prefix+newline: '{translated}'")
        elif translated.startswith(prefix):
            translated = translated[len(prefix):]  # Remove ":xx"
            print(f"After removing prefix: '{translated}'")
        # Also remove trailing newlines and spaces
        translated = translated.strip()
        print(f"After stripping: '{translated}'")
        if translated:
            print(f"Final translation: '{translated}'")
        else:
            print("Translation resulted in empty string")
    else:
        print(f"Translation failed with exit code {result.returncode}")
except Exception as e:
    print(f"Error: {e}")