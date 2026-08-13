# Erlang review — #3291 assembly leftover Xlint after #3280

**Scope:** uncommitted `fix/issue-3291-assembly-leftover-xlint` vs `origin/main`  
**Module:** `system` / `perc-system`  
**Change class:** leftover `-Xlint` rawtypes/unchecked after #3280 / PR #3284  
**Memory patterns hit:** behavioral tests for changed helpers; prefer real generics over `@SuppressWarnings`; orphaned javadoc after extract; C2 public signature grep; no path I/O

## Summary

#3284 already typed `PSHtmlAssembler` / `PSMarkdownAssembler` / `PSBinaryAssembler`, `PSAssemblyWorkItem.getMetaData`, and `PSNavonNodeInvocationHandler.copyPropertyMap`. This slice:

- Removes the leftover `@SuppressWarnings("rawtypes")` on `PSEhCacheFactoryBean.getObjectType` by matching `FactoryBean#getObjectType()` (`Class<?>`) and returning `Cache.class`.
- Documents the inherent `LiveStringObjectMap.erase` unchecked conversion as the remaining assembly map cast.
- Restores orphaned `loadLandingPage` javadoc that was left above `copyPropertyMap`.
- Adds `PSEhCacheFactoryBeanTest` covering object type, missing manager, create/put-get, existing-region reuse, and TTL/TTI/eternal branches.

Virtual Site YAML loaders were not touched.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A — tests use in-memory Ehcache `CacheManagerBuilder`; no filesystem path construction.

## Product documentation

N/A — internal compiler-warning cleanup; no operator/user/API/config change.

## Issues

None.

## Notes

- C2: `getObjectType()` return type changed from `Class<? extends Cache>` (raw `Cache`) to `Class<?>` (interface contract). Grep found no `extends PSEhCacheFactoryBean` and no `new PSEhCacheFactoryBean() {`. Downstream module install not required.
- Assemblers/workitem/navon remain as typed in #3284; leftover unchecked is isolated in `PSAssemblyBindingMaps.LiveStringObjectMap.erase`.
- `system` standalone `mvnw clean install`: BUILD SUCCESS; Tests run: 2108, Failures: 0, Errors: 0, Skipped: 241.
