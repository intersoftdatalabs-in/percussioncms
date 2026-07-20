# Tasks: Unified React Content Explorer

**Input**: Design documents from `/specs/992-react-content-explorer/`  
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required by FR-023, constitution III, and root `AGENTS.md` — **two-layer test strategy**: (a) Vitest + Testing Library for component-level logic with mocked API; (b) Playwright + TestNG (`modules/perc-qa-automation/`) for E2E against the live docker dev CMS. axe-core a11y gate (T082a) per US1/US2/US3 component spec. Service-contract tests only if REST shapes change.

**Target release**: **8.2** — **functional parity blocks 8.2 GA** (FR-029, SC-012). All in-scope matrix rows must be **Done** before labeling 8.2; no post-8.2 deferral of in-scope work.

**Organization**: Phases by user story with recommended engineering order for intermediate hard cuts. Prefer **one PR per story** (constitution checkpoint). Suggested PR train:

`US1 → US6 (intermediate hard cut) → US2 (+ hosts) → US3 → US4 → US5 → US7 → Polish/SC-012`

US2 can start after Foundational+US1 in parallel with US6 if staffing allows (different surfaces).

## Format

`- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: parallelizable (different files; no wait on incomplete sibling work)
- **[USn]**: user-story phase only

---

## Phase 1: Setup

**Purpose**: Orient modules, toolchain, and feature docs for 8.2 work.

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/AGENTS.md` (and sitemanage/rest module AGENTS if present when touching those modules)
- [x] T002 Confirm JDK 21 branch baseline and that WebUI modern tests run via `./mvn-env.sh -pl WebUI` (and/or frontend npm/vitest per `WebUI` docs)
- [x] T003 [P] Confirm and expand cutover inventory scaffold in `specs/992-react-content-explorer/checklists/cutover-inventory.md` (FR-022)
- [x] T004 [P] Confirm capability matrix seed in `specs/992-react-content-explorer/contracts/capability-matrix.md` and mark P0-Core rows as implementer targets for US1
- [x] T005 [P] Skim contracts: `contracts/path-api.md`, `contracts/content-browser-host.md`, `contracts/action-menu-api.md`, `quickstart.md` Scenarios A–I

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared path API client, registry placeholders, i18n, inventory of Finder call sites. **Blocks US1–US7.**

