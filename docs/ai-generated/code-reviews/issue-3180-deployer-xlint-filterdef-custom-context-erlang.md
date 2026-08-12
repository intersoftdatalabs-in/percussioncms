# Erlang review: issue #3180 FilterDef/Custom/Context Xlint

**Branch:** `fix/issue-3180-filterdef-custom-context-xlint`  
**Scope:** uncommitted deployer FilterDef / Custom / Context* handler generics  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no path/file I/O changes)

## Summary

PR-sized residual of parent #2028: real generics on FilterDef*, Custom*, Context* dependency handlers. Mirrors SystemDef (#3178) and SharedGroup+WorkflowDef (#3179) patterns.

## Changes reviewed

| File | Notes |
|------|-------|
| `PSFilterDefDependencyHandler` | Typed iterators, id-type mappings list, transformIds Map/entry, archive files |
| `PSFilterDependencyHandler` | `List.of` child types |
| `PSCustomDependencyHandler` | Typed deps/children iterators + `@Override` |
| `PSContextDefDependencyHandler` | Typed child types / dependency files / archive files |
| `PSContextDependencyHandler` | `List.of` child types |
| `PSFilterDefCustomContextHandlersTypedTest` | Signature smoke (peer pattern) |

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Notes

- `@SuppressWarnings("unchecked")` on `transformIds` map cast matches ContentListDef peer (runtime object is caller-supplied Map).
- `List.of` for `ms_childTypes` matches sibling batches; immutable is intentional.
- Pure tech-debt; product-docs N/A; C5 UI proof N/A.

## Build evidence (pre-commit)

```
cd deployer && ../mvnw clean install
Tests run: 273, Failures: 0, Errors: 0, Skipped: 19
PSFilterDefCustomContextHandlersTypedTest: Tests run: 5, Failures: 0
BUILD SUCCESS
```
