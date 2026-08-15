# Erlang review — feat/explorer-new-item

**Date**: 2026-08-15  
**Scope**: uncommitted vs `feat/explorer-content-editor` (#3452)  
**Reviewer**: Erlang  
**Memory patterns hit**: change-class completeness (sitemanage create REST + WebUI dispatch + Playwright + product-docs); behavioral tests for path/name sanitize; CMS paths use `/` (URL/repo, not OS I/O)

## Summary

Explorer New Item type children create an item in the current folder (`POST /itemmanagement/item/create`) and open the React editor. Parent New without a type asks to choose a type. Leftover CE HTML is not navigated.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Issues

None blocking.

### Suggestion

`percPage` create via `contentItemDao` may fail without a template. Documented; Home Create remains the templated-page path.

## Tests run

- sitemanage `clean install` — BUILD SUCCESS; Tests run: 1261, Failures: 0
- WebUI `clean install` — BUILD SUCCESS; Vitest 2528 passed