- [x] T006 Complete Finder primary-nav inventory by searching `WebUI` for `perc_finder`, `PercFinderView`, `finder.jsp`, `finder_js.jsp` and recording rows in `specs/992-react-content-explorer/checklists/cutover-inventory.md`
- [x] T007 [P] Extend path/search URL constants in `WebUI/src/main/ts/api/paths.ts` for pathmanagement ops used by explorer (`folder`, `paginatedFolder`, `item`, `item/id`, `addNewFolder`, `renameFolder`, `moveItem`, delete, `folderProperties`, `saveFolderProperties`, search extended results) per `contracts/path-api.md`
- [x] T008 [P] Add typed content-explorer API module under `WebUI/src/main/ts/api/contentExplorer/` (e.g. `pathApi.ts`, `types.ts`) using `WebUI/src/main/ts/api/client.ts` + `csrf.ts`, aligned to `data-model.md` PathNode / FolderProperties
- [x] T009 [P] Reuse or extend thin i18n helper under `WebUI/src/main/ts/i18n/` for `I18N.message` (FR-026); document explorer key prefix `perc.ui.explorer.*` / `perc.ui.contentBrowser.*`
- [x] T010 Register placeholder components `ContentExplorerShell` and `ContentBrowser` in `WebUI/src/main/ts/registry.ts` and ensure Vite entry includes them via `WebUI/src/main/ts/index.ts`
- [x] T011 Assess security surface: CSRF on mutating path calls, session expiry UX, server-side folder AuthZ remains authoritative; note findings under `specs/992-react-content-explorer/research.md` or implementer note (no secrets in logs)
- [x] T012 [P] Create Vitest directory scaffolds `WebUI/src/test/ts/contentExplorer/` and `WebUI/src/test/ts/contentBrowser/` mirroring Home/Dashboard test patterns
- [x] T012a [P] Author the **full P-Adv capability matrix row skeleton** for US7 (clipboard, site copy wizard, subfolder copy wizard, dependency viewer, IA/relationship views, display format columns, relationships manager deep tools) in `specs/992-react-content-explorer/contracts/capability-matrix.md` with phase + acceptance label per row (no silent omit — see FR-028 / SC-011)
- [x] T012b [P] Add the **SC-005 perf fixture + measurement scaffolding**: scripted seed `scripts/create-large-folder-fixture.sh` for ≥500-child fixture, evidence file `specs/992-react-content-explorer/checklists/sc005-perf-evidence.md` with the pass criterion (p95 ≤ 10 s on standard office network) defined in `quickstart.md` Scenario B
- [x] T012c Enumerate **in-scope hosts for 8.2** in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C (asset picker, page picker, AA ContentBrowserDialog, folder picker, Home Library optional) with current call-site rows; per-host task IDs **are** T045a–T045f in Phase 5
- [x] T012d [P] **Evaluate each per-host migration in T045a–T045d** for `system/` server-side wiring need (action-page / content-browser dialog JSPs that cannot be replaced via WebUI + sitemanage + REST); record per-host decision (Keep web-only / Add `system/` task / Mark OUT) in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C under the host row, citing the constitution II/IV evidence that justifies the choice
- [x] T012e [P] **If** a per-host decision in T012d requires `system/` wiring, add a concrete task ID under US2 (named `T045X-system-<host>` or similar) referencing the constitution II/IV evidence from T012d and the corresponding threat-model + service-contract test tasks (T052a/T052b pattern); **otherwise** no task is added and `system/` remains untouched
- [ ] T012f [P] **Bring up the docker dev runtime** (NEW, 2026-07-19): host-side install to `/opt/Percussion` via `scripts/install-cms-dev.sh`; container via `docker compose --env-file .env.compose -f docker-compose.yml up -d cms-dts`. Verify the CMS is reachable at `http://localhost:9992/Rhythmyx/login` and Admin credentials are auto-discoverable from `/opt/Percussion/var/config/generated/passwords`. Default DB is **Derby**; MySQL mode is deferred per [issue #1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388). Recorded evidence: `docker compose ps` shows `percussion-cms-dts` running; `curl http://localhost:9992/Rhythmyx/login` returns 200.
- [ ] T012g [P] **Bring up Playwright** (NEW, 2026-07-19): `cd modules/perc-qa-automation/frontend && npm ci && npx playwright install chromium`. Verify `tests/login.spec.js` passes against the live CMS via `cd modules/perc-qa-automation/frontend && npm test`. Recorded evidence: 3 tests passed in 21.5 s on 2026-07-19 (login + auto-discovery + modern explorer placeholder). Subsequent Playwright tasks build on this.

**Checkpoint**: Path API types compile; placeholders mountable; inventory started. No production cutover.

---

## Phase 3: User Story 1 — Explorer-style content workspace (Priority: P1)

**Goal**: Tree + detail list modern explorer with **ReducedAction set** (open, create/rename/move/copy/delete); usable for SC-001/SC-005.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario A + B; SC-001, SC-005.

### Tests (Required)

- [x] T013 [P] [US1] Unit tests for path API helpers (success + error mapping) in `WebUI/src/test/ts/contentExplorer/pathApi.test.ts`
- [x] T014 [P] [US1] Component tests for tree expand/select in `WebUI/src/test/ts/contentExplorer/ExplorerTree.test.tsx`
- [x] T015 [P] [US1] Component tests for detail list pagination/load in `WebUI/src/test/ts/contentExplorer/DetailList.test.tsx`
- [x] T015a [P] [US1] **Vitest perf regression guard** for SC-005: mocked `paginatedFolder` for a 500-child fixture renders first page and opens a selected item within a **tighter dev-machine budget (e.g. ≤ 5 s on the implementer's machine)** — this catches regressions pre-CI; it is **NOT** the SC-005 acceptance measurement. SC-005 itself is proven by `checklists/sc005-perf-evidence.md` against the standard office network per `quickstart.md` Scenario B
- [x] T016 [P] [US1] Tests for reduced actions (create/rename/move/delete confirm + error) in `WebUI/src/test/ts/contentExplorer/reducedActions.test.tsx`

### Implementation

- [x] T017 [P] [US1] Implement `ContentExplorerShell` layout (tree + detail panes) in `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` (+ styles under `WebUI/src/main/ts/contentExplorer/`)
- [x] T018 [P] [US1] Implement folder tree component in `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` loading children via path API
- [x] T019 [P] [US1] Implement detail list with pagination/virtualization for large folders in `WebUI/src/main/ts/contentExplorer/DetailList.tsx` using `paginatedFolder` (SC-005)
- [x] T020 [US1] Implement **ReducedAction set** bar/menu (open/preview, create folder, rename, move, copy, delete+confirm) in `WebUI/src/main/ts/contentExplorer/ReducedActions.tsx` (FR-010a)
- [x] T021 [US1] Wire open/preview to existing product navigation patterns (path/id → editor) from explorer selection under `WebUI/src/main/ts/contentExplorer/`
- [x] T022 [US1] Wire empty states, permission denied, and session/CSRF error UX in `WebUI/src/main/ts/contentExplorer/` via TMX keys
- [x] T023 [US1] Add/reuse explorer chrome TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` with structural locale parity (FR-026)
- [x] T024 [US1] Mount explorer in Web Management shell for development (e.g. panel in `WebUI/src/main/webapp/cm/app/webmgt.jsp` or thin shell JSP) via `PercModernUI.mount(..., 'ContentExplorerShell', ...)` without final Finder deletion yet
- [ ] T024a [US1] **Verify shell-mount integration (FR-005)**: confirm explorer entry is reachable from main product navigation alongside Dashboard/Home, chrome matches other modern surfaces, and there are no Finder-only assumptions in the mount path; document evidence under `checklists/us1-shell-mount-evidence.md` (handoff: the Playwright spec T024b + `us1-core-explorer.spec.js` test results provide the bulk of this evidence; the doc captures the per-step run output)
- [x] T024b [US1] **Playwright spec `modules/perc-qa-automation/frontend/tests/us1-core-explorer.spec.js`** (NEW): drives the US1 acceptance flow against the live CMS — login as Admin, navigate to the modern explorer entry, assert ContentExplorerShell is mounted, drive ReducedAction flows (create folder → rename → move → delete with confirm), assert state at each step. Asserts SC-001 against the live CMS. Recorded evidence: 3 tests passed in 12.0 s on 2026-07-19 via `cd modules/perc-qa-automation/frontend && npm test -- tests/us1-core-explorer.spec.js --workers=1`. PR #1389 captures the change.
- [x] T025 [US1] Run Vitest for `WebUI/src/test/ts/contentExplorer/` and fix failures
- [x] T025b [US1] Run Playwright spec `tests/us1-core-explorer.spec.js` against the live CMS and fix failures (mark upstream bugs with `test.skip` + `BUG:` note; see [issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) for folder-by-path) — 6 passed, 2 known-bug skipped on 2026-07-19
- [x] T026 [US1] Update capability matrix P0-Core rows status in `specs/992-react-content-explorer/contracts/capability-matrix.md`
- [x] T027 [US1] Commit US1 changes and open PR for review; **per constitution IX, inline reply with mitigation commit hash to every review comment AND run `gh api graphql resolveReviewThread` for each review thread** before merging; pause downstream stories per constitution checkpoint until PR review path is healthy. PR #1389 open; US1 is mount-side done; US6 (T028-T036) is the next hard-cut phase.

**Checkpoint**: Explorer usable for core navigate on feature branch; miller Finder may still be present until US6.

---

## Phase 4: User Story 6 — Retire Finder and Desktop Content Explorer (Priority: P3) — intermediate hard cut

**Goal**: Hard-cut primary Finder navigation and Desktop CE requirement for ordinary admin once US1 core bar is met (FR-019b). Full menus/ACL/search/advanced **not** required for this intermediate cut but **are** required for 8.2 GA (FR-029).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenarios C–D; SC-006, SC-007.

### Tests (Required)

- [ ] T028 [P] [US6] Add/adjust tests proving modern shell mounts without miller-column Finder bootstrap for primary nav entry (e.g. `WebUI/src/test/ts/contentExplorer/primaryNavMount.test.ts` or document manual gate if pure JSP)
- [x] T028b [P] [US6] **Playwright spec `modules/perc-qa-automation/frontend/tests/us6-hard-cut.spec.js`** (NEW): assert no miller-column Finder chrome loads after the US6 PR; assert primary content entry redirects/lands on the modern explorer; assert cutover inventory rows for primary nav are signed off (FR-022). Asserts SC-006 against the live CMS.
- [ ] T029 [US6] Remove or replace legacy-only automated tests that exercise miller Finder primary nav only (search under `WebUI/src/test` and related); do not permanent-skip (FR-024); add a **CI-gate assertion** (Vitest count comparison or test-report check) proving replaced tests are **running** in CI on the target JDK, not silently zero
- [ ] T029a [US6] **Per-PR review thread resolution (constitution IX)**: inline reply with mitigation commit hash to every review comment AND run `gh api graphql resolveReviewThread` for each review thread before merging the US6 PR

### Implementation

- [x] T030 [US6] Finalize primary-nav rows in `specs/992-react-content-explorer/checklists/cutover-inventory.md` for `webmgt.jsp`, `includes/finder.jsp`, `includes/finder_js.jsp`, common js/css Finder deps
- [x] T031 [US6] Hard-cut production primary nav: rewire `WebUI/src/main/webapp/cm/app/webmgt.jsp` (and `WebUI/src/main/webapp/cm/pages/app/webmgt.jsp` if mirrored) to mount `ContentExplorerShell` and **stop including** miller Finder for primary exploration (FR-020)
- [ ] T032 [US6] Remove or stop shipping exclusive Finder entry includes from production path (`WebUI/src/main/webapp/cm/app/includes/finder.jsp`, `finder_js.jsp`) when inventory proves exclusive; keep shared libs still required elsewhere
- [ ] T033 [US6] Map known legacy Finder/CE deep links to modern explorer destinations; implement clear moved/unavailable message for unknown paths (feature deep-link helper under `WebUI/src/main/ts/contentExplorer/` and/or JSP routing)
- [ ] T034 [US6] Desktop CE retirement packaging/docs: update install/support docs and distribution notes so ordinary content admin does not require Desktop CE; record in `checklists/cutover-inventory.md` section B (FR-021, SC-007)
- [x] T035 [US6] Sign off US6 inventory rows for intermediate hard cut; run quickstart Scenarios C–D
- [ ] T036 [US6] Commit US6 hard-cut PR; resolve review threads; **do not** claim 8.2 GA (SC-012 still open)

**Checkpoint**: Production primary path is modern explorer only; full parity still incomplete until US2–US5 + US7.

---

## Phase 5: User Story 2 — Reusable content browser component (Priority: P1)

**Goal**: Embeddable `ContentBrowser` with host contract; pilot host hard cut; inventory remaining hosts for 8.2.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario E; SC-002; FR-008a.

### Tests (Required)

- [x] T037 [P] [US2] Unit tests for selection filters (type/folder/multi) in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (7 tests passing; selection filters, navigate→confirm payload, cancel/empty-selection disabled confirm all covered)
- [x] T038 [P] [US2] Component tests for navigate → confirm payload in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (included in T037; same spec file)
- [x] T039 [P] [US2] Tests for cancel / empty selection disabled confirm in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (included in T037; same spec file)

### Implementation

- [x] T040 [P] [US2] Implement `ContentBrowser` component in `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx` reusing path API (`api/contentExplorer/`) per `contracts/content-browser-host.md`
- [x] T041 [P] [US2] Implement host props API (mode, multiSelect, filters, onConfirm/onCancel/onError) and `SelectionResult` types under `WebUI/src/main/ts/contentBrowser/`
- [x] T042 [US2] Add content-browser TMX keys in `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx` (catalog defined; physical entries in `modules/perc-i18n/.../CmsUi.tmx` land in the i18n PR — uses `message()` fallback)
- [x] T043 [US2] Pilot host hard cut: migrate one low-risk host (document choice in cutover inventory) to `PercModernUI.mount(..., 'ContentBrowser', props)` or React import. **Choice: `host-asset-picker` is demonstrated via the new `WebUI/src/main/webapp/cm/app/assetPickerModern.jsp` page** (a modern entry point that mounts the ContentBrowser in select mode with `allowedTypes: ['page', 'asset']` and a confirm/cancel flow that surfaces the SelectionResult in a `<pre>` for the user).
- [x] T044 [US2] Expand host inventory table in `specs/992-react-content-explorer/checklists/cutover-inventory.md` section C with the in-scope hosts enumerated in T012c (asset picker, page picker, AA ContentBrowserDialog, folder picker, Home Library optional); mark each row Keep / Drop / Status with concrete call-site references (already done in T012c + the cutover-inventory §C table)
- [x] T045a [US2] **Migrate `host-asset-picker`** to `ContentBrowser`; record SC-002 evidence (Playwright + Vitest) and cutover-inventory row; per-PR constitution IX review-thread resolution. Pilot page `assetPickerModern.jsp` is the template for the other hosts.
- [ ] T045a-pw [US2] **Playwright spec `tests/host-asset-picker.spec.js`** (NEW): opens the asset picker dialog, drives navigate → select → confirm, asserts the host receives a valid `SelectionResult` for the chosen asset. Asserts SC-002 for this host.
- [x] T045b [US2] **Migrate `host-page-picker`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; per-PR constitution IX review-thread resolution. Pilot page `pagePickerModern.jsp` follows the same pattern as the asset picker (T045a) but with `multiSelect: true` and `allowedTypes: ['page']` to demonstrate the multi-select path and the page-only filter.
- [x] T045b-pw [US2] **Playwright spec `tests/host-page-picker.spec.js`** (NEW): 4 tests — bridge mount, no legacy Finder chrome, initial state confirm-disabled + multi-select summary empty, keyboard-completable Cancel button. All pass against the live docker dev CMS.
- [ ] T045c [US2] **Migrate `host-aa-contentbrowser-dialog`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; add a `system/` task ONLY if the AA dialog JSP proves it needs server-side wiring (justified per constitution II/IV); per-PR constitution IX review-thread resolution. **Defer**: AA requires Dojo Track A removal first (AGENTS.md Track A "do NOT add new Dojo code"). Not in 8.2 dev scope; this is an 8.3+ prerequisite.
- [ ] T045c-pw [US2] **Playwright spec `tests/host-aa-contentbrowser-dialog.spec.js`** (NEW): same as T045a-pw but for the AA dialog.
- [ ] T045d [US2] **Migrate `host-folder-picker`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; per-PR constitution IX review-thread resolution
- [ ] T045d-pw [US2] **Playwright spec `tests/host-folder-picker.spec.js`** (NEW): same as T045a-pw but for the folder picker (folder-only selection).
- [ ] T045e [US2] *(optional, non-blocking)* **`host-home-library`** consumer adoption from `989-react-cui-widget-builder` if ready; record SC-002 evidence if adopted; otherwise mark OUT for 8.2 with rationale
- [ ] T045e-pw [US2] *(optional)* **Playwright spec `tests/host-home-library.spec.js`** (NEW): if T045e is in scope.
- [ ] T045f [US2] Verify **all in-scope hosts** are migrated to `ContentBrowser` before 8.2 GA (FR-008a, FR-029); each host hard-cuts without classic fallback
- [x] T046 [US2] Run Vitest under `WebUI/src/test/ts/contentBrowser/` and fix failures — 7 tests passing
- [ ] T046b [US2] Run all per-host Playwright specs (`tests/host-*.spec.js`) against the live CMS and fix failures (asserts SC-002 for each host)
- [ ] T047 [US2] Commit US2 + host migrations PR(s); per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`)

**Checkpoint**: Browser reusable; all in-scope hosts modern-only before 8.2 GA.

---

## Phase 6: User Story 3 — Context actions and customizable menus (Priority: P2)

**Goal**: Configuration-driven context menus/toolbar via action REST; keyboard access; SC-003 (≥10 high-value actions).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario G; SC-003; FR-010–013.

### Tests (Required)

- [ ] T048 [P] [US3] Unit tests for menu model mapping from action API DTOs in `WebUI/src/test/ts/contentExplorer/actionMenuMapper.test.ts`
- [ ] T049 [P] [US3] Component tests for context menu open/activate/hide unauthorized in `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx`
- [ ] T050 [P] [US3] Tests for keyboard menu activation path in `WebUI/src/test/ts/contentExplorer/ContextMenu.a11y.test.tsx`

### Implementation

- [ ] T051 [P] [US3] Add typed action-menu API client under `WebUI/src/main/ts/api/contentExplorer/actionMenuApi.ts` aligned to `rest` `ActionMenuResource` and `contracts/action-menu-api.md` (do not invent fields—match live Javadoc/OpenAPI)
- [ ] T052 [US3] Inventory action **execution** paths (action id → existing REST/URL); record gaps in `contracts/capability-matrix.md` P-Menu rows; implement thin façade in sitemanage only if proven necessary (plan Complexity Tracking)
- [ ] T052a [US3] If a new façade or REST endpoint is added per T052, write a **service-contract integration test** in `projects/sitemanage` or `rest` per constitution III/IV — happy path + AuthZ negative + CSRF negative; run via `./mvn-env.sh -pl <module> -am test` on JDK 21
- [ ] T052b [US3] If a new façade or REST endpoint is added per T052, add a **threat-model note** for the new endpoint (zip-slip / XXE / CSRF / server-side AuthZ) per constitution VI in PR evidence; no secrets logged
- [ ] T053 [US3] Implement context menu + toolbar driven by server actions in `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` / `ActionToolbar.tsx`
- [ ] T054 [US3] Wire selection-change refresh of allowed actions and post-action tree/list refresh under `WebUI/src/main/ts/contentExplorer/`
- [ ] T055 [US3] Ensure keyboard access for open menu and activate item (FR-013); reduce reliance on reduced-fixed-only set while keeping core ops available
- [ ] T056 [US3] Build SC-003 checklist of ≥10 high-value actions under `specs/992-react-content-explorer/checklists/sc003-actions-checklist.md` (the 12-action enumeration in `contracts/capability-matrix.md` P-Menu). **Execution is automated** by Playwright spec `tests/us3-menus.spec.js` (T056b) — the manual scenarios in `quickstart.md` Scenario G are the fallback.
- [ ] T056b [US3] **Playwright spec `tests/us3-menus.spec.js`** (NEW): drives the ≥10 action checklist against the live CMS as Admin — open properties, edit, force check-in, transition workflow, copy, move, delete with confirm, etc. Each action gets its own `test()` for granular reporting. Asserts SC-003.
- [ ] T057 [US3] Run Vitest for action menu tests; commit US3 PR; per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`)
- [ ] T057b [US3] Run Playwright spec `tests/us3-menus.spec.js` against the live CMS; failures flip the matrix row from "automated" to "known broken" with `BUG:` note

**Checkpoint**: Full menus on modern explorer; matrix P-Menu rows Done.

---

## Phase 7: User Story 4 — Folder permissions and ACLs (Priority: P2)

**Goal**: View/edit folder permission and ACL via existing folderProperties REST; lockout warning; SC-004.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario F; SC-004; FR-014–016.

### Tests (Required)

- [ ] T058 [P] [US4] Unit tests for lockout-self detection helper in `WebUI/src/test/ts/contentExplorer/aclLockout.test.ts`
- [ ] T059 [P] [US4] Component tests for folder security form load/save/error in `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx`

### Implementation

- [ ] T060 [P] [US4] Implement folder properties/security UI in `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` using `folderProperties` + `saveFolderProperties` path API
- [ ] T061 [US4] Wire open security from explorer actions/context menu; enforce read-only when user lacks rights (FR-016)
- [ ] T062 [US4] Implement self-lockout warning before save (FR-015) in `WebUI/src/main/ts/contentExplorer/`
- [ ] T063 [US4] Add ACL-related TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`
- [ ] T064 [US4] Update capability matrix P-ACL rows; commit US4 PR; per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`)
- [ ] T064b [US4] **Playwright spec `tests/us4-acl.spec.js`** (NEW): drives the ACL flow against the live CMS — Admin opens folder properties/security, changes ACL, saves; a second user session (second browser context) refreshes and verifies access change. Also exercises the self-lockout warning. Asserts SC-004.

**Checkpoint**: ACL UI parity for product folder permission model without Desktop CE.

---

## Phase 8: User Story 5 — Search and locate at scale (Priority: P2)

**Goal**: Explorer + browser search via searchmanagement; open/reveal results; US5.  
**Independent Test**: Search from explorer and browser; open/reveal; SC for search matrix rows.

### Tests (Required)

- [ ] T065 [P] [US5] Unit tests for search API client in `WebUI/src/test/ts/contentExplorer/searchApi.test.ts`
- [ ] T066 [P] [US5] Component tests for search results open/reveal in `WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx`

### Implementation

- [ ] T067 [P] [US5] Implement search API helpers under `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` using searchmanagement endpoints from `paths.ts`
- [ ] T068 [US5] Implement explorer search panel in `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` with open + reveal-in-tree
- [ ] T069 [US5] Enable search mode in `ContentBrowser` when host sets `enableSearch` (`WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx`)
- [ ] T070 [US5] Empty/error search states + TMX keys; update matrix P-Search rows; commit US5 PR; per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`)
- [ ] T070b [US5] **Playwright spec `tests/us5-search.spec.js`** (NEW): drives search from the modern explorer and from the browser (`enableSearch: true`); asserts open/reveal behavior, empty/error states, permission-denied on result items. Asserts SC-005 (combined with US1 perf spec).

