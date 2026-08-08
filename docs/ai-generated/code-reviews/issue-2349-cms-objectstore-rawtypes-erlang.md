# Erlang self-review — issue #2349 (cms.objectstore rawtypes batch 2c)

**Date:** 2026-08-07  
**Branch:** `fix/issue-2349-cms-objectstore-rawtypes`  
**Verdict:** **approve**

## Scope reviewed
- `PSSearch` / `PSSFields` / `PSMultiValuedProperty` typed iterators and property loops
- `PSServerFolderProcessor` + `PSFolderProcessorProxy` relationship/folder `List` parameterization
- Unit test `PSSearchFieldsPropertiesTest`

## Checks
| Gate | Result |
| --- | --- |
| Behavior change | None intentional — generics only; property/field mutation paths preserve prior casts |
| Bug risk | Low — IPSRelationship uses `List<?>`, IPSFolder uses `List<PSLocator>` matching interfaces |
| Portable paths | N/A (no path I/O) |
| Behavioral tests | 4 new tests for get/set/remove fields + property iterator/value APIs |
| Companion types | Proxy methods aligned with `IPSFolderProcessor` |

## Notes
- Residual package rawtypes remain (other `cms.objectstore` / folder processor internal collections); file next residual under #2022.
- Clone-folder path still uses mixed `PSLocator` / `PSLocatorWithName` via `List<Object>` + cast into summary APIs (pre-existing dual-type pattern).

> Co-Authored by Grok Build using grok-4.5 with agent main.
