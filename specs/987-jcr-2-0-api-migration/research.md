# Research: Content Repository API Standard Upgrade (JCR 1.0 → 2.0)

**Feature**: `987-jcr-2-0-api-migration`  
**Date**: 2026-07-16  
**Sources**: JSR-283 / `javax.jcr:jcr` 1.0 vs 2.0 jar method diffs; in-repo implementors; issue #506; PR #531; feature spec clarifications

---

## R1. Dependency state and branch baseline

### Decision

Treat **`javax.jcr:jcr:2.0` as already decided on `development`** (merged #531). Feature work is application compatibility. The feature branch must **sync with `development`** before compile work so the BOM pin is 2.0 (current feature branch may still show 1.0 if cut before the merge).

### Rationale

Spec FR-001 / FR-014 and clarification Q5: dependency leads; Phase 1 is compile-clean PR only.

### Alternatives considered

- Re-pin to 1.0 until code ready — rejected (security update already shipped; would reverse #531).
- Atomic dep+code only — rejected by clarification (B).

---

## R2. What actually breaks compile (implementors vs callers)

### Decision

**Phase 1 focus is classes that `implements` javax.jcr interfaces**, not the ~200 files that only *import* and call JCR APIs. Call sites of deprecated methods mostly still compile under 2.0; missing **new interface methods** do not.

### Rationale

`javap` diff of 1.0 vs 2.0 shows additive methods on `Node`, `Property`, `Value`, `ValueFactory`, `Query`, `QueryManager`, `QueryResult`, `NodeType`, `PropertyDefinition`. Product types that implement these interfaces must add methods or fail to compile as abstract implementors.

### Primary implementors (inventory)

|            Type            |                         Module path                          |           Interface(s)           |
|----------------------------|--------------------------------------------------------------|----------------------------------|
| `PSContentNode`            | `system/services/.../contentmgr/data/`                       | `IPSNode` → `javax.jcr.Node`     |
| `PSQuery`                  | same `data/`                                                 | `javax.jcr.query.Query`          |
| `PSQueryResult`            | same `data/`                                                 | `javax.jcr.query.QueryResult`    |
| `RowQueryResult` (inner)   | `system/services/.../publisher/impl/PSQueryResultUtils.java` | `QueryResult`                    |
| `PSContentMgr`             | `system/services/.../contentmgr/impl/`                       | `IPSContentMgr` → `QueryManager` |
| `PSTypeConfiguration`      | `system/services/.../contentmgr/impl/legacy/`                | `javax.jcr.nodetype.NodeType`    |
| `PSBaseValue` + subclasses | `modules/utils/.../jsr170/`                                  | `javax.jcr.Value`                |
| `PSValueFactory`           | `modules/utils/.../jsr170/`                                  | `javax.jcr.ValueFactory`         |
| `PSPropertyDefinition`     | `modules/utils/.../jsr170/`                                  | `PropertyDefinition`             |
| `PSMultiProperty`          | `modules/utils/.../jsr170/`                                  | `IPSProperty` → `Property`       |
| `PSProperty`               | `system/src/.../system/utils/jsr170/`                        | `IPSProperty` → `Property`       |
| `PSMockProperty`           | `modules/utils` tests                                        | `Property`                       |
| Toolkit REST model values  | `modules/perc-toolkit/.../pso/restservice/model/`            | `Value` (String/Date/File/Xhtml) |

### New methods that typically force compile work

**`Node` (PSContentNode)** — among others:
- `getIdentifier()`
- `getNodes(String[])`, `getProperties(String[])`
- `getReferences(String)`, `getWeakReferences()`, `getWeakReferences(String)`
- `setProperty(String, Binary)`, `setProperty(String, BigDecimal)`
- `setPrimaryType(String)`
- `getSharedSet()`, `removeShare()`, `removeSharedSet()`
- `followLifecycleTransition(String)`, `getAllowedLifecycleTransistions()` (spec spelling)
- Exception signature changes on some versioning methods (`checkout`, `restore`, `Item.remove`)

**`Value` (PSBaseValue hierarchy + toolkit values)**:
- `getBinary()`, `getDecimal()`
- Removal of `IllegalStateException` from several method signatures (override compatibility)

**`Property` (PSProperty, PSMultiProperty, PSMockProperty)**:
- `getBinary()`, `getDecimal()`, `isMultiple()`, `setValue(Binary)`, `setValue(BigDecimal)`
- `getNode()` / `getProperty()` signature nuances

**`ValueFactory` (PSValueFactory)**:
- `createBinary(InputStream)`, `createValue(Binary)`, `createValue(BigDecimal)`, `createValue(Node, boolean)`

**`Query` (PSQuery)**:
- `bindValue(String, Value)`, `getBindVariableNames()`, `setLimit(long)`, `setOffset(long)`
- `execute()` may declare `InvalidQueryException`

**`QueryManager` (PSContentMgr)**:
- `getQOMFactory()` → return QOM factory or throw `UnsupportedRepositoryOperationException` if product does not support JQOM

**`QueryResult`**:
- `getSelectorNames()`

**`PropertyDefinition`**:
- `isFullTextSearchable()`, `isQueryOrderable()`, `getAvailableQueryOperators()`

**`NodeType` (PSTypeConfiguration)**:
- Hierarchy change: `NodeType` extends `NodeTypeDefinition` in 2.0
- `canRemoveNode` / `canRemoveProperty`, subtype iterators; methods formerly only on `NodeType` may now live on `NodeTypeDefinition`

### Alternatives considered

- Reflection-only stubs — rejected (fragile, fails static compile).
- Drop implementing javax.jcr interfaces — rejected (product architecture is JSR-170 based).

---

## R3. Phase 1 stub semantics

### Decision

For **unsupported** JCR 2.0 capabilities (lifecycle, shareable nodes, weak references, JQOM, etc.), implement required methods to:
- **Throw** `UnsupportedRepositoryOperationException` (or return empty iterators / false) consistent with existing PSContentNode patterns for checkin/lock/version APIs that are already stubs, **or**
- **Delegate** to existing 1.0 behavior where there is a clear mapping (e.g. `getIdentifier()` → current UUID/identity source used by `getUUID()`).

Do **not** invent full JCR 2.0 repository features the CMS never supported.

### Rationale

Percussion uses JCR types as a content projection over the existing CMS store, not as a full Jackrabbit repository. Spec: no storage rewrite; preserve behavior.

### Alternatives considered

- Full Jackrabbit embed — out of scope.
- Leave methods abstract — does not compile.

---

## R4. Phase 2 deprecation cleanup targets

### Decision

After compile-clean, inventory and migrate **product call sites** of deprecated 1.0 APIs with clear 2.0 replacements:

|           1.0 (deprecated)            |                  2.0 replacement                   |                                                          Notes                                                          |
|---------------------------------------|----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `Node.getUUID()`                      | `Node.getIdentifier()`                             | High volume of **false positives**: many `getUUID()` calls are Percussion `IPSGuid.getUUID()`, not JCR                  |
| `Property`/`Value` stream-only binary | `Binary` / `getBinary()`                           | Prefer when touching binary paths                                                                                       |
| Optional: `Query.SQL` constant usage  | Keep **language** `Query.SQL` / product SQL parser | Product query language is proprietary SQL-like JSR-170 SQL, not necessarily JCR-SQL2; **do not** force JCR-SQL2 rewrite |
| Optional: new query languages         | Out of scope                                       | Clarification: no optional 2.0 features                                                                                 |

### Rationale

Clarification Q1 option B: deprecation cleanup where clear replacement exists; no optional modernization. Product still supports SQL/XPATH languages via `PSContentMgr.getSupportedQueryLanguages()`.

### Filter for `getUUID` inventory

Must distinguish:
1. `javax.jcr.Node#getUUID` / calls on `Node` / `IPSNode`
2. Product GUID APIs (`guid.getUUID()`, design models) — **not** JCR migration

Use type-aware search (IDE / error-prone / careful review), not naive text replace.

---

## R5. Shared helpers placement

### Decision

Put **shared Binary/identifier/value adaptations** in `modules/utils` under `com.percussion.utils.jsr170` (and existing `system/.../system/utils/jsr170` only when system-layer Property types already live there). Prefer extending `PSValueFactory`, `PSBaseValue`, and small helpers over scattering stubs.

### Rationale

Constitution I (shared code in utils); FR-013 prefers product-owned shared helpers; call-site consistency.

### Alternatives considered

- Per-module copy of Binary wrapper — rejected (duplication).
- New top-level module — rejected (complexity budget).

---

## R6. Delivery phases and PR boundaries

### Decision

| Phase |                                                           Scope                                                            |                               PR policy                               |
|-------|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| **0** | Rebase/merge `development` so BOM is jcr 2.0; capture compile error inventory                                              | May be part of Phase 1 PR prep                                        |
| **1** | Implement missing interface methods + exception signature fixes until **full product compile** green                       | **Own PR** (FR-014, SC-008); automated tests for touched implementors |
| **2** | Deprecation cleanup by module cluster (utils → system contentmgr → publisher/assembly → sitemanage → toolkit → extensions) | Story-sized PRs; automated tests                                      |
| **3** | Docs (release notes, integrator rebuild), dependency/CVE note, exception register                                          | Can combine with last Phase 2 PR                                      |
| **4** | Feature-complete: designated automated suites + **scripted smoke** (FR-012)                                                | Gate before calling feature done                                      |

### Rationale

Clarifications Q3, Q5; constitution story-checkpoint PRs.

---

## R7. Testing strategy

### Decision

- **Phase 1**: Unit tests for new methods on implementors (identifier, Binary, Query limit/offset no-ops or behavior, PropertyDefinition flags). Update `modules/utils` `PSValuesTest` / contentmgr tests as needed. Module compiles via `./mvnw`.
- **Phase 2**: Existing query/finder tests; add focused tests where deprecation changes behavior.
- **Feature-complete**: Designated module test suites + documented smoke (create/save, open, preview, one publish).
- **Not required**: Full multi-site UAT as merge gate.

### Rationale

Constitution III; FR-005, FR-012; clarification Q3 option B.

---

## R8. Public / integrator contracts

### Decision

- **REST/SOAP/sitemanage HTTP contracts**: no intentional change.
- **Custom Java extensions**: source rebuild required (clarification Q2).
- Document: repository API is JSR-283 (2.0); rebuild extensions; list any rare signature changes if product public types leak JCR interfaces.

### Rationale

FR-009, FR-011.

---

## R9. Explicit non-goals (research confirmation)

- Migrating query language from product SQL/XPATH to JCR-SQL2/JQOM
- Replacing CMS content store with Apache Jackrabbit repository
- Supporting mixed 1.0/2.0 nodes in a cluster
- Binary compatibility for pre-built third-party extension JARs
- 8.1.x line

---

## R10. Open items deferred to implementation (not blocking plan)

- Exact exception vs empty-iterator choice per stub method (follow existing PSContentNode patterns at implementation time)
- Whether `getQOMFactory()` returns a minimal stub factory or only throws UROE
- Full compile error list after rebase (run Phase 0 inventory on synced tree)

All Technical Context unknowns for planning are resolved enough to design and task.
