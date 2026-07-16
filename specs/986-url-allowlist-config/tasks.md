# Tasks: Configurable Allowed and Blocked URL Lists

**Input**: Design documents from `/specs/986-url-allowlist-config/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅  
**Branch**: `986-url-allowlist-config` | **Issue**: #1205

**Tests**: REQUIRED for every behavioral change (Constitution III + FR-013). Prefer fail-then-pass where practical.

**Organization**: Setup → Foundational (shared engine) → User stories US1–US4 (each independently testable) → Polish. Per constitution Development Workflow: implement, commit, and open a PR per story when practical; at minimum open PRs at story boundaries.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no incomplete dependencies)
- **[Story]**: US1–US4 only on story-phase tasks
- Every task includes a concrete path under the mono-repo

## Path conventions

- Security engine: `modules/perc-security-utils/src/main/java/com/percussion/security/validation/`
- Security tests: `modules/perc-security-utils/src/test/java/com/percussion/security/validation/`
- Default templates (source for install): place under the distribution config tree used by `modules/perc-distribution-tree` for `rxconfig/Server` (see `installDistributionFiles.xml` / `${configdir}`)
- Installer: `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`
- Build/test: `./mvn-env.sh -pl modules/perc-security-utils -am test`

---

## Phase 1: Setup

**Purpose**: Confirm environment and baselines before code changes.

- [x] T001 Read root `AGENTS.md` and confirm no `modules/perc-security-utils/AGENTS.md` override; note JDK 21 via `./mvn-env.sh --version`
- [x] T002 [P] Run baseline `./mvn-env.sh -pl modules/perc-security-utils -am test -Dtest=URLValidationTest` and record pass
- [x] T003 [P] Inventory current hard-coded blocks and system-property knobs in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLValidation.java` and `URLValidationConfig.java` (list for migration into defaults)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared URL list engine and decision order required by all stories. **Blocks US1–US4.**

**⚠️ CRITICAL**: Complete this phase before story work.

- [x] T004 Create `URLGlobMatcher` (normalize absolute URL + full-string glob `*`) in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLGlobMatcher.java` per `contracts/url-validation-decision.md` and research R4
- [x] T005 [P] Create unit tests for normalize/glob match (including path mismatch, case of host/scheme) in `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLGlobMatcherTest.java`
- [x] T006 Create `URLListFileLoader` (parse lines, `#` comments, ignore blank and lone `*`, load allow/block lists; seed-if-missing API taking `Path`) in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLListFileLoader.java` per `contracts/url-list-files.md`
- [x] T007 [P] Create unit tests for parser + seed-if-missing / no-overwrite in `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLListFileLoaderTest.java` using temp directories
- [x] T008 Add classpath default templates `allowedUrls.properties` (comments + inactive examples only) and `blockedUrls.properties` (active dangerous targets) under `modules/perc-security-utils/src/main/resources/com/percussion/security/validation/` for seed content
- [x] T009 Refactor `URLValidationConfig` in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLValidationConfig.java`: hold allow/block pattern lists; **remove** `loadFromProperties()` system-property path and related host/port/range private-network property fields used only by those props (FR-007a)
- [x] T010 Wire load from `{rxdeploydir}/rxconfig/Server/{allowed,blocked}Urls.properties` (factory/static load using property `rxdeploydir` only—no dependency on `utils`) and optional `URLValidationConfig.fromFiles(Path, Path)` for tests
- [x] T011 Implement decision order in `URLValidation.validateURL` in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/URLValidation.java`: scheme → hard metadata deny → block globs → baseline (loopback; public 80/443) → allow globs → deny (FR-006)
- [x] T012 Update messages that reference removed `percussion.url.validation.*` system properties in `URLValidation.java` / `URLValidationConfig.java` to point at allow/block files instead
- [x] T013 Extend `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLValidationTest.java` for baseline public permit, private deny without allow, and hard metadata deny still green after refactor

**Checkpoint**: Engine loads lists, matches globs, decision order works; system properties gone; unit tests pass.

---

## Phase 3: User Story 1 - Preserve legitimate external lookups after upgrade (Priority: P1)

**Goal**: Additive allow patterns permit integrations that would fail baseline alone (internal hosts, custom ports, specific external hosts), without exclusive allowlist breakage.

**Independent Test**: With temp allow file containing three active patterns (private host, custom-port external, path-scoped external), validation permits matching URLs and still permits baseline public `https://example.com/` with empty allow; unmatched non-baseline URL denied.

### Tests for User Story 1

- [x] T014 [P] [US1] Add nested tests for additive allow + private unlock + custom port in `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLValidationTest.java` (or `URLValidationAllowListTest.java`)

### Implementation for User Story 1

