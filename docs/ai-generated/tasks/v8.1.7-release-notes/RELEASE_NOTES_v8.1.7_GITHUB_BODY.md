# PercussionCMS v8.1.7 Release Notes

## Overview

This release delivers significant UI improvements to the Category management tools, Google Analytics 4 migration support, Delivery Tier (DTS) stability enhancements, and a major library upgrade (PDFBox 3.x) along with numerous bug fixes. Full compatibility with Java 8 (JDK 1.8.0) is maintained.

## Notable Changes

### Category Administration UI Overhaul

- Complete refactor of the Category editor and browser UI (#778, #781, #783).
- Migration from Dynatree to Fancytree for improved reliability and maintainability.
- Numerous UX fixes: overlapping icons, tree rendering, move up/down, add child, save/cancel behavior, node activation, serialization formatting, and noisy logging.
- Related fixes for Category List widget display issues on published sites (#755).

### Google Analytics 4 (GA4) Migration

- Migration support and updates for Google Analytics 4 (#756, #760).
- Enhanced Google authentication error handling and gadget updates.

### Bulk Upload Gadget Improvements

- Better error handling for zero-size file uploads (#728, #775, #776).
- General robustness and messaging improvements for the bulk upload gadget (#710, #727).

### Delivery Tier (DTS) & Startup Stability

- Reduced startup warnings and noise on DTS (#748, #764).
- Fixed ClassNotFoundException for LoadDataChange by adding opencsv dependency.
- Java 8 TLS/cipher configuration and JUL bridge improvements (#782).
- Windows-specific DTS fixes (#672, #736).

### Widget & Gadget Fixes

- Fixed Auto List widgets not rendering correctly on published sites (#749, #771, #762).
- Fixed null pointer in Google Setup gadget (#768, #773).
- Form widget accessibility improvements (autocomplete attributes) and version bump (#658).
- Added missing navigation role to Breadcrumb widget (#653).
- Fixed JSON payload leaking in error message extraction for empty fieldErrors arrays (#774).

### Other Improvements

- Footer alignment fixes across vspan regions (#757, #763, #767).
- Better error reporting when email is not configured for the system (#735).
- Process monitor fixes (#738).
- Added deprecation UI indicators and lists for widgets/gadgets scheduled for removal in 8.2 (#720, #722).
- Packaging performance improvements and build tooling updates.
- Various Jackson compatibility and null handling fixes (#675, #676).

## Dependency Updates

### Major Library Upgrades

- **Apache PDFBox:** Updated to 3.0.6 (from 2.0.30). Includes required compatibility code changes.
- **Jackson:** Updated to 2.21.1 (from 2.20.1).
- **Apache Shiro:** Updated to 2.1.0 (security/maintenance).
- **JSoup:** Updated to 1.22.1.
- **SnakeYAML:** Updated to 2.6.
- **Netty:** Updated to 4.2.10.Final.
- **OpenCSV:** Added 5.12.0 (new dependency for DTS data loading fixes).

### Maintenance & Minor Updates

- Many Maven plugins and test libraries updated (JUnit Jupiter 5.14.3, etc.).
- AWS SDK minor bump (1.12.797).
- Continued Java 8 compatibility enforcement (no Jakarta namespace or Java 11+ only dependencies introduced).

### Java 8 Compatibility

All updates were validated against JDK 1.8.0. Libraries requiring Java 11+ (e.g., certain major version bumps attempted in prior cycles) remain at their last compatible versions (MyFaces 2.3.11, Shindig 1.1-BETA5-incubating, etc.).

## Requirements

- **Java Version:** JDK 1.8.0 (Java 8) — Amazon Corretto or equivalent recommended.
- All server and DTS components remain compatible with Java 8.

## Build / Release Information

- **Branch:** development-8.1.x
- **Base Tag:** v8.1.6
- **Version:** 8.1.7-SNAPSHOT (at time of drafting)

---

For detailed PR references, dependency analysis, and correction notes, see the internal detailed release notes document.
