# Tasks: Unified Publishing UI

**Input**: Design documents from `/specs/990-unified-publishing-ui/`  
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [research/inventory.md](./research/inventory.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required by FR-017 (Vitest for non-trivial client logic; JUnit for any new Java REST façade). Mark capability matrix rows Done at each cutover.

**Organization**: Phases by user story. Prefer **one PR per story** (US1 → US8) per constitution story checkpoint. **Cutover policy** (spec Assumptions / research R5):

| Milestone | After stories | Action |
|-----------|---------------|--------|
| Ops primary path | US1–US3 | Rewire `views.put("publish", "publishModern.jsp")`; dual-path Minuet files may still exist until US8 |
| Design cutover | US4 + UAT | Map `/ui/publishing/*`; remove Design JSF from product path in US8 design portion |
| Runtime cutover | US5 + UAT | Map `/ui/pubruntime/*`; remove Runtime JSF in US8 |
| Full retirement | US8 | Delete exclusive classic clients; sign off [checklists/removal-inventory.md](./checklists/removal-inventory.md) |

## Format

`- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: parallelizable (different files; no wait on incomplete sibling work)
- **[USn]**: user-story phase only

---

## Phase 1: Setup

**Purpose**: Orient modules, toolchain, and feature docs.

- [X] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/AGENTS.md`
- [X] T002 Confirm JDK 21 branch baseline and that WebUI modern tests can run via `./mvn-env.sh -pl WebUI` and/or `WebUI/src/main/frontend` npm/vitest per module docs
- [X] T003 [P] Confirm feature docs present under `specs/990-unified-publishing-ui/` (`spec.md`, `plan.md`, `research.md`, `research/inventory.md`, `data-model.md`, `contracts/*`, `quickstart.md`)
- [X] T004 [P] Confirm checklist scaffolds exist: `specs/990-unified-publishing-ui/checklists/removal-inventory.md` and `checklists/i18n-key-checklist.md`
- [X] T005 [P] Skim contracts: `specs/990-unified-publishing-ui/contracts/ops-publish-api.md`, `design-runtime-api.md`, `deep-links.md`, `capability-matrix.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared shell mount, ops API clients, types, i18n. **Blocks US1–US8.**

- [X] T006 Document ops REST path inventory from `WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_path_constants.js` and `PercPublisherService.js` into a short note under `specs/990-unified-publishing-ui/research/` (or extend `research.md`) covering publish, incremental, status, logs, servers—**hard gate before US1 API coding**
- [X] T007 [P] Add TypeScript DTO/types for publishing ops aligned to `data-model.md` in `WebUI/src/main/ts/publishing/types.ts` (and/or `WebUI/src/main/ts/api/publishing/types.ts`)
- [X] T008 [P] Add typed ops API client modules under `WebUI/src/main/ts/api/publishing/` (e.g. `publishApi.ts`, `statusApi.ts`, `serversApi.ts`) using `WebUI/src/main/ts/api/client.ts` + CSRF helpers; match `contracts/ops-publish-api.md`
- [X] T009 Reuse or extend thin i18n helper under `WebUI/src/main/ts/i18n/` (delegate to `I18N.message`) for Publishing chrome (FR-012)
- [X] T010 Scaffold `PublishingShell` placeholder and section router stub in `WebUI/src/main/ts/publishing/PublishingShell.tsx` + `WebUI/src/main/ts/publishing/sections/` (empty Sites/Status/Logs/Design/Runtime placeholders)
- [X] T011 Register `PublishingShell` in `WebUI/src/main/ts/registry.ts` and ensure export from `WebUI/src/main/ts/index.ts` so Vite modern bundle includes it
- [X] T012 [P] Implement section/query allowlist helper in `WebUI/src/main/ts/publishing/deepLinkMap.ts` per `contracts/deep-links.md` (`section`, `siteId`, `serverId`)
- [X] T013 Create thin shell JSP `WebUI/src/main/webapp/cm/app/publishModern.jsp` (pattern from `homeModern.jsp`: CSRF, `modern_shell_head.jsp`, `tmx.jsp` session locale, `/cm/modern/assets/perc-modern-ui.js`, `PercModernUI.mount('perc-publishing-root', 'PublishingShell', props)`)
- [X] T014 Mirror shell JSP to `WebUI/src/main/webapp/cm/pages/app/publishModern.jsp`
- [X] T015 Assess security surface (CSRF on mutations, AuthZ errors, secrets never logged—FR-016); record brief note in `specs/990-unified-publishing-ui/research.md` or plan Complexity Tracking if new risks found
- [X] T016 [P] Add Vitest scaffold path `WebUI/src/test/ts/publishing/` with at least one smoke test that `PublishingShell` mounts / deepLinkMap maps known sections

**Checkpoint**: `PublishingShell` mounts from `publishModern.jsp` when forced via view map or direct test; ops API modules compile; **do not** rewire production `views.put("publish")` until US3 cutover task.

---

## Phase 3: User Story 1 — Sites & run publish (Priority: P1)

**Goal**: Site list (card/list/filter), site workspace, full + incremental publish, incremental preview queue, stop where applicable (CG-OPS site/run rows).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario A (steps 1–5 partial) + B; mark OPS-01–04, OPS-16–19, OPS-26 in progress/Done on capability matrix as applicable.

### Tests (Required)

- [X] T017 [P] [US1] Unit tests for site filter/view toggle helpers in `WebUI/src/test/ts/publishing/siteListUtils.test.ts` (or colocated)
- [X] T018 [P] [US1] Tests for publish action state machine / error mapping (FORBIDDEN, BADCONFIG) in `WebUI/src/test/ts/publishing/publishActions.test.ts`
- [X] T019 [P] [US1] Tests for incremental queue empty vs paged response handling in `WebUI/src/test/ts/publishing/incrementalQueue.test.ts`

### Implementation

- [X] T020 [P] [US1] Implement Sites section UI (card + list + filter) in `WebUI/src/main/ts/publishing/sections/SitesSection.tsx` (and subcomponents under `WebUI/src/main/ts/publishing/components/`) using existing site list APIs / patterns from Minuet `PercPublishMinuetView.js`
- [X] T021 [P] [US1] Implement site workspace shell (back to sites, server summary area placeholder for US3) in `WebUI/src/main/ts/publishing/sections/SiteWorkspace.tsx`
- [X] T022 [US1] Wire full publish action calling `api/publishing/publishApi.ts` in site workspace; show success/error toasts/messages via i18n keys
- [X] T023 [US1] Implement incremental preview queue + related items UI and incremental publish (incl. approval path if product requires) under `WebUI/src/main/ts/publishing/sections/` / `components/`
- [X] T024 [US1] Wire stop-job action for stoppable jobs from site context using stop publishing API in `WebUI/src/main/ts/api/publishing/`
- [X] T025 [US1] Add/reuse TMX keys for US1 chrome in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`; record on `specs/990-unified-publishing-ui/checklists/i18n-key-checklist.md`
- [X] T026 [US1] Update OPS-* rows for US1 in `specs/990-unified-publishing-ui/contracts/capability-matrix.md`
- [X] T027 [US1] Run Vitest under `WebUI/src/test/ts/publishing/` and fix failures for US1
- [X] T028 [US1] Commit US1 changes and open PR; pause downstream per constitution; do not claim full ops cutover yet
- [X] T029 [US1] Address review/CI feedback and resolve review threads before US2

**Checkpoint**: Sites + full/incremental publish work in modern shell when `publishModern.jsp` is loaded; classic Minuet still default nav target.

---

## Phase 4: User Story 2 — Live status & publishing logs (Priority: P1)

**Goal**: Status table with refresh/progress/stop; logs filter/list/details/purge (CG-OPS status/logs).  
**Independent Test**: quickstart Scenario A (status + logs + purge); mark OPS-20–24.

### Tests (Required)

- [X] T030 [P] [US2] Tests for status poll lifecycle (start/stop interval, job id change) in `WebUI/src/test/ts/publishing/statusPolling.test.ts`
- [X] T031 [P] [US2] Tests for progress percentage helper (completed/total edge cases) in `WebUI/src/test/ts/publishing/progressUtils.test.ts`
- [X] T032 [P] [US2] Tests for purge confirmation gate (no selection vs confirm) in `WebUI/src/test/ts/publishing/logsPurge.test.ts`

### Implementation

- [X] T033 [P] [US2] Implement Status section table (sortable columns parity with Minuet) in `WebUI/src/main/ts/publishing/sections/StatusSection.tsx` using `statusApi.ts`
- [X] T034 [US2] Implement auto-refresh polling no worse than Minuet interval in Status section / hooks under `WebUI/src/main/ts/publishing/`
- [X] T035 [P] [US2] Implement Logs section filters + list in `WebUI/src/main/ts/publishing/sections/LogsSection.tsx`
- [X] T036 [US2] Implement log details panel/drawer (job items) and purge with confirmation dialog under `WebUI/src/main/ts/publishing/components/`
- [X] T037 [US2] Wire stop job from Status section to existing stop API in `WebUI/src/main/ts/api/publishing/`
- [X] T038 [US2] Add/reuse TMX keys for status/logs; update `checklists/i18n-key-checklist.md` and `CmsUi.tmx`
- [X] T039 [US2] Update capability matrix OPS-20–24 in `contracts/capability-matrix.md`
- [X] T040 [US2] Run Vitest for status/logs; fix failures under `WebUI/src/test/ts/publishing/`
- [X] T041 [US2] Commit US2 + open PR; resolve review threads before US3

**Checkpoint**: Status and Logs usable in modern shell independently of Design/Runtime.

---

## Phase 5: User Story 3 — Publish server configuration (Priority: P1)

**Goal**: Server CRUD, default Publish Now, Production/Staging, File/Database drivers (Local, FTP, FTPS, SFTP, S3, DB variants), env helpers (CG-OPS server rows). **Ops primary nav cutover** at end of this story.  
**Independent Test**: quickstart Scenario C; mark OPS-05–15, OPS-25; rewire nav so Publish opens modern shell.

### Tests (Required)

- [X] T042 [P] [US3] Unit tests for driver validation matrix (required fields per Local/FTP/S3/DB) in `WebUI/src/test/ts/publishing/serverValidation.test.ts`
- [X] T043 [P] [US3] Tests ensuring password/privateKey fields are not included in debug/error serialization helpers in `WebUI/src/test/ts/publishing/serverSecrets.test.ts` (FR-016)
- [X] T044 [P] [US3] Component tests for server list empty/default indicator in `WebUI/src/test/ts/publishing/ServerList.test.tsx`

### Implementation

- [X] T045 [P] [US3] Implement server list UI (add/refresh/default star) in `WebUI/src/main/ts/publishing/components/ServerList.tsx` using `serversApi.ts`
- [X] T046 [US3] Implement server editor form shell (type File/Database, Production/Staging) in `WebUI/src/main/ts/publishing/components/ServerEditor.tsx`
- [X] T047 [P] [US3] Implement File driver property panels (Local, FTP, FTPS, SFTP, AMAZONS3) under `WebUI/src/main/ts/publishing/components/drivers/` porting fields from `minuetPublishTemplates/publishTemplates.jsp`
- [X] T048 [P] [US3] Implement Database driver property panels (common + MSSQL/MySQL/Oracle) under `WebUI/src/main/ts/publishing/components/drivers/`
- [X] T049 [US3] Wire create/update/delete server + default folder/availableDrivers/regions/EC2 helpers via `WebUI/src/main/ts/api/publishing/serversApi.ts`
- [X] T050 [US3] Dirty-form guard on section navigation (confirm discard) in `PublishingShell.tsx` / server editor
- [X] T051 [US3] Add/reuse TMX keys for servers; update i18n checklist + `CmsUi.tmx`
- [X] T052 [US3] Rewire `views.put("publish", "publishModern.jsp")` in `WebUI/src/main/webapp/cm/app/index.jsp` and `cm/pages/app/index.jsp` (ops primary path)
- [X] T053 [US3] Pass allowlisted query props (`section`, `siteId`, `serverId`) from `publishModern.jsp` into `PercModernUI.mount` props
- [X] T054 [US3] Mark OPS-05–15, OPS-25 Done (or gaps explicit) on `contracts/capability-matrix.md`; note Minuet exclusive delete still US8
- [X] T055 [US3] Run Vitest for server config; fix failures
- [X] T056 [US3] Commit US3 + open PR; UAT ops quickstart A–C; resolve review threads before US4

**Checkpoint**: Main nav **Publish** opens unified modern ops UI (sites, servers, status, logs). Minuet `publish.jsp` no longer primary; files may remain until US8.

---

## Phase 6: User Story 4 — Publishing design infrastructure (Priority: P2)

**Goal**: Design section parity for sites/editions/content lists/contexts/schemes/delivery types (CG-DESIGN); thin JSON façade where needed.  
**Independent Test**: quickstart Scenario D; mark DES-01–13; façade JUnit green.

### Tests (Required)

- [X] T057 [P] [US4] JUnit tests for design façade happy path + 404/400 for at least editions and content lists under `projects/sitemanage/src/test/java/com/percussion/...` (package matches implementation)
- [X] T058 [P] [US4] Vitest for design section navigation / selection in `WebUI/src/test/ts/publishing/designNavigation.test.tsx`
- [X] T059 [P] [US4] Vitest for legacy vs modern content list / scheme type handling in `WebUI/src/test/ts/publishing/designLegacyTypes.test.ts`

### Implementation

- [X] T060 [US4] Complete API gap analysis: map each DES-* row to `IPSPublishingWs` / `IPSSiteManager` / `IPSPublisherService` methods; update `contracts/design-runtime-api.md` with chosen base path and final resource list
- [X] T061 [US4] Implement thin design REST façade (DTOs + resource) under `projects/sitemanage/src/main/java/com/percussion/...` wrapping existing services only (no engine reimplementation)—package name per research R3
- [X] T062 [P] [US4] Add typed design API client under `WebUI/src/main/ts/api/publishing/designApi.ts`
- [X] T063 [P] [US4] Implement Design section IA (tree/nav: sites, editions, content lists, contexts, delivery types) in `WebUI/src/main/ts/publishing/sections/DesignSection.tsx`
- [X] T064 [US4] Implement design Site editor + context variables UI under `WebUI/src/main/ts/publishing/design/`
- [X] T065 [US4] Implement Edition list/editor + content list association + copy-from-other-site flow under `WebUI/src/main/ts/publishing/design/`
- [X] T066 [US4] Implement Content list modern + legacy editors under `WebUI/src/main/ts/publishing/design/`
- [X] T067 [US4] Implement Context + Location scheme modern/legacy (+ parameter editor) under `WebUI/src/main/ts/publishing/design/`
- [X] T068 [US4] Implement Delivery type list/editor under `WebUI/src/main/ts/publishing/design/`
- [X] T069 [US4] Implement site root / item browser for schemes under `WebUI/src/main/ts/publishing/design/`
- [X] T070 [US4] Delete confirmations and dependency error display for design objects under `WebUI/src/main/ts/publishing/design/`
- [X] T071 [US4] Add Design TMX keys to `CmsUi.tmx`; update i18n checklist
- [X] T072 [US4] Update DES-* rows on `contracts/capability-matrix.md`
- [X] T073 [US4] Run JUnit façade tests + Vitest design tests; fix failures
- [X] T074 [US4] Commit US4 + open PR; UAT Scenario D; resolve review threads before US5

**Checkpoint**: Design tasks completable without JSF Design UI on feature branch (JSF may still be requestable until US8).

---

## Phase 7: User Story 5 — Runtime edition control & demand publish (Priority: P2)

**Goal**: Runtime section: edition list, start/stop, demand publish, advanced cleanup if still product-supported (CG-RUNTIME).  
**Independent Test**: quickstart Scenario E; mark RT-01–08.

### Tests (Required)

- [X] T075 [P] [US5] JUnit tests for runtime start/stop/demand façade methods under `projects/sitemanage/src/test/java/com/percussion/...` (or reuse ops stop if delegated)
- [X] T076 [P] [US5] Vitest for edition start/stop UI state in `WebUI/src/test/ts/publishing/runtimeEditions.test.tsx`

### Implementation

- [X] T077 [P] [US5] Extend design/runtime façade with runtime endpoints per `contracts/design-runtime-api.md` in sitemanage (delegate to `startPublishingJob`, cancel/stop, `queueDemandWork`, `deleteSiteItems` as needed)
- [X] T078 [P] [US5] Add typed runtime API client in `WebUI/src/main/ts/api/publishing/runtimeApi.ts` (reuse `statusApi`/`serversApi` stop where applicable)
- [X] T079 [US5] Implement Runtime section edition list + start/stop in `WebUI/src/main/ts/publishing/sections/RuntimeSection.tsx`
- [X] T080 [US5] Implement demand publish UI under `WebUI/src/main/ts/publishing/sections/` or `components/`
- [X] T081 [US5] Implement clear site record / advanced log cleanup entries if still product-supported and not fully covered by Logs section, under `WebUI/src/main/ts/publishing/`
- [X] T082 [US5] Add Runtime TMX keys; update i18n checklist + capability matrix RT-*
- [X] T083 [US5] Run JUnit + Vitest for runtime; fix failures
- [X] T084 [US5] Commit US5 + open PR; UAT Scenario E; resolve review threads before US6

**Checkpoint**: Edition-centric ops work without JSF Runtime as primary tool.

---

## Phase 8: User Story 6 — Item-level publish coherence (Priority: P2)

**Goal**: No regression on publish now / takedown / stage / schedule / history; deep links into modern Publishing where applicable (CG-ITEM).  
**Independent Test**: quickstart Scenario F; mark ITM-01–06.

### Tests (Required)

- [X] T085 [P] [US6] API/service smoke or unit tests for item publish action endpoints still reachable (extend existing sitemanage tests or add `WebUI/src/test/ts/publishing/itemPublishPaths.test.ts` documenting path constants)
- [X] T086 [P] [US6] Test that publishing history / status deep-link targets map to modern `section` values in `WebUI/src/test/ts/publishing/deepLinkMap.test.ts`

### Implementation

- [X] T087 [US6] Audit `PercItemPublisherService.js` / path constants callers; document residual jQuery entry points in `specs/990-unified-publishing-ui/research/item-publish-entrypoints.md`
- [X] T088 [US6] Fix any regressions introduced by publish view rewire affecting item actions (nav/view keys) under `WebUI/src/main/webapp/cm/`
- [X] T089 [US6] Ensure Publishing history dialog or successor can open modern Status/Logs section when linking to jobs (minimal glue in `WebUI/src/main/ts/publishing/` and/or legacy dialog bridge)
- [X] T090 [US6] Mark ITM-* on capability matrix; note intentional “remain jQuery” items
- [X] T091 [US6] Commit US6 + open PR; resolve review threads before US7

**Checkpoint**: Item publish-now paths green in smoke; no requirement to fully React-rewrite finder actions in this story.

---

## Phase 9: User Story 7 — Ease of use & IA polish (Priority: P2)

**Goal**: Progressive disclosure, empty states, keyboard access, clear section IA (CG-UX).  
**Independent Test**: Publisher completes full publish without opening Design (SC-003 style manual); keyboard path on primary lists; mark UX-01–04.

### Tests (Required)

- [X] T092 [P] [US7] Component tests for empty states (no sites, no servers, no logs) in `WebUI/src/test/ts/publishing/emptyStates.test.tsx`
- [X] T093 [P] [US7] Tests that default landing section is Sites & servers (not Design) in `WebUI/src/test/ts/publishing/PublishingShell.test.tsx`

### Implementation

- [X] T094 [US7] Polish section nav labels, order, and progressive disclosure in `PublishingShell.tsx` (ops first; Design/Runtime secondary)
- [X] T095 [P] [US7] Empty states with next-action guidance for sites/servers/logs/design lists under `WebUI/src/main/ts/publishing/components/`
- [X] T096 [US7] Keyboard focus order and aria labels on primary tables/actions (site list, publish, stop, purge) under `WebUI/src/main/ts/publishing/`
- [X] T097 [US7] Optional role-aware hiding of Design if product roles allow (document if not available server-side) in `PublishingShell.tsx`
- [X] T098 [US7] Update UX-* matrix rows and i18n keys for empty-state strings
- [X] T099 [US7] Commit US7 + open PR; resolve review threads before US8

**Checkpoint**: Ease-of-use acceptance scenarios for US7 satisfied on modern shell.

---

## Phase 10: User Story 8 — Retire legacy publishing UIs (Priority: P3)

**Goal**: Remove exclusive Minuet publish clients, JSF Design, JSF Runtime from product path; deep links; inventory sign-off (CG-RETIRE).  
**Independent Test**: quickstart Scenario G; RET-01–05 Done; production packaging does not require classic clients for publish/design/runtime tasks.

### Tests (Required)

- [X] T100 [P] [US8] Manual/automated smoke that `view=publish` loads modern shell only (document in PR); optional Vitest/build assertion that exclusive Minuet publish entry is not referenced from `index.jsp`
- [X] T101 [P] [US8] Verify deep-link mapping tests cover classic design/runtime URL intents in `WebUI/src/test/ts/publishing/deepLinkMap.test.ts` and/or server redirect tests if implemented in Java

### Implementation

- [X] T102 [US8] Complete removal inventory table for Minuet exclusive assets in `specs/990-unified-publishing-ui/checklists/removal-inventory.md` (list every path under `cm/app` + `cm/pages/app` + war packaging)
- [X] T103 [US8] Delete or stop packaging Minuet publish exclusive clients: `PercPublishMinuetView.js`, status/logs minuet views, `minuetPublishTemplates/`, classic `publish.jsp` if unused—under `WebUI/src/main/webapp/cm/app/` and `cm/pages/app/` (only after consumer inventory; keep shared services if item flows need them)
- [X] T104 [US8] Remove `perc_publish` packed bundle entry from `WebUI/src/main/frontend/vite.legacy.config.ts` (or equivalent) if no remaining consumers
- [X] T105 [US8] Implement redirects or clear moved messages for `/ui/publishing/*` and `/ui/pubruntime/*` (server mapping or modern unavailable view) per `contracts/deep-links.md`
- [X] T106 [US8] Remove JSF Design pages from product path: `WebUI/src/main/webapp/ui/publishing/` (and faces-config entries as needed) after DES matrix Done
- [X] T107 [US8] Remove JSF Runtime pages from product path: `WebUI/src/main/webapp/ui/pubruntime/` after RT matrix Done
- [X] T108 [US8] Update `WebUI/AGENTS.md` UI layer inventory table to reflect Publishing retirement / React ownership
- [X] T109 [US8] Sign off removal inventory + capability matrix RET-* + i18n checklist milestone rows
- [X] T110 [US8] Commit US8 + open PR; full UAT A–G; resolve review threads; merge only when retirement proof accepted

**Checkpoint**: SC-006 / FR-015 satisfied; single unified Publishing UI is the production path.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Docs, hygiene, security sweep after stories.

- [X] T111 [P] Update nearest WebUI README or feature notes documenting Publishing modern entry (`publishModern.jsp`, section query params) under `WebUI/` or `specs/990-unified-publishing-ui/`
- [X] T112 [P] Spotless / format check on touched Java modules via `./mvn-env.sh` as required by project norms
- [X] T113 Security review: AuthZ on design façade, CSRF on all mutations, no secrets in logs (FR-011, FR-016)—record in PR or `specs/990-unified-publishing-ui/checklists/`
- [X] T114 Cross-platform path review for any new Java file I/O/tests (constitution Cross-Platform rules)
- [X] T115 Final capability matrix audit: all in-scope rows Done or explicitly N/A with reason in `contracts/capability-matrix.md`

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational (blocks all stories)
    ↓
US1 (Sites & publish) ──→ US2 (Status & logs) ──→ US3 (Servers + ops nav rewire)
                                                      ↓
                                              US4 (Design + façade)
                                                      ↓
                                              US5 (Runtime)
                                                      ↓
                                              US6 (Item coherence)
                                                      ↓
                                              US7 (UX polish)
                                                      ↓
                                              US8 (Retire classics)
                                                      ↓
                                              Phase 11 Polish
```

| Story | Depends on | Blocks |
|-------|------------|--------|
| US1 | Foundational | US2 (soft—can parallelize UI files but integrate after US1 PR) |
| US2 | Foundational; prefer after US1 shell | — |
| US3 | US1 site workspace; Foundational APIs | Ops cutover for later UAT |
| US4 | Foundational; ops shell optional | US8 design delete |
| US5 | Design/runtime façade patterns from US4 (or ops stop APIs) | US8 runtime delete |
| US6 | US3 nav rewire | — |
| US7 | US1–US5 UI present | — |
| US8 | US1–US7 parity + matrix | Release retirement claim |
| Polish | US8 preferred | — |

**Story completion order**: US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → Polish.

---

## Parallel Execution Examples

```text
# Foundational (after T006 path inventory):
T007 types || T008 ops API clients || T009 i18n || T012 deepLinkMap

# US1:
T017–T019 tests || T020 SitesSection || T021 SiteWorkspace
# then T022–T024 sequential wire-up

# US3 drivers:
T047 File drivers || T048 Database drivers
# after T046 editor shell

# US4 after façade skeleton (T061):
T064 Site editor || T066 Content lists || T068 Delivery types
# editions associations (T065) after list APIs stable
```

---

## Implementation Strategy

### MVP (first shippable ops value)

1. Phase 1–2 Foundations  
2. **US1** sites + full/incremental publish  
3. **US2** status + logs  
4. **US3** servers + **rewire Publish nav** to modern shell  
5. Validate quickstart A–C and SC-001  

Design/Runtime can follow in a later train; temporary dual existence of JSF Design/Runtime is allowed until US4/US5/US8.

### Incremental delivery

- Each story: tests → implementation → matrix update → PR → review resolve → next story  
- Do not delete classic Design/Runtime until matrix DES/RT Done  
- Do not claim SC-006 until US8 removal inventory signed  

### Suggested first agent slice

Start at **T001–T016** (setup + foundational), then **T017–T029** (US1) as first PR.

---

## Phase 12: User Story 9 — Ops parity residual & packaging (Priority: P2)

**Purpose**: Close post-implement analysis gaps (speckit-analyze E1–E6). Baseline shell (US1–US8) shipped on PR #1370; these tasks restore honest tracking and finish Minuet parity polish.

**Independent Test**: OPS-18/20/22/23 matrix → Done; optional RET-06 packaging PR.

### Tests (Required)

- [x] T116 [P] [US9] Vitest for incremental approval selection → `publishIncrementalWithApproval` call shape in `WebUI/src/test/ts/publishing/incrementalApproval.test.ts`
- [x] T117 [P] [US9] Vitest for status table sort helpers in `WebUI/src/test/ts/publishing/statusSort.test.ts`
- [x] T118 [P] [US9] Vitest for log filter request builder (site/server/days) in `WebUI/src/test/ts/publishing/logsFilter.test.ts`

### Implementation

- [x] T119 [US9] Wire related-items approval UI when queue/product requires it: use `publishIncrementalWithApproval` from `WebUI/src/main/ts/api/publishing/publishApi.ts` in `SiteWorkspace.tsx` / incremental components (OPS-18)
- [x] T120 [US9] Add sortable columns to status table in `WebUI/src/main/ts/publishing/sections/StatusSection.tsx` (OPS-20)
- [x] T121 [US9] Add logs filters (site, server, days/window) in `WebUI/src/main/ts/publishing/sections/LogsSection.tsx` matching Minuet-supported fields (OPS-22)
- [x] T122 [US9] Structured log details panel (job items list, not raw JSON only) under `WebUI/src/main/ts/publishing/components/` (OPS-23)
- [x] T123 [US9] Mark OPS-18, OPS-20, OPS-22, OPS-23 **Done** on `contracts/capability-matrix.md` only after T119–T122 pass tests
- [x] T124 [P] [US9] Packaging follow-up (RET-06): inventory remaining `WebUI/src/main/webapp/ui/publishing/**` and `ui/pubruntime/**` faces pages + `publishing-faces-config.xml` entries; delete or stop packaging exclusive deep pages after consumer check; update `checklists/removal-inventory.md`
- [x] T125 [P] [US9] Document UAT evidence for SC-001 / SC-003 / SC-008 in `specs/990-unified-publishing-ui/checklists/uat-signoff.md` (or PR comment with environment + date)
- [x] T126 [P] [US9] Review FR-020: ensure server/design save errors surface success/failure clearly (toasts/alerts) in `ServerEditor.tsx` / design editors; add test if missing
- [ ] T127 [US9] Commit US9 + open PR (or push to #1370); resolve review threads

**Checkpoint**: Matrix OPS residual closed; packaging residual either Done or explicitly deferred with date owner.

---

## Remediation note (2026-07-19)

`/speckit-analyze` after PR #1370 found **tasks.md fully checked** while **capability matrix still In progress** for OPS-18/20/22/23, plus residual faces packaging. Phase 12 / US9 reopens that work as tracked tasks rather than silently leaving matrix drift.

---

## Task summary

| Phase | Story | Task IDs | Count |
|-------|-------|----------|-------|
| 1 Setup | — | T001–T005 | 5 |
| 2 Foundational | — | T006–T016 | 11 |
| 3 | US1 | T017–T029 | 13 |
| 4 | US2 | T030–T041 | 12 |
| 5 | US3 | T042–T056 | 15 |
| 6 | US4 | T057–T074 | 18 |
| 7 | US5 | T075–T084 | 10 |
| 8 | US6 | T085–T091 | 7 |
| 9 | US7 | T092–T099 | 8 |
| 10 | US8 | T100–T110 | 11 |
| 11 Polish | — | T111–T115 | 5 |
| 12 Residual | US9 | T116–T127 | 12 |
| **Baseline total** | | **T001–T115** | **115** |
| **With residual** | | **T001–T127** | **127** |

| Story | Tasks (approx) | Priority |
|-------|----------------|----------|
| US1 | 13 | P1 MVP |
| US2 | 12 | P1 MVP |
| US3 | 15 | P1 MVP + ops cutover |
| US4 | 18 | P2 |
| US5 | 10 | P2 |
| US6 | 7 | P2 |
| US7 | 8 | P2 |
| US8 | 11 | P3 retirement |
| US9 | 12 | P2 residual parity |

**Format validation**: All tasks use `- [ ]` or `- [X]`, sequential IDs, `[USn]` on story phases only, `[P]` only when parallel-safe, and concrete file paths.
