# Erlang review: #3290 HTTPClient CookieModule + PSServerFolderProcessor Xlint

**Branch:** `fix/issue-3290-httpclient-folder-xlint`  
**Base:** `origin/main`  
**Date:** 2026-08-13  
**Reviewer:** Erlang (pre-commit)

## Summary

Typed cookie-jar deserialization in `CookieModule` (runtime-checked `ConcurrentHashMap` of `Cookie`) and extracted `PSFolderLocatorPaths` so folder locator paths are `List<List<PSLocator>>` without an unchecked dual-return helper. Public `getFolderLocatorPaths(PSLocator)` signature unchanged. HTTP/cookie send/accept behavior unchanged.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover new type-check and path-walk logic. Paths in new tests use `java.nio.file.Path` / `Files`. No product-facing surface; no WebUI.

**Memory patterns hit:** missing behavioral tests; non-portable path joins (checked — not present); change-class closure for Xlint (tests + module suite, no Spring adaptor / Playwright companions required).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Cookie jar tests use `Path` / `Files`
- [x] No Unix-only absolute path assertions
- [x] Temp files via JUnit `@TempDir`

## Issues

None (blocking).

### Suggestions (non-blocking)

- `PSFolderLocatorPaths.collect` NPEs if `parentLookup` returns `null`; production lookup always returns a list. Could treat null as empty if a future caller is sloppy.
- Folder ancestor walk still recurses without cycle detection (same as the previous private helper).

## Re-review (2026-08-13, PR #3293 kilo thread)

Kilo flagged `collect` NPE if `FolderParentLookup.getImmediateParents` returns null. Follow-up:

- `parentsOrEmpty` coerces null to `List.of()` at both `collect` and `appendAncestors` call sites.
- Interface `@return` documents never-null; collect still treats null as empty (defense in depth).
- Tests: `collect_nullImmediateParents_empty`, `collect_nullAncestorParents_stopsWalk`.

**Recommendation:** approve. **May commit/push: yes.** No blocking bugs. Cycle-detection still a non-blocking suggestion.

## Tests / build

- `CookieModuleCookieJarTest` — 4 tests
- `PSServerFolderProcessorLocatorPathsTest` — 5 tests
- `cd system && ../mvnw.cmd clean install` — **BUILD SUCCESS**, Tests run: 2112, Failures: 0, Errors: 0, Skipped: 241