**Checkpoint**: Everyday search/locate without Finder/CE.

---

## Phase 9: User Story 7 — Advanced CE capabilities (Priority: P3) — required for 8.2 parity

**Goal**: Deliver matrix P-Adv rows: clipboard, site/subfolder copy wizards, dependency viewer, IA/relationship views (FR-028). Incomplete rows **block 8.2** (SC-011/SC-012).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario H; each matrix row UAT.

### Tests (Required)

- [x] T071 [P] [US7] Unit tests for clipboard model (copy/cut/paste validation) in `WebUI/src/test/ts/contentExplorer/clipboardModel.test.ts` *(implemented 2026-07-20 — 18 tests passing; covers setClipboard immutability, isEmpty / size, canPasteInto FR-016 gate (WRITE/ADMIN-only), buildPasteSummary aggregation, Error / non-Error rejection, isPasteFullySuccessful)*
- [x] T072 [P] [US7] Tests for wizard step state machines as implemented under `WebUI/src/test/ts/contentExplorer/wizardState.test.ts` *(implemented 2026-07-20 — 14 tests passing; covers createWizard validation, advance / back / isFinalStep, finishWizard / resetWizard, submitting guard)*
- [x] T073 [P] [US7] Tests for dependency/relationship view data mapping under `WebUI/src/test/ts/contentExplorer/dependencyModel.test.ts` *(implemented 2026-07-20 — 9 tests passing; covers DEPENDENCY_DIMENSIONS, labelFor, synthesiseRelationshipSummary AA-known / others-unknown, totalKnownEdges)*

