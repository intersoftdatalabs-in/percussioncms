# Erlang review — #3925 rss-atom rebuild reads current local feed

**Branch:** `fix/issue-3925-rss-atom-rebuild-current`  
**Date:** 2026-08-27  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

`PSRssAtomVirtualSiteSource` already `Files.readString`s the local RSS 2.0 / Atom fixture (or a fresh loopback GET) on every discover/load (no path/mtime parse cache). `VirtualSiteConfigLoader` always opens `_config.yaml` from disk (including `rss.file` / `rss.url`). `PSVirtualSiteBuildService.build` reloads config then re-discovers. This slice locks that no-stale-parse-cache contract (Javadoc), proves same-JVM feed / `_config.yaml` Path/Files edit→rebuild emits new HTML (same-instance source, factory wiring with `rss.file` switch `pageCount` 2→1, loopback `rss.url` body), and documents the operator rebuild path (REST/CLI; no JVM restart; no file watchers).

Peer: object-storage rebuild #3880 / http-json #3837 / sql-database #3780 / csv-filesystem #3708 / git-filesystem #3367. Parent epic: #2678.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes in new tests
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality
- [x] Loopback `rss.url` is `http://127.0.0.1:<port>/…` (URI, not an OS file join)

## Issues

None (hard-gate).

## Nits

- Two nearly parallel `secondBuildAfterRss…` tests (`PSVirtualSiteBuildServiceTest` factory wiring vs `PSRssAtomVirtualSiteSourceTest` same-instance source). Acceptable as the SPI + assemble proofs matching object-storage #3880 / HTTP JSON #3837.
- Factory test also proves `_config.yaml` `rss.file` is honored on the second build (`pageCount` 2→1). Extra vs the source-class feed-edit test; useful, not sprawl.
- Loopback rebuild uses in-process `HttpServer` + `AtomicReference` (peer of http-json). No live internet feeds.

## C2 API shape

Javadoc-only on existing public types; no `final`/`sealed` and no method/ctor signature change. Reverse-deps (`rest` / `sitemanage`) not required.

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
Focused: `PSRssAtomVirtualSiteSourceTest` Tests run: 25, Failures: 0, Skipped: 1. `PSVirtualSiteBuildServiceTest` Tests run: 15, Failures: 0. `VirtualSiteConfigLoaderTest` Tests run: 19, Failures: 0.  
Module Surefire: Tests run: 2571, Failures: 0, Errors: 0, Skipped: 246.  
Product-docs: `scripts\ci-smoke-product-docs.bat` → **OK** (24 pages).
