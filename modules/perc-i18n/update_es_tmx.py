#!/usr/bin/env python3
"""
Script to update the Spanish TMX file with missing English translations.
Uses the local-translate skill (trans command) to translate missing strings.
"""

import xml.etree.ElementTree as ET
import subprocess
import sys
import os

def parse_tmx(file_path, lang):
    """Parse TMX file and return a dictionary of tuid to segment for the given language."""
    tree = ET.parse(file_path)
    root = tree.getroot()
    # Define namespace for XML namespace in tuv elements
    ns = {'xml': 'http://www.w3.org/XML/1998/namespace'}
    tu_dict = {}
    body = root.find('body')
    if body is None:
        return tu_dict
    for tu in body.findall('tu'):
        tuid = tu.get('tuid')
        if tuid is None:
            continue
        # Find the tuv with the specified language
        for tuv in tu.findall('tuv'):
            xml_lang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
            if xml_lang == lang:
                seg_elem = tuv.find('seg')
                if seg_elem is not None and seg_elem.text is not None:
                    tu_dict[tuid] = seg_elem.text
                break
    return tu_dict

def translate_text(text, source_lang='en', target_lang='es'):
    """Translate text using the trans command (local-translate skill)."""
    try:
        # Use trans command with batch mode and no pager
        result = subprocess.run(
            ['trans', f'{source_lang}:{target_lang}', text, '-no-pager', '-b'],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error translating text: {e}", file=sys.stderr)
        print(f"stderr: {e.stderr}", file=sys.stderr)
        return None
    except FileNotFoundError:
        print("Error: 'trans' command not found. Please install translate-shell.", file=sys.stderr)
        return None

def main():
    # Paths to TMX files
    base_dir = os.path.dirname(os.path.abspath(__file__))
    en_us_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'en-us.tmx')
    es_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'es.tmx')
    
    # Check if files exist
    if not os.path.exists(en_us_path):
        print(f"Error: English TMX file not found at {en_us_path}", file=sys.stderr)
        sys.exit(1)
    if not os.path.exists(es_path):
        print(f"Error: Spanish TMX file not found at {es_path}", file=sys.stderr)
        sys.exit(1)
    
    # Parse the TMX files
    print("Parsing English TMX...")
    en_dict = parse_tmx(en_us_path, 'en-us')
    print(f"Found {len(en_dict)} entries in English TMX.")
    
    print("Parsing Spanish TMX...")
    es_dict = parse_tmx(es_path, 'es')
    print(f"Found {len(es_dict)} entries in Spanish TMX.")
    
    # Find missing tuids in Spanish TMX
    missing_tuids = []
    for tuid, en_seg in en_dict.items():
        if tuid not in es_dict or not es_dict[tuid]:
            missing_tuids.append((tuid, en_seg))
    
    print(f"Found {len(missing_tuids)} missing or empty translations in Spanish TMX.")
    
    if not missing_tuids:
        print("No missing translations to process.")
        return
    
    # Translate missing strings
    print("Translating missing strings...")
    translations = {}
    for tuid, en_seg in missing_tuids:
        print(f"Translating tuid {tuid}: {en_seg[:50]}..." if len(en_seg) > 50 else f"Translating tuid {tuid}: {en_seg}")
        es_seg = translate_text(en_seg)
        if es_seg is None:
            print(f"Warning: Failed to translate tuid {tuid}. Skipping.", file=sys.stderr)
            continue
        translations[tuid] = es_seg
    
    print(f"Successfully translated {len(translations)} strings.")
    
    # Load the Spanish TMX file for updating
    tree = ET.parse(es_path)
    root = tree.getroot()
    body = root.find('body')
    if body is None:
        print("Error: No <body> element found in Spanish TMX.", file=sys.stderr)
        sys.exit(1)
    
    # Add new translation units for missing tuids
    for tuid, es_seg in translations.items():
        # Create new tu element
        tu = ET.Element('tu')
        tu.set('tuid', tuid)
        
        # Create tuv element for Spanish
        tuv = ET.Element('tuv')
        tuv.set('{http://www.w3.org/XML/1998/namespace}lang', 'es')
        
        seg = ET.Element('seg')
        seg.text = es_seg
        
        tuv.append(seg)
        tu.append(tuv)
        
        # Append to body
        body.append(tu)
        print(f"Added translation for tuid {tuid}")
    
    # Write back to file
    tree.write(es_path, encoding='utf-8', xml_declaration=True)
    print(f"Updated Spanish TMX file saved to {es_path}")
    
    # Validate the updated TMX file
    print("Validating updated TMX...")
    try:
        ET.parse(es_path)
        print("TMX validation passed.")
    except ET.ParseError as e:
        print(f"Error: Updated TMX file is not valid XML: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()