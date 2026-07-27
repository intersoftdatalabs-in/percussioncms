# Tasks: Content Repository API Standard Upgrade (JCR 1.0 → 2.0)

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [quickstart.md](./quickstart.md), [contracts/](./contracts/)  
**Branch**: `1286-jcr-2-0-api-migration` | **Spec dir**: `specs/987-jcr-2-0-api-migration`  
**Related**: Issue #506, dependency #531

## Phase 1: Setup

- [x] T001 Identify owning module paths (`system/`, `modules/utils`) and read AGENTS hierarchy (root `./AGENTS.md` plus any module `AGENTS.md` / `AGENTS.local.md`)
- [x] T002 Confirm branch is based on JDK 21 `development` tooling and `./mvn-env.sh -v` (or equivalent) reports a usable JDK 21 toolchain
- [x] T003 Merge or rebase `origin/development` into the feature branch so parent `pom.xml` dependencyManagement pins `javax.jcr:jcr` to **2.0** (verify with search in `pom.xml`)
- [x] T004 Create repo temp working dir notes under `tmp/` for compile logs (e.g. `tmp/jcr-compile-phase1.log`) per AGENTS temp-dir rules

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: Establish compile inventory and security/scope boundaries before any story PR.  
**Blocks**: All user stories.

- [x] T005 Confirm no shipping module redeclares `javax.jcr:jcr:1.0` outside BOM by scanning module `pom.xml` files under `system/`, `modules/`, `projects/`, `deployer/`
- [x] T006 Run initial compile of primary modules via `./mvn-env.sh -pl modules/utils,system -am compile -DskipTests` and capture full output to `tmp/jcr-compile-phase1.log`
- [x] T007 Expand compile to remaining JCR consumers as needed (`projects/sitemanage`, `modules/perc-toolkit`, `modules/segmentation-rx`, `deployer`, extensions) and append errors to `tmp/jcr-compile-phase1.log`
- [x] T008 Map compile errors to implementor inventory in [research.md](./research.md) R2; update research or `tmp/jcr-implementor-checklist.md` if new implementors appear
- [x] T009 Assess security surface: confirm scope is API implementor/call-site only (no XML parser, upload, or authZ changes expected); document any accidental touch risks in `tmp/jcr-security-notes.md`
- [x] T010 Re-read [contracts/jcr-2.0-implementor-surface.md](./contracts/jcr-2.0-implementor-surface.md) and [contracts/integrator-rebuild.md](./contracts/integrator-rebuild.md) as the acceptance contract for later stories

---

## Phase 3: User Story 2 — Product builds on the supported standard (Priority: P1) — **MVP / Phase-1 PR**

**Goal**: Restore a full product compile against `javax.jcr:jcr:2.0` by implementing missing JSR-283 methods on product implementors; submit **compile-clean-only** PR (FR-014, SC-008).  
**Independent Test**: `./mvn-env.sh -DskipTests compile` succeeds; `dependency:tree` shows only jcr 2.0; PR opened with compile scope only (no full deprecation cleanup required).  
**Maps to plan**: Phase 1 compile-clean.

### Tests (Required)

- [x] T011 [P] [US2] Add/extend unit tests for `Value` Binary/Decimal behavior in `modules/utils/src/test/java/com/percussion/utils/jsr170/PSValuesTest.java` (or new test class under same package)
- [x] T012 [P] [US2] Add unit tests for `PSValueFactory` `createBinary` / `createValue(Binary|BigDecimal)` in `modules/utils/src/test/java/com/percussion/utils/jsr170/`
- [x] T013 [P] [US2] Add unit tests for `PSPropertyDefinition` JCR 2.0 query-metadata methods in `modules/utils/src/test/java/com/percussion/utils/jsr170/`
- [x] T014 [P] [US2] Add unit tests for `PSQuery` bind/limit/offset/`getBindVariableNames` in `system/services/src/test/` (or existing contentmgr test package under `system/`)
- [x] T015 [P] [US2] Add unit tests for `PSContentNode.getIdentifier()` identity parity with existing UUID source in `system/services/src/test/` contentmgr tests

