# Erlang review — #3367 Virtual Site rebuild reads current filesystem

**Branch:** `fix/issue-3367-virtual-site-rebuild-current-fs`  
**Date:** 2026-08-14  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

Git-filesystem Virtual Site discover/load already `Files.readString`s on every call; SitesAdaptor constructs a new `PSVirtualSiteBuildService` per build. This change makes the no-stale-parse-cache contract explicit (SPI + source + build Javadoc), proves same-JVM edit→rebuild emits new HTML, and documents that operators rebuild after `git pull`/local edit without a CMS restart.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality

## Issues

None (hard-gate).

## Nits

- SPI Javadoc is slightly longer than surrounding Phase-1 comments; acceptable as a contract lock.

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
`PSVirtualSiteBuildServiceTest`: Tests run: 8, Failures: 0.  
Module Surefire: Tests run: 2099, Failures: 0, Errors: 0, Skipped: 238.
