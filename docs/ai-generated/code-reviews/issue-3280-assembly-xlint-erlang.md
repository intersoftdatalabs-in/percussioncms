# Erlang review — #3280 assembly PSNavHelper/assemblers/jexl Xlint

**Scope:** uncommitted `fix/issue-3280-assembly-xlint` vs `origin/main`  
**Module:** `system` / `perc-system`  
**Change class:** residual `-Xlint` rawtypes/unchecked cleanup in `com.percussion.services.assembly`  
**Memory patterns hit:** behavioral tests for changed helpers; prefer real generics over `@SuppressWarnings`; no public API blast radius; no path I/O

## Summary

Removes leftover unchecked map casts in assemblers, `PSAssemblyWorkItem.getMetaData`, nav (`PSNavHelper` / `PSNavonNodeInvocationHandler`), and jexl (`PSAssemblerUtils.combine` / `getTimers`). Introduces `PSAssemblyBindingMaps` (copy + live write-through view) and `PSBinaryAssemblerSupport.resolveSys` so named production files no longer use `@SuppressWarnings("unchecked")`. The single residual unchecked conversion is isolated in `LiveStringObjectMap.erase`. Tests cover copy/live maps, binary `$sys` resolution, nav params/base/property-map copy, jexl combine/timers, and metadata write-through.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A — no filesystem path construction, installers, or path assertions.

## Product documentation

N/A — internal compiler-warning cleanup; no operator/user/API surface change.

## Issues

None.

## Notes

- `getMetaData()` still returns `Map<String, Object>`; the live view mutates the original `$sys.metadata` map (tested).
- `PSContentFinderBase.find` now writes `$sys.hasMore` through the same live view (same `$sys` instance).
- C2: no type made `final`/`sealed`; no public/protected method or ctor signature change. Downstream install not required.
- Focused tests green (22). Module `clean install` is the remaining pre-PR gate.
