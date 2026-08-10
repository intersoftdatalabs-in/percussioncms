# Erlang self-review — issue #2030 perc-toolkit Xlint batch 1

**Date:** 2026-08-08  
**Branch:** `fix/issue-2030-perc-toolkit-xlint-batch1`  
**Module:** `modules/perc-toolkit`  
**Verdict:** **Approve** (commit-ready)

## Scope

PR-sized batch 1 for parent #2200 / issue #2030. Live main-source project `-Xlint` diagnostics **87 → 26** (~61 cleared). Prefer real generics and mechanical easy wins; no blanket suppressions.

### In batch

- For-removal boxed constructors (`Long`/`Double`/`Boolean`) → valueOf / primitive casts
- `serialVersionUID` on `ArchivedException`, `UniqueIdLocatorSet`, `TrashTask.FatalTaskException`
- Missing `@Deprecated` on `QueuedEdition` + `PublishEditionService` deprecated API surface
- Real generics: `MutableHttpServletRequestWrapper` servlet override maps/enumerations; `PSOMutableUrl` entry iteration; `PSOParseUrlQueryString` multi-value lists; `SimplifyParameters` `List<?>`; `PSOListTools` reverse/`Class<?>`/`sublist` cast
- JAXB `Class[]` → `JAXBContext.newInstance(Item.class)` style
- Raw upstream iterators consumed as `Iterator<?>` + element cast (folder properties, extensions, ACL, AA relations)
- `SlotItemComparator.hashCode` consistent with all-equal `equals`

### Out of batch (residual)

- `this-escape` constructor sites (~14): Error/Field/Http*Response/FolderTools/PSOExtensionParamsHelper/relationship builders/PSORequestContext
- `PSORequestContext` cascade of parent `PSRequestContext` override rawtype/unchecked mismatches (~11) — fix belongs with parent API typing in perc-system or local suppress after parent work
- `PropertyData` non-serializable field type (~1)
- Any remaining test-source diagnostics under uncapped inventory

## Checklist

|              Gate               |                                                                                 Result                                                                                  |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs in typed refactors         | None found — collections already held typed elements; only declarations/casts updated                                                                                   |
| Portable paths / file I/O       | N/A (no path-string changes)                                                                                                                                            |
| Behavioral unit tests           | New: `PSOMutableUrlTest`, `PSOParseUrlQueryStringTest`; extended `PSOSlotContentsTest`, `UniqueIdLocatorSetTest`; existing image/simplify/wrapper tests cover easy wins |
| Prefer real fix over suppress   | Yes — no new blanket `@SuppressWarnings`                                                                                                                                |
| Standalone `mvnw clean install` | BUILD SUCCESS — Tests run: 223, Failures: 0, Errors: 0, Skipped: 16                                                                                                     |
| Scope confined to perc-toolkit  | Yes                                                                                                                                                                     |

## Residual issue

Follow-up residual child for remaining ~26 main-source diagnostics (this-escape + PSORequestContext cascade + serial field).
