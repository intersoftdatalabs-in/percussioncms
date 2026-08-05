# Issue #1921 — content leftovers + PSNodeDefinition Jackson deviations

Parent: #1892 · Grandparent: #1823 · Epic: #505

## Scope delivered

|               Type               |      Package      |                                     Notes                                     |
|----------------------------------|-------------------|-------------------------------------------------------------------------------|
| `PSAutoTranslation`              | `content.data`    | Opt-in Jackson; suppress catalog aliases, version, GUID, `Optional` accessors |
| `PSFieldDescription`             | `content.data`    | Root `field-description`; `exportable` omit-null                              |
| `PSContentTypeSummary` / `Child` | `content.data`    | Nested `field-description` / `content-type-summary-child`                     |
| `PSFolderProperty`               | `content.data`    | Added `toXML`/`fromXML`; opt-in surface; suppress `Optional` accessors        |
| `PSItemStatus`                   | `content.data`    | No-arg ctor + `setId`; content id as wire property `id`                       |
| `PSNodeDefinition`               | `contentmgr.data` | Opt-in surface; `template-id` / workflow `string` items; TreeSet order        |

## Approved Jackson deviations vs historical Betwixt

- No graph-identity `id="…"` attributes on complex elements
- No XML declaration (`PSJacksonXmlSerializationHelper` default)
- Catalog / `Optional` / Hibernate association graph suppressed on modern write
- `PSNodeDefinition` template association **restore** still needs live content-manager (integration tests); offline suite pins write shape + scalar restore
- `PSNodeDefinition` workflow association **restore** is offline-friendly via `setWorkflowIds` / `addWorkflowGuid` (new `PSContentTypeWorkflow` rows; association PK left unset for Hibernate). Live package install may still re-merge existing DB rows via `PSContentTypeHelper`.
- `PSNodeDefinition#getId` / `getRawContentType` fail-fast (NPE) when unset — no synthetic `0L` (historical Betwixt unboxing).
- `PSItemStatus#setFromState` / `setToState` allow `null` (no transition / Jackson absent optionals) and reject empty non-null strings. Historical setters used `StringUtils.isEmpty` (also rejected null); null is intentional for Jackson + domain.
- No production `.betwixt` files existed for these types (nothing to drop)

## Tests

- `PSContentLeftoversXmlSerializationTest` — golden + round-trip + null/empty state contract
- `PSNodeDefinitionXmlSerializationTest` — golden + scalar/package smoke + workflow restore + fail-fast id getters