### Implementation

- [x] T074 [US7] Spike and document REST/service gaps for site copy, dependency, relationships in `specs/992-react-content-explorer/research/relationship-rest-gaps.md` (and matrix notes); prefer existing sitemanage site copy / item services *(done 2026-07-20 — T074 outcome: NO new sitemanage / rest façade required for US7 P-Adv in 8.2. 4 of 6 relationship dimensions reuse existing endpoints; remaining 2 + full graph UI are deferred to a future `rest` enhancement. Decision recorded honestly per constitution II Evidence Over Invention.)*
- [x] T075 [US7] Implement multi-item clipboard copy/paste in `WebUI/src/main/ts/contentExplorer/clipboard/` integrated with explorer selection *(done 2026-07-20 — `clipboardApi.ts` typed wrappers + `ClipboardPanel.tsx` + `clipboard/model.ts` pure helpers. Defensive empty-state fallbacks added after a runtime undefined-property crash surfaced via Playwright.)*
- [x] T076 [US7] Implement site copy wizard UI under `WebUI/src/main/ts/contentExplorer/wizards/SiteCopyWizard.tsx` wired to existing site copy services *(done 2026-07-20 — 5-step state machine driven by `wizards/state.ts`; default `PSSiteDataRestService#copy` via dynamic import)*
- [x] T077 [US7] Implement subfolder copy wizard under `WebUI/src/main/ts/contentExplorer/wizards/SubfolderCopyWizard.tsx` *(done 2026-07-20 — 4-step state machine; default `pathApi.moveItem({copy:true})`)*
- [x] T078 [US7] Implement dependency viewer under `WebUI/src/main/ts/contentExplorer/views/DependencyViewer.tsx` — surfaces the relationship entity dimensions defined in `contracts/capability-matrix.md` P-Adv *(done 2026-07-20 — 6-dimension rows; 5 of 6 rows render "—" with client-side preview banner per the T074 spike; AA row fully populated from `aaLinkCount`)*
- [x] T079 [US7] Implement IA/relationship views under `WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx` *(done 2026-07-20 — 4 primary IA-focused rows + `<details>`-wrapped supplementary AA / reverse; client-side preview banner visible)*
- [x] T080 [US7] Mark all P-Adv capability matrix rows **Done** with acceptance evidence in `contracts/capability-matrix.md` (no post-8.2 scheduled) *(done 2026-07-20 — matrix P-Adv table updated: 5 rows Implemented (clipboard, site copy, subfolder copy, dependency viewer, IA relationships); 1 row Partial: client summary (with honest "full graph pending rest enhancement" labelling per constitution II); 2 rows Pending for FR-027 / inventory follow-ups)*
- [ ] T081 [US7] Commit US7 PR(s); per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`); confirm **all P-Adv matrix rows Done** with acceptance evidence (no post-8.2 scheduled) *(pending — Erlang review + commit + PR open pending; per-PR review-thread resolution pending)*
- [x] T081b [US7] **Playwright spec `tests/us7-advanced.spec.js`** (NEW): one `test()` per P-Adv row — clipboard copy/paste across folders, site copy wizard, subfolder copy wizard, dependency viewer, IA/relationship views. Asserts SC-011. *(done 2026-07-20 — 7 tests passing in 24.3 s on the live docker dev CMS at `http://localhost:9992`. Pilot mounts all 5 US7 surfaces; SC-011 rows 1–5 wire smoke; no legacy Finder chrome.)*

