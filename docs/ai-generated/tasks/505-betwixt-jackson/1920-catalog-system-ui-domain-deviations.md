# Issue #1920 — Catalog / system / ui leftovers

Parent: #1892 · Grandparent: #1823 · Epic: #505 · Inventory: #1821

> **Status: CLOSED (2026-08-05 re-inventory)** — all inventory **RW** types for this
> slice are on Jackson-backed `PSXmlSerializationHelper` with golden/RT tests.
> Remaining items are **A-only suppress** (`PSLegacyGuid`, `PSLocale`) — no further
> RW sub-batch required. Betwixt POM / leftover `.betwixt` purge remains #1824 / #2062.

## Delivery roll-up

|           Class           |       Root element        |                 Sub-batch                  |                                    PR                                    |
|---------------------------|---------------------------|--------------------------------------------|--------------------------------------------------------------------------|
| `PSAudit`                 | `audit`                   | first (#1920)                              | [#1995](https://github.com/intersoftdatalabs-in/percussioncms/pull/1995) |
| `PSAuditTrail`            | `audit-trail`             | first (#1920)                              | [#1995](https://github.com/intersoftdatalabs-in/percussioncms/pull/1995) |
| `PSSharedProperty`        | `shared-property`         | first (#1920)                              | [#1995](https://github.com/intersoftdatalabs-in/percussioncms/pull/1995) |
| `PSHierarchyNode`         | `hierarchy-node`          | first (#1920)                              | [#1995](https://github.com/intersoftdatalabs-in/percussioncms/pull/1995) |
| `PSHierarchyNodeProperty` | `hierarchy-node-property` | first (#1920); dropped obsolete `.betwixt` | [#1995](https://github.com/intersoftdatalabs-in/percussioncms/pull/1995) |
| `PSDependent`             | `dependent`               | residual #1993                             | [#2013](https://github.com/intersoftdatalabs-in/percussioncms/pull/2013) |
| `PSDependency`            | `dependency`              | residual #1993                             | [#2013](https://github.com/intersoftdatalabs-in/percussioncms/pull/2013) |
| `PSMimeContentAdapter`    | `mime-content-adapter`    | residual #1994                             | [#2047](https://github.com/intersoftdatalabs-in/percussioncms/pull/2047) |

Tracked separately (not this issue): `PSObjectSummary` / `PSObjectLockSummary` — #1903
(closed).

### Sub-batch deviation docs

|             Sub-batch              |                                         Doc                                          |
|------------------------------------|--------------------------------------------------------------------------------------|
| First (audit / shared / hierarchy) | this file (sections below)                                                           |
| Dependency                         | [1993-dependency-domain-deviations.md](./1993-dependency-domain-deviations.md)       |
| Mime content adapter               | [1994-mime-content-adapter-deviations.md](./1994-mime-content-adapter-deviations.md) |

## Re-inventory proof (2026-08-05)

Cross-check of inventory §6.9 (`docs/ai-generated/tasks/505-betwixt-jackson/00-inventory.md`)
and workspace sources on `main`:

|        Inventory class         |    Role    |                                        Disposition                                        |
|--------------------------------|------------|-------------------------------------------------------------------------------------------|
| `PSObjectSummary`              | RW + A + T | #1903 closed (not in #1920 scope)                                                         |
| `PSAudit` / `PSAuditTrail`     | RW         | Jackson + goldens — PR #1995                                                              |
| `PSDependency` / `PSDependent` | RW         | Jackson + goldens — PR #2013 / #1993 closed                                               |
| `PSMimeContentAdapter`         | RW         | Jackson + goldens — PR #2047 / #1994 closed                                               |
| `PSSharedProperty`             | RW         | Jackson + goldens — PR #1995                                                              |
| `PSHierarchyNode`              | RW + A     | Jackson + goldens — PR #1995                                                              |
| `PSHierarchyNodeProperty`      | RW         | Jackson + goldens; production `.betwixt` removed — PR #1995                               |
| `PSLegacyGuid`                 | **A only** | suppress properties only — no design RW surface                                           |
| `PSLocale`                     | **A only** | suppress properties only — nested in security tests via `addType`; no standalone RW slice |

### Production `*.betwixt` remaining (not #1920)

Repo-wide source scan (excluding `target/` and utils junit fixtures):

|                      File                      |             Owning domain / issue             |
|------------------------------------------------|-----------------------------------------------|
| `system/.../content/data/PSKeyword.betwixt`    | keywords (#1888 line) — not catalog leftovers |
| `system/.../guidmgr/data/PSGuid.betwixt`       | guid pilot / shared                           |
| `system/.../security/data/PSCommunity.betwixt` | security (#1889)                              |

No remaining production `.betwixt` under `services/ui`, `services/system`, or
`services/catalog`. `PSHierarchyNodeProperty.betwixt` was dropped in #1995.

### Grep holdouts

- Catalog package: only `PSObjectSummary` is RW design XML (done under #1903).
- System data package: all RW types above carry `@JacksonXmlRootElement` +
  `PSXmlSerializationHelper` `fromXML`/`toXML`.
- UI data package: `PSHierarchyNode*` Jackson + goldens.
- `PSLegacyGuid` / `PSLocale`: `@IPSXmlSerialization(suppress=true)` only — **no**
  new golden/RT batch required.

## Scope delivered (first sub-batch — PR #1995)

|           Class           |       Root element        |                              Nested / notes                              |
|---------------------------|---------------------------|--------------------------------------------------------------------------|
| `PSAudit`                 | `audit`                   | Scalar `id` identity; derived `guid` omitted                             |
| `PSAuditTrail`            | `audit-trail`             | `audits` wrapper / nested `audit` (`addType`)                            |
| `PSSharedProperty`        | `shared-property`         | `guid` + name + value; Hibernate `version` suppressed                    |
| `PSHierarchyNode`         | `hierarchy-node`          | `properties` key-as-element map; type enum name; catalog aliases omitted |
| `PSHierarchyNodeProperty` | `hierarchy-node-property` | Scalar `node-id` (not historical betwixt `parentGuid`)                   |

Golden fixtures + round-trip + legacy `<null>` root tests:

- `system/.../system/data/PSSystemDataXmlSerializationTest` (also hosts #1993 / #1994 cases)
- `system/.../ui/data/PSHierarchyNodeXmlSerializationTest`

## Approved / intentional deviations vs historical Betwixt (first sub-batch)

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

See [1993-dependency-domain-deviations.md](./1993-dependency-domain-deviations.md) and
[1994-mime-content-adapter-deviations.md](./1994-mime-content-adapter-deviations.md) for
residual sub-batch deviations.

## Residual

**None for #1920.** Historical residuals shipped:

1. ~~PSDependency / PSDependent~~ → #1993 closed / PR #2013
2. ~~PSMimeContentAdapter~~ → #1994 closed / PR #2047
3. ~~Other catalog/system/ui RW holdouts~~ → re-inventory found none
4. `PSLegacyGuid` / `PSLocale` A-only — intentionally **not** sliced (suppress-only)

Epic residual (not this issue): Betwixt POM removal / leftover `.betwixt` files →

# 1824 / #2062. Do **not** remove `commons-betwixt` from this slice.

## Out of scope

- filter (#1915), sitemgr (#1918), publisher/pubserver (#1919)
- content leftovers (#1921)
- `PSObjectSummary` (#1903 closed)
- Betwixt POM removal (#1824 / #2062)
- Facade engine deletion

## Tests

|                 Suite                 |                                                       Coverage                                                       |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `PSSystemDataXmlSerializationTest`    | audit / audit-trail / shared-property / dependent / dependency / mime-content-adapter golden + RT + legacy null root |
| `PSHierarchyNodeXmlSerializationTest` | hierarchy-node / hierarchy-node-property golden + RT + legacy null root                                              |
| `PSMimeContentAdapterTest`            | interface programming surface (JUnit 5; #1994)                                                                       |

