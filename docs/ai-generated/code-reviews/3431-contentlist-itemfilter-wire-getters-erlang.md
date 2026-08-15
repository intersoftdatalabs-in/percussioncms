# Erlang review — #3431 ContentList / ItemFilterRuleDefinitionParam wire getters

**Date:** 2026-08-15  
**Branch:** `fix/issue-3431-contentlist-itemfilter-wire-getters`  
**Scope:** uncommitted vs `origin/main`  
**Change class:** REST wire DTO Optional→plain nullable getters (parent #3388 slice 9)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure; missing behavioral tests; agent rule files without human review (avoided)

## Summary

Converts publishing Content List + ItemFilter rule-param REST wire getters from `Optional<T>` to plain nullable types with `@JsonInclude(NON_NULL)`, matching User / Role / ObjectSummary / Asset / Acl slices. Production `JacksonContextResolver` round-trip tests appended; no `empty`/`present` Optional-bean keys. Guid left untouched. `rest/AGENTS.md` not modified.

## Cross-platform path checklist

N/A — no filesystem path/file I/O, installers, or packaging. Tests use JSON strings only.

## Issues

None.

## Notes

- Public getter signatures changed (C2). Grep found no `extends ContentList` / anonymous subclasses. Reverse-dep `projects/sitemanage` standalone `clean install` green; adaptors only used setters on converted types.
- Product-docs N/A: no operator-facing example currently shows Optional-bean JSON for these types.
- UI/Playwright N/A: no WebUI screen change.
