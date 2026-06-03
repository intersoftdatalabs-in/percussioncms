#!/usr/bin/env python3
"""
Script to merge TMX files by locale.
Each locale gets a single TMX file containing all translations for that locale.
"""

import os
import sys
from xml.etree import ElementTree as ET
from xml.dom import minidom

def parse_tmx_file(filepath):
    """Parse a TMX file and return translations grouped by locale."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    root = ET.fromstring(content)
    
    header = root.find('header')
    languages = []
    if header is not None:
        for prop in header.findall('prop'):
            if prop.get('type') == 'supportedlanguage':
                languages.append(prop.text)
    
    body = root.find('body')
    translations = {}
    
    if body is not None:
        for tu in body.findall('tu'):
            tuid = tu.get('tuid')
            if not tuid:
                continue
            
            for tuv in tu.findall('tuv'):
                lang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
                if not lang:
                    continue
                
                seg = tuv.find('seg')
                if seg is not None and seg.text:
                    if lang not in translations:
                        translations[lang] = {}
                    translations[lang][tuid] = seg.text
    
    return languages, translations

def merge_tmx_files(input_files, output_dir):
    """Merge multiple TMX files into one per locale."""
    
    # Collect all translations by locale
    all_translations = {}  # {locale: {key: value}}
    
    for input_file in input_files:
        if not os.path.exists(input_file):
            print(f"Warning: File not found: {input_file}")
            continue
        
        print(f"Processing: {input_file}")
        _, translations = parse_tmx_file(input_file)
        
        for locale, trans in translations.items():
            if locale not in all_translations:
                all_translations[locale] = {}
            all_translations[locale].update(trans)
    
    # Create one TMX file per locale
    locales = sorted(all_translations.keys())
    print(f"\nFound locales: {locales}")
    
    for locale in locales:
        create_locale_tmx(locale, all_translations[locale], output_dir)

def create_locale_tmx(locale, translations, output_dir):
    """Create a TMX file for a specific locale."""
    
    if not translations:
        print(f"  No translations for {locale}")
        return
    
    # Create root element
    root = ET.Element('tmx', version="1.4")
    
    # Add header
    header = ET.SubElement(root, 'header')
    prop = ET.SubElement(header, 'prop', type="supportedlanguage")
    prop.text = locale
    
    # Add body
    body = ET.SubElement(root, 'body')
    
    # Add translation units
    for key, value in sorted(translations.items()):
        tu = ET.SubElement(body, 'tu', tuid=key)
        tuv = ET.SubElement(tu, 'tuv', **{'{http://www.w3.org/XML/1998/namespace}lang': locale})
        seg = ET.SubElement(tuv, 'seg')
        seg.text = value
    
    # Pretty print
    xml_str = ET.tostring(root, encoding='unicode')
    pretty_xml = minidom.parseString(xml_str).toprettyxml(indent="    ")
    
    # Remove extra whitespace lines
    lines = [line for line in pretty_xml.split('\n') if line.strip()]
    pretty_xml = '\n'.join(lines)
    
    # Write file
    output_file = os.path.join(output_dir, f"{locale}.tmx")
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
    
    print(f"  Created {output_file} with {len(translations)} entries")

def main():
    if len(sys.argv) < 2:
        print("Usage: merge_locale_tmx.py <output_directory> [input_files...]")
        print("Example: merge_locale_tmx.py . CmsUi.tmx SystemResources.tmx")
        sys.exit(1)
    
    output_dir = sys.argv[1]
    input_files = sys.argv[2:] if len(sys.argv) > 2 else []
    
    if not input_files:
        print("Error: No input files specified")
        sys.exit(1)
    
    os.makedirs(output_dir, exist_ok=True)
    
    merge_tmx_files(input_files, output_dir)

if __name__ == '__main__':
    main()
