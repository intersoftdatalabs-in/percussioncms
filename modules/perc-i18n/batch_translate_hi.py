#!/usr/bin/env python3
"""
Batch translate missing English strings to Hindi and update the TMX file.
"""

import xml.etree.ElementTree as ET
import subprocess
import sys
import os
import time
import math

def parse_tmx(file_path, lang):
    """Parse TMX file and return a dictionary of tuid to segment for the given language."""
    try:
        tree = ET.parse(file_path)
    except ET.ParseError as e:
        print(f"Error parsing TMX file {file_path}: {e}", file=sys.stderr)
        return {}
    
    root = tree.getroot()
    tu_dict = {}
    body = root.find('body')
    if body is None:
        return tu_dict
    
    for tu in body.findall('tu'):
        tuid = tu.get('tuid')
        if tuid is None:
            continue
        for tuv in tu.findall('tuv'):
            xml_lang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
            if xml_lang == lang:
                seg_elem = tuv.find('seg')
                if seg_elem is not None and seg_elem.text is not None:
                    tu_dict[tuid] = seg_elem.text
                break
    return tu_dict

def translate_text(text, source_lang='en', target_lang='hi'):
    """Translate text using the trans command (local-translate skill)."""
    try:
        result = subprocess.run(
            ['trans', f'{source_lang}:{target_lang}', text, '-no-pager', '-b'],
            capture_output=True,
            text=True,
            check=True,
            timeout=10  # 10 second timeout per translation
        )
        return result.stdout.strip()
    except subprocess.TimeoutExpired:
        print(f"Timeout translating: {text[:50]}...", file=sys.stderr)
        return None
    except subprocess.CalledProcessError as e:
        print(f"Error translating text: {e}", file=sys.stderr)
        return None
    except FileNotFoundError:
        print("Error: 'trans' command not found. Please install translate-shell.", file=sys.stderr)
        return None

