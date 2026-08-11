# Erlang review — issue #2895 sitemanage Xlint unchecked/rawtypes residual

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** Main-source compiler lint tech debt — real generics / typed helpers (no REST contract change)  
**Module:** `projects/sitemanage`  
**Date:** 2026-08-11

## Scope

Clear sitemanage main-source **unchecked / rawtypes** cluster after batch 3 (#2870 / PR #2896 serial-field collections). Prefer real generics and runtime-checked conversions; preserve Spring/test wiring types.

## Inventory (main-source, uncapped `-Xmaxwarns`)

| Metric | Before | After |
|--------|--------|-------|
| Total WARNING lines | 227 | 174 |
| unchecked + rawtypes | 53 | **0** |
| serial-field | 86 | 86 (owned by batch 3) |
| this-escape | 55 | 55 |

## Findings

### Bugs
- **None.** Bind/eval helpers use `Class.cast` with null-safe paths; JSON helpers copy via `instanceof` rather than bare generic casts.

### Tests
- **Pass:** `PSWidgetUtilsTest` re-enabled with coerce behavior; new `PSAbstractTransformerTest`, `PSConcurrentRegionsAssemblerFutureListTest`; existing `PSResourceDefinitionUtilsTest` / `PSAbstractFilterTest` still cover dependency sort + filter.
- Module suite: **BUILD SUCCESS**, Tests run: **976**, Failures: **0**, Errors: **0**, Skipped: **126**.

### Cross-platform paths
- **N/A** for production path I/O. Siteimprove helpers use string conversion only.

### API / contracts
- No rest adaptor signature changes.
- Public getters/setters and Spring bean types unchanged.
- C2 downstream: **none** (no final/sealed/public signature change).

### Residual
- **this-escape** (~55), non-collection serial-field residual after batch 3 (~14), misc other (~33). File residual child for next PR-sized slice.

## Verdict

**PASS** — ready for commit/PR.
