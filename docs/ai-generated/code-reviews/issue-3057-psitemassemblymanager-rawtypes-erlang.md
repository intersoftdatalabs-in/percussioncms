# Erlang review: issue #3057 PSItemAssemblyManager rawtypes

**Scope:** `modules/DesktopContentExplorer` — `PSItemAssemblyManager.java` + `PSItemAssemblyManagerTest.java`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** rawtypes residual slice (prefer real generics; pure helpers unit-tested; no product behavior change)

## Summary

Parameterizes residual raw `Iterator` / `Map` / `HashMap` on `PSItemAssemblyManager` (insert/delete/update/reorder node lists + postData payload). Extracts pure static helpers (`isSlotTarget`, `resolveDropIndex`, `resolveReorderDropIndex`, `buildActiveAssemblerParams`) with behavioral unit tests. No Swing/server wiring in tests.

## Issues

None (bugs / missing behavioral tests / non-portable paths).

### Cross-platform path checklist

N/A — no file I/O or path handling in this diff.

### API shape / reverse deps (C2)

Public/private method parameters changed raw `Iterator` → `Iterator<PSNode>` (erasure-compatible). Call sites only in `PSActionManager` within the same module (already typed `Iterator<PSNode>`). No monorepo reverse-dep blast radius beyond `DesktopContentExplorer`.

## Build evidence

```
cd modules/DesktopContentExplorer
..\..\mvnw.cmd clean install
# BUILD SUCCESS — Tests run: 159, Failures: 0, Errors: 0
# PSItemAssemblyManagerTest: 11 tests
```

## Product documentation

N/A — pure tech-debt/rawtypes; no operator- or user-visible behavior change.
