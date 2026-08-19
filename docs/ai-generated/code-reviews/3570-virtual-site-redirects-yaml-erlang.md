# Erlang review — #3570 Honor `_redirects.yaml` at Virtual Site build

**Branch:** `fix/issue-3570-virtual-site-redirects-yaml`  
**Date:** 2026-08-18  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new validation + emit; open-redirect rejection paths; portable `Path`/`Files` + existing `resolveHref` barrier; product-docs companion; no rest/sitemanage signature change

## Summary

Phase 1 Virtual Site build now loads optional `_redirects.yaml` beside `_config.yaml`. Missing file is a no-op. Unsafe targets (off-site, protocol-relative, non-http(s), traversal) fail the build. Valid entries emit static redirect HTML and `redirects.json`. Publish already copies ordinary files (not `_meta`); a publisher test proves redirect artifacts are copied.

## Cross-platform path checklist

- [x] Filesystem joins use `Path.resolve` / `resolveHref` (no `"/" +` or `"\\" +` for local files)
- [x] URL/href strings correctly use `/`
- [x] Tests use `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes
- [x] No raw OS `toString()` path equality assertions (hrefs normalized with `replace('\\', '/')`)
- [x] Line-ending sensitive checks use `contains` tokens

## Issues

None (hard-gate).

## Nits

- Static HTML refresh is always immediate; `status` is a map hint for operators/CDNs only (documented).

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
`PSVirtualSiteBuildServiceTest`: Tests run: 10, Failures: 0.  
`VirtualRedirectsLoaderTest`: Tests run: 13, Failures: 0.  
`PSVirtualSiteFilesystemPublisherTest`: Tests run: 11, Failures: 0.  
Module Surefire: Tests run: 2231, Failures: 0, Errors: 0, Skipped: 241.  
`scripts\ci-smoke-product-docs.bat` → OK (redirect HTML + `redirects.json` emitted).

## Re-review (PR #3603 CodeQL #1983)

**Recommendation:** approve  
**Gate:** May commit/push: yes

`redirects.json` is no longer `safeOut.resolve(constant)` (CodeQL residual on `Files.writeString(map)`). It now goes through modeled `resolveHref` after `requireSafeBuildRoot`. Path query-filter + suppressions.md row for #1983. Tests: `VirtualRedirectsEmitterTest` writes under outputRoot and rejects `../` outputRoot.

Cross-platform path checklist: `Path`/`resolveHref`/`@TempDir`; `startsWith` on normalized absolute paths. Clean.

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
`VirtualRedirectsEmitterTest`: Tests run: 2, Failures: 0.  
Module Surefire: Tests run: 2233, Failures: 0, Errors: 0, Skipped: 241.
