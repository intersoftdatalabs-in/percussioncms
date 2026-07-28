# Erlang review — fix/home-library-search-nav

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 |
| **Branch** | `fix/home-library-search-nav` |
| **Scope** | Home Library parent/breadcrumb navigation + Search result count UX |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

Library only offered “back to site list” from any depth. Adds `parentCmsPath` / `cmsPathSegments`, Up + breadcrumb navigation, and a dedicated empty-folder message. Search shows result count and echoes the query on empty hits. Unit tests for path helpers, Library up-navigation, Search count.

## Issues

None (bugs). Cross-platform path checklist: N/A (CMS logical paths, not OS filesystem).

## Verification

- Vitest: filenameUtils, LibrarySection, SearchSection, HomeShell — pass
- `cd WebUI && ../mvn-env.sh clean install` — required before PR