def main():
    # Parse command line arguments
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python3 batch_translate_hi.py <start_batch> <end_batch>")
        print("  python3 batch_translate_hi.py --total-batches <total> <start_batch>")
        print("  python3 batch_translate_hi.py --auto")
        print("")
        print("Examples:")
        print("  python3 batch_translate_hi.py 0 2      # processes batches 0,1,2")
        print("  python3 batch_translate_hi.py --total-batches 40 0  # auto-calculate end batch")
        print("  python3 batch_translate_hi.py --auto   # automatically process all remaining batches")
        sys.exit(1)
    
    # Initialize variables
    auto_mode = False
    start_batch = 0
    end_batch = 0
    
    # Handle --auto flag
    if sys.argv[1] == '--auto':
        auto_mode = True
        if len(sys.argv) > 2:
            try:
                start_batch = int(sys.argv[2])
            except ValueError:
                print("Error: Start batch must be an integer")
                sys.exit(1)
    # Handle --total-batches flag
    elif sys.argv[1] == '--total-batches':
        if len(sys.argv) < 4:
            print("Error: --total-batches requires <total> and <start_batch> arguments")
            sys.exit(1)
        try:
            total_batches = int(sys.argv[2])
            start_batch = int(sys.argv[3])
            end_batch = start_batch + total_batches - 1
            auto_mode = False
        except ValueError:
            print("Error: Batch numbers must be integers")
            sys.exit(1)
    # Handle regular batch range
    else:
        if len(sys.argv) < 3:
            print("Error: Missing end_batch argument")
            sys.exit(1)
        try:
            start_batch = int(sys.argv[1])
            end_batch = int(sys.argv[2])
            auto_mode = False
        except ValueError:
            print("Error: Batch numbers must be integers")
            sys.exit(1)
    
    # Validate batch numbers
    if start_batch < 0:
        print("Error: Start batch cannot be negative")
        sys.exit(1)
    
    if end_batch < start_batch:
        print("Error: End batch must be greater than or equal to start batch")
        sys.exit(1)
    
    # Paths to TMX files
    base_dir = os.path.dirname(os.path.abspath(__file__))
    en_us_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'en-us.tmx')
    hi_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'hi.tmx')
    
    # Check if files exist
    if not os.path.exists(en_us_path):
        print(f"Error: English TMX file not found at {en_us_path}", file=sys.stderr)
        sys.exit(1)
    if not os.path.exists(hi_path):
        print(f"Error: Hindi TMX file not found at {hi_path}", file=sys.stderr)
        sys.exit(1)
    
    # Parse the TMX files
    print("Parsing English TMX...")
    en_dict = parse_tmx(en_us_path, 'en-us')
    print(f"Found {len(en_dict)} entries in English TMX.")
    
    print("Parsing Hindi TMX...")
    hi_dict = parse_tmx(hi_path, 'hi')
    print(f"Found {len(hi_dict)} entries in Hindi TMX.")
    
    # Find missing tuids in Hindi TMX
    missing_tuids = []
    for tuid, en_seg in en_dict.items():
        if tuid not in hi_dict or not hi_dict[tuid]:
            missing_tuids.append((tuid, en_seg))
    
    print(f"Found {len(missing_tuids)} missing or empty translations in Hindi TMX.")
    
    if not missing_tuids:
        print("No missing translations to process.")
        return
    
    # Calculate total batches needed
    batch_size = 50
    total_batches_needed = math.ceil(len(missing_tuids) / batch_size)
    
    # Handle auto mode
    if auto_mode:
        start_batch = 0
        end_batch = total_batches_needed - 1
        print(f"Auto mode: Processing all {total_batches_needed} batches (0-{end_batch})")
    else:
        # Adjust end_batch if it exceeds total needed
        if end_batch >= total_batches_needed:
            print(f"Warning: End batch {end_batch} exceeds total needed batches {total_batches_needed-1}")
            print(f"Adjusting end batch to {total_batches_needed-1}")
            end_batch = total_batches_needed - 1
        
        if start_batch >= total_batches_needed:
            print(f"Error: Start batch {start_batch} exceeds total needed batches {total_batches_needed-1}")
            sys.exit(1)
        
        print(f"Processing batches {start_batch} to {end_batch} of {total_batches_needed} total batches")
    
    # Process batches
    total_processed = 0
    
    for batch_num in range(start_batch, end_batch + 1):
        start_idx = batch_num * batch_size
        end_idx = start_idx + batch_size
        
        if start_idx >= len(missing_tuids):
            print(f"Batch {batch_num} is out of range. Only {len(missing_tuids)} missing translations available.")
            break
        
        batch_missing = missing_tuids[start_idx:end_idx]
        actual_batch_size = len(batch_missing)
        print(f"\n=== Processing batch {batch_num} (entries {start_idx+1}-{start_idx+actual_batch_size}) ===")
        
        # Translate missing strings
        translations = {}
        batch_start_time = time.time()
        
        for i, (tuid, en_seg) in enumerate(batch_missing):
            if i % 10 == 0 and i > 0:
                elapsed = time.time() - batch_start_time
                print(f"  Progress: {i}/{actual_batch_size} translations in {elapsed:.1f}s")
            
            print(f"  Translating tuid {tuid}: {en_seg[:50]}..." if len(en_seg) > 50 else f"  Translating tuid {tuid}: {en_seg}")
            hi_seg = translate_text(en_seg)
            if hi_seg is None:
                print(f"    Warning: Failed to translate tuid {tuid}. Skipping.", file=sys.stderr)
                continue
            translations[tuid] = hi_seg
        
        batch_elapsed = time.time() - batch_start_time
        print(f"  Batch {batch_num} completed: {len(translations)}/{actual_batch_size} translations successful in {batch_elapsed:.1f}s")
        
        if not translations:
            print(f"  No translations were successful in batch {batch_num}.")
            continue
        
        # Load the Hindi TMX file for updating
        try:
            tree = ET.parse(hi_path)
        except ET.ParseError as e:
            print(f"Error: Cannot parse Hindi TMX file: {e}", file=sys.stderr)
            continue
        
        root = tree.getroot()
        body = root.find('body')
        if body is None:
            print("Error: No <body> element found in Hindi TMX.", file=sys.stderr)
            continue
        
        # Add new translation units for missing tuids
        added_count = 0
        for tuid, hi_seg in translations.items():
            # Create new tu element
            tu = ET.Element('tu')
            tu.set('tuid', tuid)
            
            # Create tuv element for Hindi
            tuv = ET.Element('tuv')
            tuv.set('{http://www.w3.org/XML/1998/namespace}lang', 'hi')
            
            seg = ET.Element('seg')
            seg.text = hi_seg
            
            tuv.append(seg)
            tu.append(tuv)
            
            # Append to body
            body.append(tu)
            added_count += 1
        
        # Write back to file
        try:
            tree.write(hi_path, encoding='utf-8', xml_declaration=True)
            print(f"  Updated Hindi TMX file saved to {hi_path}")
        except Exception as e:
            print(f"Error writing Hindi TMX file: {e}", file=sys.stderr)
            continue
        
        # Validate the updated TMX file
        try:
            ET.parse(hi_path)
            print(f"  TMX validation passed.")
        except ET.ParseError as e:
            print(f"Error: Updated TMX file is not valid XML: {e}", file=sys.stderr)
            continue
        
        total_processed += added_count
        
        # Show overall progress
        new_hi_count = len(hi_dict) + total_processed
        progress_pct = (new_hi_count / len(en_dict)) * 100
        print(f"  Overall progress: {new_hi_count}/{len(en_dict)} Hindi translations complete ({progress_pct:.1f}%)")
        
        # Delay between batches to be nice to the translation service
        if batch_num < end_batch:
            print(f"  Waiting 2 seconds before next batch...")
            time.sleep(2)
    
    print(f"\n=== Translation Complete ===")
    print(f"Processed {total_processed} new Hindi translations")
    print(f"Total Hindi entries now: {len(hi_dict) + total_processed}")
    print(f"Remaining missing: {len(en_dict) - (len(hi_dict) + total_processed)}")

if __name__ == '__main__':
    main()