# Data Model: Content Repository API Standard Upgrade

**Feature**: `987-jcr-2-0-api-migration`  
**Date**: 2026-07-16  

This feature does **not** introduce new persisted domain tables. It documents the **logical repository projection model** the product already uses, and how it maps under JSR-283 (JCR 2.0).

---

## Entities

### Content repository session
- **Meaning**: Authenticated unit of work for repository-backed operations (product session / content manager usage patterns).
- **Persistence**: Existing CMS session and security context; not a new JCR `Session` product rewrite.
- **Rules**: Lifecycle and save semantics unchanged; no new transaction model.
- **Migration impact**: Indirect — implementors used under session must compile against 2.0.

### Content node / item projection (`PSContentNode` / `IPSNode`)
- **Meaning**: JSR-170/283 `Node` view of a CMS content item (and related structure).
- **Key identity**:
  - **1.0**: `getUUID()` (deprecated in 2.0)
  - **2.0**: `getIdentifier()` (Phase 1 implement; Phase 2 migrate callers of JCR `getUUID`)
- **Properties**: Accessed via `Property` / product property wrappers (`PSProperty`, `PSMultiProperty`).
- **Relationships**: Child nodes via `getNodes` / addNode; references via `getReferences` (+ 2.0 weak references API stubs if unsupported).
- **Validation**: Existing content type / node type configuration (`PSTypeConfiguration` / `NodeType`).
- **Unsupported 2.0 features**: Shareable nodes, lifecycle transitions — methods present for interface compliance; throw UROE or empty results per research R3.

### Repository property / value
- **Meaning**: Typed field values on a node (`Value`, `Property`).
- **Types**: String, long, double, boolean, calendar, stream/binary, reference — existing `PS*Value` classes in `com.percussion.utils.jsr170`.
- **2.0 additions**:
  - `Binary` type and `getBinary()` / `createBinary`
  - `BigDecimal` / `getDecimal()` / `createValue(BigDecimal)`
- **Rules**: Binary content still stored via existing CMS LOB/blob paths; Binary wrapper adapts streams without schema change.

### Repository query (`PSQuery`, `PSQueryResult`)
- **Meaning**: Product content list / finder query using JCR `Query` / `QueryResult` types.
- **Languages retained**: Product-supported `Query.SQL` and `Query.XPATH` (via `PSContentMgr`).
- **2.0 API surface**:
  - Bind variables, limit, offset on `Query` (implement; bind/limit may no-op or wire if already partially supported)
  - `getSelectorNames()` on results
  - `QueryManager.getQOMFactory()` — unsupported unless a minimal stub is justified
- **Out of scope**: Migrating statements to JCR-SQL2/JQOM.

### Shared repository helpers
- **Meaning**: `modules/utils` JSR-170 utilities and system `PSProperty` wrappers.
- **Role**: Preferred place for Binary factory, value conversions, iterators (`PSNodeIterator`, `PSPropertyIterator`).
- **Rules**: No new top-level modules; keep centralized BOM for `javax.jcr:jcr`.

### Exception register (work artifact, not DB)
- **Meaning**: Documented list of non-critical call sites without clear 2.0 replacement (FR-013).
- **Fields**: path/class, rationale, owner, follow-up issue, criticality = non-critical only.
- **Storage**: Feature doc under `specs/987-jcr-2-0-api-migration/` (e.g. `exceptions.md`) or issue tracker; created in Phase 2 if needed.

---

## Relationships (logical)

```text
Session / ContentMgr
    └── creates/executes Query → QueryResult → NodeIterator / RowIterator
    └── loads Node (PSContentNode)
            ├── Property* (PSProperty / PSMultiProperty)
            │       └── Value* (PSBaseValue hierarchy)
            └── NodeType (PSTypeConfiguration)
```

---

## State transitions

No new content lifecycle states. Existing CMS workflow/checkout remains authoritative. JCR versioning/lock methods on `PSContentNode` stay as existing stub/delegate behavior; only signatures/new methods adjust for 2.0.

---

## Validation rules (migration-specific)

| Rule | Enforcement |
|------|-------------|
| No content data migration required | FR-003; smoke + existing load tests |
| Critical editor/publish paths have no exception-list entries | FR-013 |
| Legacy 1.0 jcr artifact absent from shipping classpath | FR-001, FR-006 |
| Custom extensions rebuild against 2.0 product | FR-011 |

---

## Identity & uniqueness

- CMS content identity remains system content id / GUID model.
- JCR `getIdentifier()` MUST return a stable string consistent with prior `getUUID()` behavior for referenceable product nodes (typically the same identity string editors/publish already rely on).
