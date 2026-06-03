#!/usr/bin/env python3

import subprocess

def test_trans():
    # Test the trans command directly
    text = "Hello world"
    target_locale = "es"
    
    print(f"Testing trans with: '{text}' -> {target_locale}")
    
    try:
        result = subprocess.run(
            ['trans', '-b', ':' + target_locale, text],
            capture_output=True, 
            text=True, 
            timeout=5
        )
        print(f"Return code: {result.returncode}")
        print(f"Stdout: '{result.stdout}'")
        print(f"Stderr: '{result.stderr}'")
        
        if result.returncode == 0:
            print(f"Success: '{result.stdout.strip()}'")
        else:
            print(f"Failed with code {result.returncode}")
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_trans()