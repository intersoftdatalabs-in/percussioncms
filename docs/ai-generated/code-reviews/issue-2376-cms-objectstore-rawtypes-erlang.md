# Erlang self-review — issue #2376 (cms.objectstore rawtypes batch 2d)

**Date:** 2026-08-08  
**Branch:** `fix/issue-2376-cms-objectstore-rawtypes-2d`  
**Verdict:** **approve**

## Scope reviewed
- `PSKey` — `HashMap<String,String>` name/value map; typed fromXml/toXml/clone locals
- `PSProcessorProxy` / `PSComponentProcessorProxy` — `createComponentProcessorGroups` → `Map<PSProcessorCommon, Collection<IPSDbComponent>>`; typed property sets; `Class<?>` reflection
- `PSItemDefinition` — typed display-mapper / field-set iterators; `Map<String,PSCollection>` options; slot/variant `Iterator` types
- `PSCloningOptions` — `Map<Integer,Integer>` community/site mappings (ctors + getters + field)
- `PSCmsProperty` — `Collection<String>` getValues/setValues; typed key parts
- `PSComponentUtils` — `List<String>` enums; `Iterator<Element>` children
- `PSDbComponentSet` clone residual diamond constructors
- Companion: `PSServerFolderProcessor` site/community mapping consumers
- Tests: `PSKeyTest.testTypedNameValueMapRoundTrip`, `PSCloningOptionsTest.testTypedMappingApis`, `PSComponentUtilsAndPropertyGenericsTest` (3)

## Checks
| Gate | Result |
| --- | --- |
| Behavior change | None intentional — real generics only; cast removals match prior runtime types |
| Bug risk | Low — processor grouping still keys on `PSProcessorCommon` (was always cast); mapping maps already documented as Integer→Integer |
| Portable paths | N/A (no path I/O) |
| Behavioral tests | New/extended tests for key map, cloning maps, component utils/property XML |
| Companion types | Folder processor updated for typed `getSiteMappings`/`getCommunityMappings` |
| Copyright | New test file uses Intersoft 2026 header |

## Notes
- Residual package rawtypes remain (e.g. more `PSItemDefinition`/`PSCoreItem`/`server/*` handlers, display-format iterators, IPSComponent `List` parentComponents from design.objectstore interface). File next residual under #2022 if inventory still large after this batch.
- `@SuppressWarnings("unchecked")` on `PSDisplayMapper.iterator()` sites: design.objectstore collection iterators still raw; local variables typed as `Iterator<PSDisplayMapping>`.

> Co-Authored by Grok Build using grok-4.5 with agent main.
