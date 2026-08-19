# Erlang review — issue #3628 Explorer New-item type picker live

**Date:** 2026-08-19  
**Branch:** `fix/issue-3628-explorer-new-item-picker-live`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Playwright surface filter; no-skip live catalog; incomplete change-class (Playwright companion); false green from stubs

## Summary

Convert `explorer-new-item-type-picker.spec.js` from stubbed `GET /actions/find` + create POST fulfill to live H2 product-route proof. Helpers prefer create-safe asset types (avoid required-field widgets). Locate **New Item** by accessible name and test ids. Cancel does not POST. Create must be 200/201/204. Leftover Data Flow CE HTML is forbidden. Unit tests lock the no-stub / no-skip contract.

## Gate

- **Bugs:** none found. Live locator matches catalog label `New Item`. Preferred types avoid `percBlogIndexAsset` validation 500.
- **Behavioral tests:** helper unit tests (preferred type, URLs, envelope parse, spec must not stub). Playwright surface 2 passed 0 skipped on H2.
- **Cross-platform paths:** no filesystem I/O. CMS URL/path strings use `/`.

## Issues

None blocking.