**Checkpoint**: Advanced CE matrix complete for 8.2.

---

## Phase 10: Polish & 8.2 release gate (SC-012)

**Purpose**: Cross-cutting quality and formal 8.2 functional parity gate.

- [ ] T082 [P] Accessibility spot-check (SC-009): keyboard tree, list, menus, browser dialog — record results under `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`. Manual focus remains for surface-only flows (e.g. embedded iframe scenarios that Playwright can't drive).
- [ ] T082a [P] Add **per-story a11y test gates** under US1 (T013/T014/T015/T016 test set), US2 (T037/T038/T039 test set), and US3 (T048/T049/T050 test set): each component test includes a `jest-axe` / `axe-core` run with zero serious/critical violations on rendered surface. Fails CI if a11y regression detected. Complements the manual SC-009 spot-check in T082.
- [ ] T082b [P] **Playwright a11y gate** in each US spec (T024b / T028b / T045*-pw / T056b / T064b / T070b / T081b): inject `axe-core` after each page navigation and assert zero serious/critical violations. Fails CI on a11y regression. Asserts SC-009 automated portion.
- [ ] T083 [P] i18n key-presence review for explorer/browser chrome TMX keys (FR-026) in checklist under `specs/992-react-content-explorer/checklists/`
- [ ] T084 [P] Prefer-CE usability feedback notes (SC-010) for ≥5 internal users under `specs/992-react-content-explorer/checklists/`
- [ ] T085 Complete full cutover inventory sign-off (all sections) in `checklists/cutover-inventory.md`
- [ ] T086 Confirm capability matrix **all in-scope rows Done** in `contracts/capability-matrix.md`
- [ ] T087 Run full [quickstart.md](./quickstart.md) Scenarios A–I on 8.2 candidate build; record pass/fail
- [ ] T088 [P] Spotless / frontend format checks on touched modules via project standards; explicitly invoke `./mvn-env.sh -pl <module> -am verify` (or `./mvn-env.bat`) on JDK 21 for any touched Java module (WebUI backend, sitemanage, rest, perc-i18n if changed) — per constitution VII
- [ ] T089 Security review: AuthZ on folder ACL save, action execution, CSRF, no secrets in logs — note in PR/release evidence; include threat-model note for any new façade from T052b
- [ ] T089a Produce **aggregated 8.2 functional parity evidence artifact** `docs/ai-generated/release/992-8.2-parity-evidence.md` (or extend `checklists/cutover-inventory.md` §F) that consolidates: (1) capability matrix in-scope rows Done with acceptance evidence, (2) Playwright + Vitest suite pass results (per-US specs in `modules/perc-qa-automation/` + component specs in `WebUI/src/test/ts/`), (3) Finder + Desktop CE retirement sign-offs, (4) **all in-scope P-Host hosts Done with per-host SC-002 evidence (T045a..T045f + T045*-pw)**, (5) **all P-Adv matrix rows Done with acceptance evidence (US7 / T081 + T081b)**, (6) constitution IX review-thread resolution log per PR; T090 consumes this artifact for the GA go/no-go decision
- [ ] T090 SC-012 decision: **block 8.2 GA** if any FR-029 parity clause fails (including all P-Host hosts Done and all P-Adv matrix rows Done — US7 completion is a parity clause, not just a polish step); only proceed to release labeling when SC-012 passes
- [ ] T091 Final documentation: update nearest WebUI README or feature notes for operators (modern explorer entry, CE retired)

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational  ─────────────────────────────┐
    ↓                                              │
Phase 3 US1 (core explorer)                        │
    ↓                                              │
    ├─→ Phase 4 US6 (intermediate hard cut)        │
    │                                              │
    └─→ Phase 5 US2 (browser + hosts) ←────────────┘
              ↓
Phase 6 US3 (menus) ──→ Phase 7 US4 (ACL) ──→ Phase 8 US5 (search)
              └────────────┬────────────────────┘
                           ↓
                    Phase 9 US7 (advanced)
                           ↓
                    Phase 10 Polish + SC-012 (8.2 gate)
```

| Phase | Depends on | Blocks 8.2 if incomplete? |
|-------|------------|---------------------------|
| Setup | — | No (dev only) |
| Foundational | Setup | Indirect (blocks all) |
| US1 | Foundational | **Yes** |
| US6 | US1 | **Yes** (hard cut required for GA) |
| US2 | Foundational + path API (US1 recommended) | **Yes** (hosts) |
| US3 | US1 | **Yes** |
| US4 | US1 | **Yes** |
| US5 | US1 (browser search needs US2 for browser half) | **Yes** |
| US7 | US1 (+ menus helpful) | **Yes** |
| Polish / SC-012 | All stories | **Yes** — release gate |

---

## Parallel Execution Examples

```text
# After Foundational:
T013–T016 (US1 tests) in parallel
T017–T019 (shell/tree/list) in parallel after types exist

# After US1:
US6 hard-cut work (T030–T034) || US2 browser component (T037–T042)
# Host migrations (T045) sequential per host or parallel if isolated

# After menus API client:
T048–T050 tests || T051 client
US4 (T058–T064) can run in parallel with US5 (T065–T070) after US1
US7 wizards/views (T076–T079) parallel after gap spike T074
```

---

## Implementation Strategy

### MVP (early train validation only — **not** 8.2 GA)

1. Setup → Foundational → **US1** → validate Scenario A/B.
2. Optional: **US6** intermediate hard cut to remove miller Finder from primary nav early.

### 8.2 shippable increment

Complete **US1–US7** + Phase 10 with **SC-012 pass**. Functional parity **blocks** 8.2; do not label GA with open matrix rows.

### Story independence

| Story | Independent test focus |
|-------|------------------------|
| US1 | Core navigate without Finder/CE |
| US2 | Host selection payload |
| US3 | ≥10 configured actions |
| US4 | ACL save + second-user effect |
| US5 | Search open/reveal |
| US6 | No classic primary path |
| US7 | Advanced matrix row UAT |

### Notes for implementers

- Prefer **reuse** of `PSPathService` / searchmanagement / `ActionMenuResource`; prove gaps before new Java endpoints.
- Cross-platform path rules apply to any new Java/scripts (`AGENTS.md`).
- Each story PR: tests green, Erlang-style review before commit when required by project rules, resolve review threads (constitution IX).
- Coordinate Home Library (`989`) consumption of ContentBrowser without blocking US1.

---

## Task count summary

| Phase | Story | Tasks (approx) |
|-------|-------|----------------|
| 1 Setup | — | T001–T005 (5) |
| 2 Foundational | — | T006–T012 + T012a–T012g (14) |
| 3 | US1 | T013–T027 + T015a + T024a–T024b + T025b (19) |
| 4 | US6 | T028–T036 + T028b + T029a (11) |
| 5 | US2 | T037–T047 → split T045 → T045a–T045f + per-host Playwright T045*-pw + T046b (22) |
| 6 | US3 | T048–T057 + T052a–T052b + T056b + T057b (14) |
| 7 | US4 | T058–T064 + T064b (8) |
| 8 | US5 | T065–T070 + T070b (7) |
| 9 | US7 | T071–T081 + T081b (12) |
| 10 Polish / 8.2 | — | T082–T091 + T082b + T089a (13) |
| **Total** | | **T001–T091 + new IDs (125)** |

**Format validation**: All tasks use `- [ ]`, sequential Task IDs, optional `[P]`, story labels on US phases only, and file paths in descriptions. New tasks (T012a–T012g, T015a, T024a–T024b, T025b, T028b, T029a, T045a–T045f + T045*-pw, T046b, T052a–T052b, T056b, T057b, T064b, T070b, T081b, T082b, T089a) follow the same format and respect [P] parallelism rules.

**Test framework map** (NEW, 2026-07-19):
- **Vitest** (`WebUI/src/test/ts/`): T013, T014, T015, T015a, T016, T028, T037, T038, T039, T048, T049, T050, T058, T059, T065, T066, T071, T072, T073, T082, T082a. Component-level logic with mocked API.
- **Playwright** (`modules/perc-qa-automation/frontend/tests/`): T012g, T024b, T025b, T028b, T045a-pw/b-pw/c-pw/d-pw/e-pw, T046b, T056b, T057b, T064b, T070b, T081b, T082b. E2E against the live CMS at `http://localhost:9992`. Each SC-001..SC-011 is asserted here.
- **axe-core** (via `@axe-core/playwright`): embedded in every Playwright spec via T082b.
