#!/usr/bin/env python3
"""
Script to update the Hindi TMX file with a limited number of missing English translations.
"""

import xml.etree.ElementTree as ET
import subprocess
import sys
import os

def parse_tmx(file_path, lang):
    """Parse TMX file and return a dictionary of tuid to segment for the given language."""
    tree = ET.parse(file_path)
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
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error translating text: {e}", file=sys.stderr)
        return None
    except FileNotFoundError:
        print("Error: 'trans' command not found. Please install translate-shell.", file=sys.stderr)
        return None

def main():
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
    
    # Process only first 50 entries to avoid timeouts
    batch_size = 50
    missing_tuids = missing_tuids[:batch_size]
    print(f"Processing first {len(missing_tuids)} missing translations...")
    
    # Translate missing strings
    translations = {}
    for tuid, en_seg in missing_tuids:
        print(f"Translating tuid {tuid}: {en_seg[:50]}..." if len(en_seg) > 50 else f"Translating tuid {tuid}: {en_seg}")
        hi_seg = translate_text(en_seg)
        if hi_seg is None:
            print(f"Warning: Failed to translate tuid {tuid}. Skipping.", file=sys.stderr)
            continue
        translations[tuid] = hi_seg
    
    print(f"Successfully translated {len(translations)} strings.")
    
    # Load the Hindi TMX file for updating
    tree = ET.parse(hi_path)
    root = tree.getroot()
    body = root.find('body')
    if body is None:
        print("Error: No <body> element found in Hindi TMX.", file=sys.stderr)
        sys.exit(1)
    
    # Add new translation units for missing tuids
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
        print(f"Added translation for tuid {tuid}")
    
    # Write back to file
    tree.write(hi_path, encoding='utf-8', xml_declaration=True)
    print(f"Updated Hindi TMX file saved to {hi_path}")
    
    # Validate the updated TMX file
    print("Validating updated TMX...")
    try:
        ET.parse(hi_path)
        print("TMX validation passed.")
    except ET.ParseError as e:
        print(f"Error: Updated TMX file is not valid XML: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()