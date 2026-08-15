# Erlang review — issue #3418 Asset / AssetField / BinaryFile wire getters

**Branch:** `fix/issue-3418-asset-optional-wire-getters`  
**Scope:** uncommitted vs `HEAD` (origin/main)  
**Date:** 2026-08-15  
**Recommendation:** approve  
**Gate:** May commit/push: yes  

Memory patterns hit: change-class completeness (rest DTO + production-mapper tests + sitemanage callers); public getter signature change requires reverse-dep install; do not commit unreviewed `rest/AGENTS.md`.

## Summary

Slice 6 of parent #3388 converts REST wire getters on `Asset`, `AssetField`, and `BinaryFile` from `Optional<T>` to plain nullable types under `@JsonInclude(NON_NULL)`. Callers in `AssetsResource`, `AssetAdaptor`, and `PageAdaptor` no longer treat those getters as `Optional`. `WorkflowInfo` / `ImageInfo` class bodies are left unconverted; `ImageInfo` inherits the new `BinaryFile` scalar getters (expected).

## Change-class closure

| Companion | Status |
| --- | --- |
| Plain getters + `@JsonInclude(NON_NULL)` | Done |
| Production `JacksonContextResolver` round-trip tests (append-only) | Done |
| rest / sitemanage caller updates | Done |
| Reverse-dep standalone `sitemanage` clean install | Done (`BUILD SUCCESS`) |
| `rest/AGENTS.md` | Intentionally not committed (human rule review) |
| product-docs | N/A — no documented Optional-bean JSON example |
| Playwright / C5 | N/A — no WebUI screen change |

## Issues

None. No bugs, no missing behavioral tests for the new wire contract or MIME-type caller change, no non-portable path I/O.

## Cross-platform path checklist

N/A — no filesystem path construction or path assertions.

## Tests / build

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 424, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1223, Failures: 0, Skipped: 125 (pre-existing skips)
- C2 greps: `ImageInfo extends BinaryFile` only (production); no anonymous `new Asset() {` subclasses
