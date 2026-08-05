# Assembly domain Jackson XML deviations (#1891)

Parent: #1823 · Epic: #505 · Depends on: #1887 facade default

## Scope

`PSAssemblyTemplate`, `PSTemplateSlot`, `PSTemplateTypeSlotAssociation`, `PSTemplateBinding`
under `system/services/.../assembly/data/`.

## Wire surface (Jackson)

|              Type               |              Root               |         Nested items          |                        Notes                        |
|---------------------------------|---------------------------------|-------------------------------|-----------------------------------------------------|
| `PSAssemblyTemplate`            | `assembly-template`             | `binding`, `template-slot-id` | Slot membership as scalar longs (not slot graphs)   |
| `PSTemplateSlot`                | `template-slot`                 | `slot-type-association`       | Package unhyphenated tags rewritten before read     |
| `PSTemplateTypeSlotAssociation` | `slot-type-association`         | —                             | Also registered as `template-type-slot-association` |
| `PSTemplateBinding`             | `template-binding` (standalone) | —                             | Nested under template as `binding`                  |

## Restored companions

- `PSAssemblyTemplate.getTemplateSlotIds` / `setTemplateSlotIds` / `addTemplateSlotId` — lost in the
  Java 11 modernization; required for package `template-slot-ids` contract. Offline unit path
  attaches GUID-only slot placeholders when the assembly service is unavailable.
- Shared `IPSGuid`/`PSGuid` string converters in `PSJacksonXmlSerializationHelper` (same as #1888)
  so package `<guid>` elements bind.

## Package-normalize (preserve)

`PSTemplateSlot.fromXML` still rewrites association tags:

- `contenttypeid` → `content-type-id`
- `templateid` → `template-id`
- `slotid` → `slot-id`

Covered by `PSTemplateSlotXmlRestoreTest` (non-zero association IDs).

## Approved deviations vs historical Betwixt writes

|               Topic                |                                                                                                          Behavior                                                                                                           |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Graph-identity `id="…"` attributes | Jackson does not emit Betwixt identity attributes                                                                                                                                                                           |
| Null `global-template`             | Jackson may emit `xsi:nil="true"`; packages use empty element. Read tolerates both (`FAIL_ON_UNKNOWN` + null handling)                                                                                                      |
| Nested binding `jexl-script`       | Historical Betwixt sometimes expanded compiled script objects; Jackson suppresses — expression/variable/order only                                                                                                          |
| Binding `execution-order` `0`      | Packages ship `0`; setter accepts `>= 0` (factory docs still prefer `>= 1`)                                                                                                                                                 |
| Finder-arguments map shape         | Empty map writes as self-closing wrapper; non-empty uses Jackson map element form                                                                                                                                           |
| Dual-engine rollback               | `PSAssemblyTemplate.betwixt` and `PSTemplateTypeSlotAssociation.betwixt` **removed** after golden/package proof. Rollback uses annotations + kebab naming only if re-enabled via `PSXmlSerializationHelper.ENGINE_PROPERTY` |

## Tests

- `PSAssemblyXmlSerializationTest` — golden, round-trip, package smoke (`perc.nav.image.slotDef`, `perc.base.Box.templateDef`)
- `PSTemplateSlotXmlRestoreTest` — package association normalize + non-zero IDs
- `PSJacksonXmlSerializationHelperTest.ipsGuidSerializesAndDeserializesAsBetwixtStringForm`

