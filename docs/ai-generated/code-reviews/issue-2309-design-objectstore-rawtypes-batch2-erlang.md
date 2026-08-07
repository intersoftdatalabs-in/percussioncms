# Erlang review: issue #2309 design.objectstore rawtypes batch 2

**Date:** 2026-08-07  
**Branch:** `fix/issue-2309-design-objectstore-rawtypes-batch2`  
**Scope:** uncommitted vs `HEAD` (function/extension call cluster + param value hierarchy)

## Summary

Continues #2022 / #2295 with real generics for rawtypes/unchecked in the design.objectstore **call + param** cluster:

| Area | Change |
| --- | --- |
| `PSFunctionCall` | `Collection<PSFunctionParamValue>`, `ArrayList<IPSComponent>`, typed iterators / for-each, `List<IPSComponent>` parents |
| `PSExtensionCall` | `Collection<PSExtensionParamValue>`, `List<String>` applyTo, typed setParamValues/fromXml |
| `PS*ParamValue` / `PSAbstractParamValue` | `List<IPSComponent>` ctor/fromXml |
| `PSNamedReplacementValue` | Align fromXml/ctor with `List<IPSComponent>` |
| `IPSDependentObject` | `Collection<? extends IPSParameter> getParameters()` |
| `IPSDocumentMapping` | `List<IPSComponent>` fromXml (match `IPSComponent`) |
| Macro resolver | `Iterator<?>` for parameter walks (call-site compatibility) |

No class-level `@SuppressWarnings({"rawtypes","unchecked"})`. Behavioral surface preserved (XML round-trip, clone, equals, applyTo, param arrays).

## Recommendation

**approve**

## Gate

| Check | Result |
| --- | --- |
| Bugs | none found |
| Behavioral unit tests for new/changed logic | yes (`PSFunctionCallTest` 7, `PSExtensionCallTest` 7) |
| Portable paths / file I/O | N/A (no path I/O) |
| Scope confined | yes (objectstore call/param cluster + companion interface/resolver typing) |
| May commit/push | **yes** |

## Issues

None.

### Nits (non-blocking)

- `PSContentEditorDependencyMacroResolver` still has an internal raw `Iterator mappings` in an unrelated helper path (not part of this diagnostic batch; leave for residual #2022 children).
- Hottest remaining package files (`PSContentEditorMapper`, `PSDisplayMapper`, `PSContentEditorSystemDef`, `PSFieldSet`, …) intentionally out of this batch for overnight size control.

## Memory patterns hit

- Prefer real generics over class-level suppress-only
- Keep interface/`fromXml` parent list typing consistent with `IPSComponent` (`List<IPSComponent>`)
- `Iterator<?>` / `Collection<? extends T>` when accepting heterogeneous callers
- Intersoft 2026 header on new test files

## Verification

- `cd system` → `../mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: **1204**, Failures: **0**, Errors: **0**, Skipped: **240**
- Focused: `PSFunctionCallTest` + `PSExtensionCallTest` green
- Primary production files in batch: **0 residual raw collection decls** (real generics throughout)