- [x] T015 [US1] Ensure allow-list match path in `URLValidation.java` permits private-range and nonstandard-port URLs when pattern matches (FR-006a); cover via T014
- [x] T016 [US1] Confirm empty allow file still permits baseline public standard-port URLs in tests (US1 scenario 5)
- [x] T017 [US1] Run `./mvn-env.sh -pl modules/perc-security-utils -am test` for allow-list suites
- [x] T018 [US1] Commit story-ready changes and open/update PR for US1 scope; pause for review before treating US1 done
- [x] T019 [US1] Address review feedback; reply inline + `resolveReviewThread` per AGENTS.md; verify merge or human approval gate before next story if stacking

**Checkpoint**: US1 acceptance scenarios 1–5 covered by automated tests.

---

## Phase 4: User Story 2 - Keep dangerous targets blocked by default (Priority: P1)

**Goal**: Default blocked list + hard deny keep SSRF-critical destinations blocked; block wins over allow and baseline.

**Independent Test**: Default/seeded block patterns deny metadata URLs; allow pattern for metadata still denied when block matches; hard deny remains if block file empty of metadata lines.

### Tests for User Story 2

- [x] T020 [P] [US2] Add block-precedence and default-block tests in `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLValidationTest.java` (or `URLValidationBlockListTest.java`)

### Implementation for User Story 2

- [x] T021 [US2] Finalize active default entries in classpath `blockedUrls.properties` under `modules/perc-security-utils/src/main/resources/com/percussion/security/validation/blockedUrls.properties` from inventory T003
- [x] T022 [US2] Keep defense-in-depth hard-coded metadata/reserved host deny in `URLValidation.java` even when block file emptied (research R8)
- [x] T023 [US2] Implement block-wins-over-allow in decision order (already T011; assert with allow+block same URL in T020)
- [x] T024 [US2] Run `./mvn-env.sh -pl modules/perc-security-utils -am test`
- [x] T025 [US2] Commit/PR gate for US2; resolve review threads before continuing

**Checkpoint**: SC-002 style denials pass; block-over-allow proven.

---

## Phase 5: User Story 3 - Administrators manage URL policy via install-root files (Priority: P1)

**Goal**: Files exist under install-root `rxconfig/Server/`, wildcards documented, operator docs describe configuration (not JVM props).

**Independent Test**: Distribution/installer sources include both property files; install copy uses never-overwrite; docs mention paths, globs, additive allow, no system properties.

### Tests for User Story 3

- [x] T026 [P] [US3] Add full-URL glob documentation cases (path mismatch fails match) if not already covered in `URLGlobMatcherTest.java`

### Implementation for User Story 3

- [x] T027 [US3] Ship default templates into distribution config source for Server (same tree as `server.properties`) under `modules/perc-distribution-tree` configdir / `src/main/resources/distribution/rxconfig/Server/allowedUrls.properties` and `blockedUrls.properties` (align content with classpath defaults)
- [x] T028 [US3] Register both files in `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` Server config `PSCopy` with `replaceType="never"` (or equivalent never-overwrite)
- [x] T029 [US3] Optional: ensure CMS server init after Rx dir known reloads/sets `URLValidationConfig` in appropriate `system/` startup path if auto-load via `rxdeploydir` is insufficient (only if T010 alone fails integration smoke)
- [x] T030 [US3] Update release notes for 8.2 (project release-notes location under `docs/` or established release process) covering FR-010 topics
- [x] T031 [US3] Update administrator/end-user help content for allow/block URL configuration (common weather/HR/i18n examples) per FR-011
- [x] T032 [US3] Commit/PR gate for packaging + docs; resolve review threads

**Checkpoint**: Files packaged; install never overwrites; docs discoverable.

---

## Phase 6: User Story 4 - Safe upgrade seeding of configuration (Priority: P1)

**Goal**: Missing files created with correct defaults; existing files never overwritten; partial missing only creates absent file.

**Independent Test**: `URLListFileLoader` seed tests: missing both → create both; existing allow with custom content → unchanged; only block missing → only block created.

### Tests for User Story 4

- [x] T033 [P] [US4] Expand seed tests in `modules/perc-security-utils/src/test/java/com/percussion/security/validation/URLListFileLoaderTest.java` for partial missing and content-preservation (FR-008/FR-009, SC-001)

### Implementation for User Story 4

- [x] T034 [US4] Ensure seed writes allow template with **no active** allows and block template with **active** dangerous targets (FR-003/FR-003a)
- [x] T035 [US4] Invoke seed on first config load when install-root Server dir is resolvable (`URLValidationConfig` / loader) without overwriting
- [x] T036 [US4] Run full `./mvn-env.sh -pl modules/perc-security-utils -am test`
- [x] T037 [US4] Commit/PR gate for US4 seeding behavior