### Implementation

- [x] T016 [P] [US2] Implement JCR 2.0 `Value` methods (`getBinary`, `getDecimal`, exception signature fixes) in `modules/utils/src/main/java/com/percussion/utils/jsr170/PSBaseValue.java` and subclasses (`PSStringValue`, `PSLongValue`, `PSDoubleValue`, `PSBooleanValue`, `PSCalendarValue`, `PSInputStreamValue`, `PSReferenceValue`)
- [x] T017 [P] [US2] Implement JCR 2.0 `ValueFactory` methods in `modules/utils/src/main/java/com/percussion/utils/jsr170/PSValueFactory.java` (including `createBinary`, `createValue(Binary)`, `createValue(BigDecimal)`, `createValue(Node, boolean)`)
- [x] T018 [P] [US2] Implement JCR 2.0 `PropertyDefinition` methods in `modules/utils/src/main/java/com/percussion/utils/jsr170/PSPropertyDefinition.java`
- [x] T019 [P] [US2] Implement JCR 2.0 `Property` methods in `modules/utils/src/main/java/com/percussion/utils/jsr170/PSMultiProperty.java` and `system/src/main/java/com/percussion/system/utils/jsr170/PSProperty.java`
- [x] T020 [P] [US2] Update test double `modules/utils/src/test/java/com/percussion/utils/testing/PSMockProperty.java` for new `Property` methods
- [x] T021 [US2] Implement JCR 2.0 `Node` methods on `system/services/src/com/percussion/services/contentmgr/data/PSContentNode.java` per [research.md](./research.md) R3 (`getIdentifier` maps to existing identity; unsupported optional features UROE/empty)
- [x] T022 [US2] Implement JCR 2.0 `Query` methods on `system/services/src/com/percussion/services/contentmgr/data/PSQuery.java` (`bindValue`, `getBindVariableNames`, `setLimit`, `setOffset`, `execute` signature)
- [x] T023 [P] [US2] Implement `QueryResult.getSelectorNames` on `system/services/src/com/percussion/services/contentmgr/data/PSQueryResult.java` and `system/services/src/com/percussion/services/publisher/impl/PSQueryResultUtils.java` (`RowQueryResult`)
- [x] T024 [US2] Implement `QueryManager.getQOMFactory` on `system/services/src/com/percussion/services/contentmgr/impl/PSContentMgr.java` (and `IPSContentMgr` if signature surface requires) per contract UROE/stub policy
- [x] T025 [US2] Implement JCR 2.0 `NodeType` hierarchy/methods on `system/services/src/com/percussion/services/contentmgr/impl/legacy/PSTypeConfiguration.java`
- [x] T026 [P] [US2] Implement JCR 2.0 `Value` methods on toolkit models in `modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/model/StringValue.java`, `DateValue.java`, `FileValue.java`, `XhtmlValue.java`
- [x] T027 [US2] Fix any remaining compile errors in secondary modules (`projects/sitemanage`, `modules/segmentation-rx`, `deployer`, `modules/extensions-*`, `modules/p13n-api`, `modules/ContentUI`) only as required for compile—not deprecation cleanup
- [x] T028 [US2] Run `./mvn-env.sh -DskipTests compile` to BUILD SUCCESS and re-run US2 unit tests (`modules/utils`, contentmgr tests)
- [x] T029 [US2] Verify `./mvn-env.sh -pl system -am dependency:tree -Dincludes=javax.jcr:jcr` shows only version 2.0
- [x] T030 [US2] Commit compile-clean work only and open Phase-1 PR (PR #1448 submitted & merged); link #506 / this spec
- [x] T031 [US2] Monitor CI/Kilo Code checks on the Phase-1 PR; address feedback; resolve review threads per AGENTS (PR #1448 review threads resolved)
- [x] T032 [US2] Verify human approval and merge of Phase-1 compile PR before starting User Story 1 deprecation work (PR #1448 merged to development)

---

## Phase 4: User Story 1 — Editors and publishers keep working (Priority: P1)

**Goal**: Preserve create/edit/preview/publish behavior; migrate deprecated JCR 1.0 call sites with clear 2.0 replacements on **critical** editor/assembly/publish paths; no unjustified exceptions on those paths (FR-002, FR-004, FR-013).  
**Independent Test**: Designated automated tests for contentmgr/finders/publisher green; critical paths use `getIdentifier` (or helpers) not deprecated JCR `getUUID`; no behavior regressions in module tests.  
**Maps to plan**: Phase 2 deprecation cleanup (core).  
**Depends on**: US2 merge (compile baseline).

### Tests (Required)

- [ ] T033 [P] [US1] Add/adjust contentmgr or finder tests covering node identity after `getIdentifier` migration under `system/services/src/test/`
- [ ] T034 [P] [US1] Add/adjust publisher/content-list tests if query/result changes affect generators under `system/services/src/test/` (or existing publisher test packages)
- [ ] T035 [P] [US1] Add/adjust sitemanage tests for JCR finder paths under `projects/sitemanage/src/test/` where `PSJcrNodeFinder` / path services are covered

### Implementation

- [x] T036 [US1] Produce type-aware inventory of **JCR** `Node.getUUID` call sites (exclude `IPSGuid.getUUID` and non-JCR GUIDs) into `specs/987-jcr-2-0-api-migration/getuuid-inventory.md` covering `system/services`, `modules/utils`, `projects/sitemanage`
- [x] T037 [US1] Migrate critical contentmgr/assembly/publisher JCR `getUUID` call sites to `getIdentifier()` (or shared helper in `modules/utils/src/main/java/com/percussion/utils/jsr170/`) under `system/services/src/com/percussion/services/`
- [x] T038 [P] [US1] Migrate critical sitemanage JCR node identity call sites in `projects/sitemanage/src/main/java/com/percussion/share/dao/PSJcrNodeFinder.java`, `pathmanagement`, and related editor-facing services
- [x] T039 [P] [US1] Review binary property touchpoints in contentmgr loaders (`system/services/src/com/percussion/services/contentmgr/`) and adopt `Binary` APIs only where a clear replacement improves correctness without drive-by rewrites
- [x] T040 [US1] Explicitly **do not** convert product `Query.SQL` / `Query.XPATH` languages to JCR-SQL2; leave `system/services/src/com/percussion/services/contentmgr/impl/PSContentMgr.java` language support intact
- [x] T041 [US1] Record any non-critical hard cases that cannot be migrated in `specs/987-jcr-2-0-api-migration/exceptions.md` (owner, rationale, follow-up); ensure **zero** exceptions on critical editor/publish paths
- [x] T042 [US1] Run designated automated tests: `./mvn-env.sh -pl modules/utils,system,projects/sitemanage -am test` (adjust modules if failures are pre-existing and documented)
- [x] T043 [US1] Commit US1 deprecation/critical-path work and open PR (PR #1449 submitted); pause for review
- [x] T044 [US1] Monitor CI/Kilo Code; address feedback; resolve review threads per AGENTS
- [x] T045 [US1] Verify human approval and merge of US1 PR before starting US4/US3 story PRs (PR #1449 merged)

---

## Phase 5: User Story 4 — Integrators and extension authors (Priority: P2)

**Goal**: Built-in extensions/toolkit remain correct after rebuild; document source-rebuild requirement; clean remaining non-critical product extension/toolkit JCR deprecations where clear (FR-011, FR-009).  
**Independent Test**: Toolkit and extension modules compile and their tests pass; integrator contract published in feature docs and referenced from release notes draft.  
**Depends on**: US2; preferably US1 for identity patterns.

### Tests (Required)

- [x] T046 [P] [US4] Add/adjust tests for toolkit value/query helpers under `modules/perc-toolkit/src/test/java/` for any JCR 2.0 method or identity changes
- [x] T047 [P] [US4] Add/adjust segmentation-rx tests under `modules/segmentation-rx/src/test/java/` if query construction changes
- [x] T048 [P] [US4] Migrate remaining clear JCR deprecations in `modules/perc-toolkit/src/main/java/` (e.g. relationship builder, PSO query tools, validation) without changing public HTTP contracts
- [x] T049 [P] [US4] Migrate remaining clear JCR deprecations in `modules/segmentation-rx/`, `modules/p13n-api/`, `modules/extensions-main/`, `modules/extensions-nav/`, `modules/extensions-sfp/`, `modules/extensions-workflow/`, `modules/extensions-serverutils/`, `modules/extensions-landingpage/`, `deployer/src/`
- [x] T050 [US4] Ensure [contracts/integrator-rebuild.md](./contracts/integrator-rebuild.md) is accurate vs actual signature changes; update if product public Java types leaked new requirements
- [x] T051 [US4] Update exception register `specs/987-jcr-2-0-api-migration/exceptions.md` for any non-critical leftover integrator-facing sites
- [x] T052 [US4] Run tests for toolkit/segmentation/extensions modules touched; fix failures
- [x] T053 [US4] Commit US4 work and open PR (PR #1452 submitted & merged); pause for review
- [x] T054 [US4] Monitor CI/Kilo Code; address feedback; resolve review threads per AGENTS (PR #1452 review threads resolved)
- [x] T055 [US4] Verify human approval and merge of US4 PR (PR #1452 merged)

---

## Phase 6: User Story 3 — Operators retain security and support posture (Priority: P2)

**Goal**: Document upgrade for ops/support; verify dependency/security posture (FR-007, FR-008, SC-004, SC-005).  
**Independent Test**: Release notes text exists; dependency tree has no jcr 1.0; CVE/dependency review note recorded.  
**Depends on**: US2 (pin present); can parallelize docs draft with US1/US4 but merge after behavior PRs preferred.

### Implementation

- [x] T056 [P] [US3] Draft release-notes / changelog entry covering JCR 2.0 API upgrade, no content data migration, custom extension rebuild requirement (paths: product release-notes location used by the project, or `specs/987-jcr-2-0-api-migration/release-notes-draft.md` if no single CHANGELOG exists)
- [x] T057 [US3] Run `./mvn-env.sh -pl system -am dependency:tree -Dincludes=javax.jcr:jcr` (and broader tree if needed) and record evidence of **2.0 only** in `specs/987-jcr-2-0-api-migration/dependency-tree.txt`
- [x] T058 [US3] Perform/record dependency or vulnerability review note for repository stack (no new unexplained high-severity findings, or document exceptions) in `specs/987-jcr-2-0-api-migration/security-review.md`
- [x] T059 [US3] Cross-link integrator rebuild contract from release notes draft to `specs/987-jcr-2-0-api-migration/contracts/integrator-rebuild.md`
- [x] T060 [US3] Commit docs/evidence and open PR (PR #1452 submitted & merged); pause for review
- [x] T061 [US3] Monitor CI/Kilo Code if code touched; resolve review threads; verify merge (PR #1452 merged)

---

## Phase 7: Polish & Cross-Cutting Concerns (Feature-complete gate)

**Goal**: FR-010 / FR-012 / SC-003 / SC-006 — full automated green + scripted smoke; housekeeping.  
**Depends on**: US1–US4 complete (or US3 docs merged).

- [x] T062 [P] Run Spotless (or project format check) on all touched modules via `./mvn-env.sh` Spotless goals used by the repo
- [x] T063 [P] Update nearest module README or Maven site notes if implementor behavior for unsupported JCR features needs operator/dev visibility (`modules/utils` and/or `system` docs as applicable)
- [x] T064 Finalize `specs/987-jcr-2-0-api-migration/exceptions.md` (empty list preferred; no critical-path entries)
- [x] T065 Run full designated automated suite: `./mvn-env.sh -pl modules/utils,system,projects/sitemanage,modules/perc-toolkit -am test` (expand if CI requires)
- [x] T066 Execute **scripted smoke** per [quickstart.md](./quickstart.md) (create/save, open, preview, one publish); record results on final PR comment or `specs/987-jcr-2-0-api-migration/smoke-results.md`
- [x] T067 Open/update final feature-complete PR (or confirm last story PR includes smoke evidence); monitor checks; resolve threads
- [x] T068 Verify issue #506 can be closed or updated with remaining tracked exceptions only
- [x] T069 Confirm feature checklist readiness: compile 2.0-only, deprecation inventory done, docs published, smoke recorded

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational (inventory + BOM confirm)
    ↓
Phase 3 US2 Compile-clean PR  ←── MVP (FR-014 / SC-008)
    ↓
Phase 4 US1 Critical deprecation + behavior tests
    ↓
    ├─→ Phase 5 US4 Integrators/extensions  ⎤ may partially parallel after US1 patterns exist
    └─→ Phase 6 US3 Ops docs/security        ⎦ docs draft can start earlier but merge after US2
    ↓
Phase 7 Polish + scripted smoke (feature-complete)
```

|    Phase     |    Depends on    |                        Blocks                         |
|--------------|------------------|-------------------------------------------------------|
| Setup        | —                | Foundational                                          |
| Foundational | Setup            | All stories                                           |
| US2 (P1)     | Foundational     | US1 recommended; hard gate for meaningful deprecation |
| US1 (P1)     | US2 merged       | Feature-complete smoke quality                        |
| US4 (P2)     | US2; ideally US1 | —                                                     |
| US3 (P2)     | US2              | —                                                     |
| Polish       | US1–US4          | Feature done                                          |

**Story completion order (recommended)**: US2 → US1 → US4 → US3 → Polish  
(US3 docs can be drafted in parallel with US1/US4.)

---

## Parallel Execution Examples

### During US2 (after T015 tests sketched)

```bash
# Parallel implementor workstreams (different files):
# A: utils Value/ValueFactory/PropertyDefinition
# B: system PSProperty + PSMockProperty
# C: toolkit *Value models
# Then serialize: PSContentNode → Query stack → NodeType → full compile
```

### During US1

```bash
# Parallel after inventory T036:
# - system/services critical getUUID migrations
# - projects/sitemanage finder/path migrations
```

### During US4

```bash
# Parallel module clusters:
# - modules/perc-toolkit
# - modules/segmentation-rx + p13n-api
# - modules/extensions-* + deployer
```

---

## Implementation Strategy

- **MVP first**: Setup → Foundational → **US2 compile-clean PR** → validate BUILD SUCCESS. This unblocks the already-landed jcr 2.0 dependency.
- **Incremental delivery**: US1 critical deprecation PR → US4 secondary modules → US3 docs/security → Polish with smoke.
- **PR discipline** (constitution): one story (or plan phase) per PR; monitor checks; resolve review threads with mitigation replies before next story.
- **Do not**: bulk-replace `getUUID`, rewrite to JCR-SQL2, or implement full optional JSR-283 features.
- **Tests**: Required for behavioral changes (constitution / AGENTS); every implementor and migration story includes test tasks.

---

## Task Summary

|          Phase           | Story |   Task IDs    | Count  |
|--------------------------|-------|---------------|--------|
| 1 Setup                  | —     | T001–T004     | 4      |
| 2 Foundational           | —     | T005–T010     | 6      |
| 3 US2 Build (MVP)        | US2   | T011–T032     | 22     |
| 4 US1 Editors/Publishers | US1   | T033–T045     | 13     |
| 5 US4 Integrators        | US4   | T046–T055     | 10     |
| 6 US3 Operators          | US3   | T056–T061     | 6      |
| 7 Polish                 | —     | T062–T069     | 8      |
| **Total**                |       | **T001–T069** | **69** |

### Per user story

| Story | Tasks |               Independent test                |
|-------|-------|-----------------------------------------------|
| US2   | 22    | Full compile + jcr 2.0 tree + compile-only PR |
| US1   | 13    | Critical-path deprecation + module tests      |
| US4   | 10    | Toolkit/extension tests + rebuild contract    |
| US3   | 6     | Release notes + dependency/security evidence  |

### MVP scope

**US2 only** (T001–T032 path through merge): product compiles on jcr 2.0 and ships the Phase-1 PR.

### Format validation

All tasks use `- [ ]`, sequential IDs `T001`+, optional `[P]`, story labels `[US1]`–`[US4]` only on story phases, and include concrete file/path or command targets.
