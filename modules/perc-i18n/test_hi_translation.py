#!/usr/bin/env python3
"""
Test script to translate a few items to Hindi
"""

import xml.etree.ElementTree as ET
import subprocess
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
        print(f"Error translating text: {e}")
        return None
    except FileNotFoundError:
        print("Error: 'trans' command not found.")
        return None

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    en_us_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'en-us.tmx')
    hi_path = os.path.join(base_dir, 'src', 'main', 'resources', 'i18n', 'hi.tmx')
    
    print("Parsing English TMX...")
    en_dict = parse_tmx(en_us_path, 'en-us')
    print(f"Found {len(en_dict)} entries in English TMX.")
    
    print("Parsing Hindi TMX...")
    hi_dict = parse_tmx(hi_path, 'hi')
    print(f"Found {len(hi_dict)} entries in Hindi TMX.")
    
    # Find first 10 missing tuids
    missing_tuids = []
    for tuid, en_seg in en_dict.items():
        if tuid not in hi_dict or not hi_dict[tuid]:
            missing_tuids.append((tuid, en_seg))
        if len(missing_tuids) >= 10:
            break
    
    print(f"Found {len(missing_tuids)} missing translations to test.")
    
    # Translate missing strings
    translations = {}
    for tuid, en_seg in missing_tuids:
        print(f"Translating tuid {tuid}: {en_seg}")
        hi_seg = translate_text(en_seg)
        if hi_seg is None:
            print(f"  Failed to translate")
            continue
        translations[tuid] = hi_seg
        print(f"  Translated to: {hi_seg}")
    
    print(f"\nSuccessfully translated {len(translations)} strings.")
    
    # Show what we would add
    if translations:
        print("\nWould add these translations to Hindi TMX:")
        for tuid, hi_seg in translations.items():
            print(f"  {tuid}: {hi_seg}")

if __name__ == '__main__':
    main()