**Checkpoint**: SC-001 seed behaviors automated.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T038 [P] Run consumer SSRF regression: `./mvn-env.sh -pl modules/extensions-main -Dtest=PSProxyQueryResourceTest test` and `./mvn-env.sh -pl system -Dtest=PSDocumentUtilsSsrfTest,PSDtdTreeSsrfTest test`; fix any breakage from decision-order changes
- [x] T039 [P] Grep for removed system property names under `modules/perc-security-utils` and docs; eliminate stale references
- [x] T040 Spotless/format on touched modules via project standard (`./mvn-env.sh -pl modules/perc-security-utils,modules/perc-distribution-tree spotless:apply` or equivalent)
- [x] T041 Security review pass: confirm secrets not logged; block wins; lone `*` ignored; metadata hard deny
- [x] T042 Update `specs/986-url-allowlist-config/quickstart.md` with final test class names if they differ from placeholders
- [x] T043 Final PR: link issue #1205; ensure all review threads resolved; green checks before merge

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: none
- **Phase 2 (Foundational)**: after Setup — **blocks all stories**
- **Phase 3 (US1)**: after Foundational
- **Phase 4 (US2)**: after Foundational (can parallelize with US1 after T011 if careful; prefer sequential for single engine PR stack)
- **Phase 5 (US3)**: after Foundational; best after US1/US2 defaults finalized (T021/T008)
- **Phase 6 (US4)**: after loader seed API (T006–T008) and default templates (T008/T021)
- **Phase 7 (Polish)**: after US1–US4 desired scope complete

### User story dependencies

| Story | Depends on | Notes |
|-------|------------|--------|
| US1 Allow lookups | Foundational | Additive allow + private unlock |
| US2 Block defaults | Foundational | Block patterns + hard deny |
| US3 Admin files/docs | Foundational + default file content | Packaging + docs |
| US4 Upgrade seed | Foundational loader/seed | Create-if-absent |

### Within each story

1. Tests first (or alongside)  
2. Implementation  
3. Module test run  
4. Commit / PR / review resolution  

### Parallel opportunities

```text
# After Foundational T011:
Parallel: US1 allow tests (T014) || US2 block tests (T020)
Parallel: US3 packaging (T027-T028) || US3 docs (T030-T031) once templates exist
Parallel: T038 consumer SSRF || T039 grep cleanup
```

---

## Parallel Example: Foundational

```bash
# After T004 skeleton exists:
Task: "T005 URLGlobMatcherTest"
Task: "T006 URLListFileLoader"   # different files from matcher tests once API stable
```

## Parallel Example: US3

```bash
Task: "T027 distribution templates"
Task: "T030 release notes"
Task: "T031 admin help"
```

---

## Implementation Strategy

### MVP (minimum shippable)

1. Phase 1 Setup  
2. Phase 2 Foundational (matcher, loader, wire decision order, remove system props)  
3. Phase 3 US1 (additive allow + private unlock proven)  
4. Phase 4 US2 (block defaults + precedence)  
5. Minimal packaging seed (T027–T028 + T034–T035) even if full docs follow  

### Incremental delivery

1. MVP → validate with `URLValidationTest`  
2. US3 docs/packaging polish  
3. US4 seed edge cases  
4. Polish + consumer SSRF  

### Story checkpoint (constitution)

After each story phase: commit, open/update PR, wait for Kilo/review, resolve threads, merge (or explicit human go-ahead) before next story when stacking is required.

---

## Task completeness validation

| Story | Has tests? | Has implement tasks? | Independent test stated? |
|-------|------------|----------------------|---------------------------|
| US1 | T014 | T015–T016 | Yes |
| US2 | T020 | T021–T023 | Yes |
| US3 | T026 | T027–T031 | Yes |
| US4 | T033 | T034–T035 | Yes |

| Check | Status |
|-------|--------|
| All tasks use `- [ ] Tnnn ...` with paths | Yes |
| Foundational blocks stories | Yes |
| FR-013 coverage mapped | Yes |
| Contracts mapped (files + decision) | T004–T011, T027–T028 |

---

## Summary

| Metric | Value |
|--------|--------|
| **Total tasks** | 43 (T001–T043) |
| **US1** | 6 (T014–T019) |
| **US2** | 6 (T020–T025) |
| **US3** | 7 (T026–T032) |
| **US4** | 5 (T033–T037) |
| **Setup + Foundational + Polish** | 3 + 10 + 6 |
| **Parallelizable markers** | T002–T003, T005, T007, T014, T020, T026, T033, T038–T039 |
| **MVP** | Setup + Foundational + US1 + US2 + minimal seed packaging |

## Format validation

All tasks use: checkbox + sequential ID + optional `[P]` + story label only in US phases + description with concrete file path.
