# Issue #1920 — Catalog / system / ui leftovers (first sub-batch)

Parent: #1892 · Grandparent: #1823 · Epic: #505 · Inventory: #1821

## Scope delivered (this PR)

|           Class           |       Root element        |                              Nested / notes                              |
|---------------------------|---------------------------|--------------------------------------------------------------------------|
| `PSAudit`                 | `audit`                   | Scalar `id` identity; derived `guid` omitted                             |
| `PSAuditTrail`            | `audit-trail`             | `audits` wrapper / nested `audit` (`addType`)                            |
| `PSSharedProperty`        | `shared-property`         | `guid` + name + value; Hibernate `version` suppressed                    |
| `PSHierarchyNode`         | `hierarchy-node`          | `properties` key-as-element map; type enum name; catalog aliases omitted |
| `PSHierarchyNodeProperty` | `hierarchy-node-property` | Scalar `node-id` (not historical betwixt `parentGuid`)                   |

Golden fixtures + round-trip + legacy `<null>` root tests:

- `system/.../system/data/PSSystemDataXmlSerializationTest`
- `system/.../ui/data/PSHierarchyNodeXmlSerializationTest`

## Approved / intentional deviations vs historical Betwixt

|                          Deviation                           |                                                          Notes                                                           |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| No Betwixt graph-identity `id="…"` attributes                | Same as #1887 facade / other domain batches.                                                                             |
| No XML declaration                                           | `PSJacksonXmlSerializationHelper` default.                                                                               |
| `PSAudit` omits derived `guid`                               | Identity is scalar `id`; avoids dual-write / `setGUID` one-shot races.                                                   |
| `event-time` UTC ISO8601 `yyyyMMdd'T'HHmmssSSS`              | Matches Betwixt `PSDateFormatISO8601` pattern; timezone fixed to UTC for goldens (Betwixt used JVM default zone).        |
| Hibernate `version` suppressed                               | `@IPSXmlSerialization(suppress=true)` + `@JsonIgnore` on hierarchy + shared property.                                    |
| `PSHierarchyNode` catalog `label` / `description` suppressed | Aliases of name / always null — not design wire.                                                                         |
| `PSHierarchyNode` type as enum name                          | `FOLDER` / `PLACEHOLDER` (not ordinal `type-int`).                                                                       |
| `PSHierarchyNode.properties` key-as-element map              | Sorted `TreeMap` for stable goldens (same pattern as filter params).                                                     |
| Null root `parent-id` omitted / nil                          | Root nodes may omit parent; read accepts absent parent.                                                                  |
| `PSHierarchyNodeProperty` uses `node-id`                     | Historical `.betwixt` mapped non-existent `parentGuid`→`parentId` (typo-ridden file); production bean field is `nodeId`. |
| Dropped `PSHierarchyNodeProperty.betwixt`                    | Unused under Jackson-default facade; golden proves modern wire.                                                          |
| Null-safe `setVersion`                                       | BeanUtils copy after Jackson restore may pass null.                                                                      |
| `setNodeType` + idempotent `setType`                         | BeanUtils property `nodeType` must round-trip enum form.                                                                 |

## Residual (file as children of #1920)

Not in this PR:

1. **PSDependency / PSDependent** — system dependency graph leftovers
2. **PSMimeContentAdapter** — system content adapter design XML
3. Any other catalog/system/ui holdouts discovered after re-inventory (e.g. `PSLegacyGuid` A-only, `PSLocale` A-only are suppress-only and may not need a slice)

## Out of scope

- filter (#1915), sitemgr (#1918), publisher/pubserver (#1919)
- content leftovers (#1921)
- `PSObjectSummary` (#1903 closed)
- Betwixt POM removal (#1824)
- Facade engine deletion

## Tests

|                 Suite                 |                                Coverage                                 |
|---------------------------------------|-------------------------------------------------------------------------|
| `PSSystemDataXmlSerializationTest`    | audit / audit-trail / shared-property golden + RT + legacy null root    |
| `PSHierarchyNodeXmlSerializationTest` | hierarchy-node / hierarchy-node-property golden + RT + legacy null root |

