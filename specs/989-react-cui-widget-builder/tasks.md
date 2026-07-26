# Tasks: Migrate Home/Contributor UI and Widget Builder to Modern UI

**Input**: Design documents from `/specs/989-react-cui-widget-builder/`  
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required by FR-011 / FR-018 (modern UI and/or service-contract coverage; replace legacy-only client tests).

**Organization**: Phases by user story. Prefer one PR per story (US1 → US2 → US3). **Release gate (FR-008):** do **not** merge US1/US2 alone to `development` / a shippable release line without US3 in the same train—classic entry files must not remain requestable in a customer build. Release only after US3 + removal inventory sign-off (big-bang).

## Format

`- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: parallelizable (different files; no wait on incomplete sibling work)
- **[USn]**: user-story phase only

---

## Phase 1: Setup

**Purpose**: Orient and baseline the WebUI Track B toolchain.

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/AGENTS.md`
- [x] T002 Confirm JDK 21 branch baseline and that WebUI modern tests run via `./mvn-env.sh -pl WebUI` (and/or `WebUI/src/main/frontend` npm test per module docs)
- [x] T003 [P] Confirm removal inventory scaffold exists and is the sign-off target in `specs/989-react-cui-widget-builder/checklists/removal-inventory.md`
- [x] T003a [P] Confirm i18n key-presence checklist scaffold exists in `specs/989-react-cui-widget-builder/checklists/i18n-key-checklist.md` (SC-008 / FR-024)
- [x] T004 [P] Skim contracts: `specs/989-react-cui-widget-builder/contracts/home-deep-links.md` and `contracts/widget-builder-api.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared API surface, registry hooks, and URL mapping utilities every story needs. **Blocks US1–US3.**

- [x] T005 Document contributor REST operations used by Home by inventorying `WebUI/src/main/webapp/cm/plugins/PercContributorUiAdaptor.js` and related `perc_path_constants.js` entries into a short implementer note under `specs/989-react-cui-widget-builder/` (or extend research.md)—**hard gate before T016–T019**: written endpoint list for Recent, Library, Search, and Create (partial MVP list OK if gaps are explicitly deferred)
- [x] T006 [P] Add typed Home API helpers under `WebUI/src/main/ts/api/home/` (recent, sites/folders, search, create page/asset/blog wrappers) using `WebUI/src/main/ts/api/client.ts` + `csrf.ts`
- [x] T007 [P] Add typed Widget Builder API helpers under `WebUI/src/main/ts/api/widgetbuilder/` matching `contracts/widget-builder-api.md` paths (`/widgetmanagement/widgetbuilder/*`)
- [x] T008 Add shared TypeScript DTO types aligned to `data-model.md` under `WebUI/src/main/ts/api/types/` (or colocated with API modules)
- [x] T009 Register placeholder components `HomeShell` and `WidgetBuilderApp` in `WebUI/src/main/ts/registry.ts` and export from `WebUI/src/main/ts/index.ts` so Vite bundle includes them
- [x] T010 Implement deep-link / `initialScreen` mapping helper (library→Library, list→Recent, search→Search, newitem→Create) under `WebUI/src/main/ts/home/deepLinkMap.ts` per `contracts/home-deep-links.md`
- [x] T010a [P] Add thin TypeScript i18n helper under `WebUI/src/main/ts/i18n/` (e.g. `message.ts`) that delegates to global `I18N.message` for TMX keys (FR-023); document usage for Home/WB components
- [x] T011 Assess security surface for new shells: CSRF on mutating calls, session expiry UX, WB enablement gate; note findings in plan or implementer note (no secrets in logs)

**Checkpoint**: API modules + registry placeholders + i18n helper compile; no production cutover yet.

---

## Phase 3: User Story 1 — Modern Home / Contributor shell (Priority: P1)

**Goal**: Single modern Home shell with Recent, Library, Search, Create; no CUI iframe for production Home path.  
**Independent Test**: Open Home from nav; exercise all four sections per SC-001 / [quickstart.md](./quickstart.md); deep links map correctly; session/permission errors recoverable.

### Tests (Required)

- [x] T012 [P] [US1] Unit tests for deep-link mapping in `WebUI/src/test/ts/home/deepLinkMap.test.ts`
- [x] T013 [P] [US1] Unit/component tests for Home shell section navigation in `WebUI/src/test/ts/home/HomeShell.test.tsx`
- [x] T014 [P] [US1] Tests for Home API error handling (401/403/network) in `WebUI/src/test/ts/home/homeApi.test.ts`

### Implementation

- [x] T015 [P] [US1] Implement `HomeShell` layout and section nav in `WebUI/src/main/ts/home/HomeShell.tsx` (+ styles as needed under `WebUI/src/main/ts/home/`) using i18n helper for chrome labels (FR-021/023)
- [x] T016 [P] [US1] Implement Recent section in `WebUI/src/main/ts/home/sections/RecentSection.tsx` using Home API helpers
- [x] T017 [P] [US1] Implement Library section (site/folder browse + open item) in `WebUI/src/main/ts/home/sections/LibrarySection.tsx`
- [x] T018 [P] [US1] Implement Search section in `WebUI/src/main/ts/home/sections/SearchSection.tsx`
- [x] T019 [US1] Implement Create section (page/asset/blog and related flows in scope) in `WebUI/src/main/ts/home/sections/CreateSection.tsx` and wizard subcomponents under `WebUI/src/main/ts/home/create/`
- [x] T020 [US1] Wire empty states and role messaging (no sites / create site for admins) in Home components under `WebUI/src/main/ts/home/` via TMX keys (not hardcoded English-only)
- [x] T020a [US1] Add/reuse Home chrome TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` with structural locale parity (FR-022); record keys on `specs/989-react-cui-widget-builder/checklists/i18n-key-checklist.md`
- [x] T021 [US1] Create thin modern Home shell JSP (new filename, e.g. `homeModern.jsp`) under `WebUI/src/main/webapp/cm/app/` that loads CSRF/`JavaScriptServlet`, **`/Rhythmyx/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…` (session locale, FR-023)**, modern bundle `/cm/modern/`, and `PercModernUI.mount('…', 'HomeShell', props)`
- [x] T022 [US1] Mirror modern Home shell JSP under `WebUI/src/main/webapp/cm/pages/app/` if that tree is still deployed (include same `tmx.jsp` pattern)
- [x] T023 [US1] Rewire `views.put("home", …)` in `WebUI/src/main/webapp/cm/app/index.jsp` and `cm/pages/app/index.jsp` to the new shell; pass mapped `initialSection` from query without classic iframe
- [x] T024 [US1] Ensure open-item / editor handoff uses existing product navigation patterns (no full editor rewrite) from Home Library/Recent/Search
- [x] T025 [US1] Run Home Vitest suite and fix failures under `WebUI/src/test/ts/home/`
- [ ] T026 [US1] Commit US1 changes and open PR for review; pause downstream story work per constitution checkpoint. **Do not merge US1 alone to `development`/release** without US2+US3 in the same train (FR-008 release gate). Include partial i18n checklist rows for Home keys in PR notes.
- [ ] T027 [US1] Address review/CI feedback and resolve review threads; verify stack-ready approval before US2 continues on integrated baseline

**Checkpoint**: Home is modern-only via view key `home` on the feature branch; classic `home.jsp` may still exist on disk until US3 hard cut and is no longer the mapped view target. **Not shippable alone** (FR-008).

---

## Phase 4: User Story 2 — Modern Widget Builder (Priority: P1)

**Goal**: Modern WB UI for list/create/edit/validate/deploy using existing server services; last-write-wins save UX.  
**Independent Test**: With WB enabled, create/save/reload/package a simple definition under 15 minutes (SC-002); disabled WB inaccessible (SC-006).

### Tests (Required)

- [x] T028 [P] [US2] Component tests for definition list/empty state in `WebUI/src/test/ts/widgetbuilder/DefinitionList.test.tsx`
- [x] T029 [P] [US2] Tests for save/validate result handling (success + server validation errors) in `WebUI/src/test/ts/widgetbuilder/DefinitionEditor.test.tsx`
- [x] T030 [P] [US2] Tests for enablement/denied behavior when `/widgetbuilder/active` is false in `WebUI/src/test/ts/widgetbuilder/enablement.test.ts`

### Implementation

- [x] T031 [P] [US2] Implement `WidgetBuilderApp` shell in `WebUI/src/main/ts/widgetbuilder/WidgetBuilderApp.tsx` using i18n helper for chrome labels (FR-021/023)
- [x] T032 [P] [US2] Implement definition list table/UI in `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx` using summaries API
- [x] T033 [US2] Implement definition editor (metadata, fields, display HTML, JS/CSS resources) under `WebUI/src/main/ts/widgetbuilder/editor/`
- [x] T034 [US2] Wire validate + save + deploy/delete actions with success confirmation and reload-from-server truth (last-write-wins) in `WebUI/src/main/ts/widgetbuilder/`
- [x] T034a [US2] Add/reuse Widget Builder chrome TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` with structural locale parity (FR-022); record keys on `specs/989-react-cui-widget-builder/checklists/i18n-key-checklist.md`
- [x] T035 [US2] Create thin modern WB shell JSP (new filename, e.g. `widgetBuilderModern.jsp`) under `WebUI/src/main/webapp/cm/app/` loading CSRF/`JavaScriptServlet`, **`tmx.jsp` with session locale (FR-023)**, modern bundle, and `PercModernUI.mount` for `WidgetBuilderApp`
- [x] T036 [US2] Mirror modern WB shell under `WebUI/src/main/webapp/cm/pages/app/` if needed (include same `tmx.jsp` pattern)
- [x] T037 [US2] Rewire `views.put("widgetbuilder", …)` in `WebUI/src/main/webapp/cm/app/index.jsp` and `cm/pages/app/index.jsp`; preserve admin/designer view gates and `IS_WIDGET_BUILDER_ACTIVE` nav behavior
- [x] T038 [US2] Run WB Vitest suite under `WebUI/src/test/ts/widgetbuilder/` and fix failures
- [ ] T039 [US2] Commit US2 changes and open PR; pause for review/CI. **Do not merge US2 alone to `development`/release** without US3 in the same train (FR-008). Update i18n checklist rows for WB keys.
- [ ] T040 [US2] Address feedback, resolve threads; verify stack ready before US3 deletion work

**Checkpoint**: Widget Builder modern-only via view key `widgetbuilder` on the feature branch; classic `widgetBuilder.jsp` unmapped but may remain on disk until US3. **Not shippable without US3** (FR-008).

---

## Phase 5: User Story 3 — Remove legacy Home/CUI and Widget Builder clients (Priority: P2)

**Goal**: Hard-delete exclusive classic clients, classic entry JSPs, packed WB assets; orphan vendors only if inventory proves unused; replace legacy-only tests; sign removal inventory.  
**Independent Test**: Inventory 100% signed (SC-003); Home/WB load zero retired scripts; CI green without legacy client tests (SC-004); nav smoke (SC-005).

### Tests (Required)

- [x] T041 [US3] Confirm modern Home + WB test suites remain green after deletions under `WebUI/src/test/ts/home/` and `WebUI/src/test/ts/widgetbuilder/`
- [x] T042 [US3] Remove legacy-only client tests `WebUI/src/test/js/percWidgetBuilderDefinitionView.test.js` and `WebUI/src/test/js/percWidgetFieldsViews.test.js` (and any other exclusive CUI/WB client tests found); ensure build does not reference them

### Implementation

- [x] T043 [US3] Delete classic entry JSPs `WebUI/src/main/webapp/cm/app/home.jsp` and `widgetBuilder.jsp` (hard cut—no rewrite/redirect stubs)
- [x] T044 [US3] Delete mirror classic entry JSPs under `WebUI/src/main/webapp/cm/pages/app/home.jsp` and `widgetBuilder.jsp` if present
- [x] T045 [US3] Delete exclusive CUI trees `WebUI/src/main/webapp/cm/cui/` and `WebUI/src/main/webapp/cm/pages/cui/`
- [x] T046 [US3] Delete exclusive Widget Builder client trees `WebUI/src/main/webapp/cm/widgetbuilder/` and `WebUI/src/main/webapp/cm/app/widgetbuilder/` (and pages mirrors if any)
- [x] T047 [US3] Remove packed WB client artifacts and packaging references (`perc_widgetBuilder.packed.min.js` / `.css`, build script entries under `WebUI/src/main/frontend/scripts/` or Maven config as applicable)
- [x] T048 [US3] Complete manual orphan-vendor inventory for Backbone, Underscore, Backgrid (and similar) in `specs/989-react-cui-widget-builder/checklists/removal-inventory.md`; drop only zero-consumer libs from production distribution
- [x] T049 [US3] Explicitly retain shared assets still in use (platform jQuery, `perc_widget_library` used by `webmgt.jsp` / `editAsset.jsp`) and document consumers in the inventory
- [x] T050 [US3] Finish known deep-link mappings without classic JSP stubs (dispatcher and/or modern shell props) per `contracts/home-deep-links.md` and FR-013; record final modern shell JSP filenames in `specs/989-react-cui-widget-builder/checklists/removal-inventory.md` rewire section
- [x] T050a [US3] Implement an explicit **moved/unavailable** on-page surface for unmapped obsolete Home/CUI/WB paths (e.g. thin JSP under `WebUI/src/main/webapp/cm/app/` and/or React component under `WebUI/src/main/ts/home/UnavailableView.tsx` mounted via dispatcher)—clear user message via **TMX key** (FR-013 / FR-021 / SC-007), no classic UI, no silent blank page
- [x] T050b [P] [US3] Unit or component test for unavailable/moved messaging in `WebUI/src/test/ts/home/UnavailableView.test.tsx` (or equivalent path)
- [x] T050c [US3] Add/reuse TMX key(s) for unavailable/moved message in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` (FR-022) and list on i18n-key-checklist
- [x] T051 [US3] Clean packaged/synced `WebUI/war/cui` and `war/widgetbuilder` (or ensure build no longer copies them) so installs do not ship retired trees
- [x] T052 [US3] Fill and sign off all sections of `specs/989-react-cui-widget-builder/checklists/removal-inventory.md` in the US3 PR description/review (include final shell JSP names and orphan keep/drop)
- [ ] T053 [US3] Commit US3 changes and open PR to complete the shippable train with US1+US2; address CI/review; resolve threads

**Checkpoint**: Production Home/WB paths modern-only; inventory signed; **shippable only when US1–US3 are all on the release branch** (FR-008 big-bang).

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Docs, a11y bar, final verification.

- [x] T054 [P] Update WebUI-facing notes if needed (`WebUI/AGENTS.md` migration status and/or module README) documenting Home/WB React shells and removal of CUI/WB classic
- [x] T055 [P] Accessibility pass on primary Home actions and WB forms (keyboard, labels) per FR-012 in `WebUI/src/main/ts/home/` and `widgetbuilder/`
- [ ] T056 Run Home and Widget Builder end-to-end smoke from [quickstart.md](./quickstart.md) (SC-001, SC-002, SC-006, SC-007 known deep links) and record results in PR or feature notes
- [ ] T056a Execute **main-nav smoke checklist (SC-005 / FR-020)** and record pass/fail in PR or `specs/989-react-cui-widget-builder/checklists/` notes: (1) Dashboard opens; (2) at least one other non-Home tab (e.g. editor/Web Management or Design) opens for the tester’s roles; (3) Home still opens; (4) Widget Builder opens only when enabled
- [ ] T056b UAT sample **unmapped** legacy path shows the on-page moved/unavailable message (SC-007); record URL used and result
- [ ] T056c Complete and sign **SC-008 / FR-024 i18n key-presence checklist** in `specs/989-react-cui-widget-builder/checklists/i18n-key-checklist.md` (primary Home + WB chrome → TMX keys; optional non-default locale spot-check if env available); attach to shippable PR notes
- [ ] T057 Security review of CSRF, session expiry messaging, and WB AuthZ gates on new shells
- [x] T058 Spotless / formatting and frontend lint clean on touched WebUI sources

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies
- **Phase 2 (Foundational)**: after Setup; **blocks** US1–US3
- **Phase 3 (US1 Home)**: after Foundational
- **Phase 4 (US2 Widget Builder)**: after Foundational; preferably after US1 PR checkpoint (constitution story order) but code-independent of Home sections
- **Phase 5 (US3 Removal)**: after US1 and US2 production paths are live on the feature branch (must not delete classic while still mapped); **required before any shippable merge**
- **Phase 6 (Polish)**: after US3 (or in parallel with late US3 docs once deletes are done)

### User story dependencies

```text
Setup → Foundational → US1 (Home) → US2 (WB) → US3 (Removal) → Polish
                         ↘__________↗
              (WB can start after Foundational if staffed in parallel,
               but cutover/delete still waits for both modern paths)
```

### Within each story

1. Tests (can start [P] with stubs)
2. Components / API usage
3. Shell JSP + `index.jsp` rewire
4. Test green
5. PR checkpoint

### Parallel opportunities

- T006 || T007 (Home API vs WB API modules)
- T012–T014 Home tests in parallel
- T015–T018 Home sections in parallel after shell scaffold
- T028–T030 WB tests in parallel
- T031–T032 WB shell/list in parallel
- T043–T046 deletes can be parallelized carefully after rewires verified
- T054 || T055 polish items

---

## Parallel Example: User Story 1

```bash
# After T009–T010 (registry + deepLinkMap):
# Parallel tests
Task: T012 deepLinkMap.test.ts
Task: T013 HomeShell.test.tsx
Task: T014 homeApi.test.ts

# Parallel sections (after T015 shell shell)
Task: T016 RecentSection.tsx
Task: T017 LibrarySection.tsx
Task: T018 SearchSection.tsx
# Then T019 Create (often depends on shared create helpers)
```

## Parallel Example: User Story 2

```bash
Task: T028 DefinitionList.test.tsx
Task: T029 DefinitionEditor.test.tsx
Task: T030 enablement.test.ts
# Implementation
Task: T031 WidgetBuilderApp.tsx
Task: T032 DefinitionList.tsx
```

---

## Implementation Strategy

### MVP (recommended first delivery slice)

1. Phase 1–2 complete
2. **US1 only**: modern Home with four sections + tests + view rewire
3. Validate SC-001 / deep links on a feature build

### Incremental delivery to release

1. US1 PR → review/merge stack
2. US2 PR → modern WB
3. US3 PR → hard delete + unavailable surface + inventory sign-off
4. Polish + quickstart smoke + **main-nav checklist (T056a)** + unmapped-path UAT (T056b) + **i18n key checklist (T056c / SC-008)**
5. Merge/release only when US1+US2+US3 present on the shippable train (FR-008 big-bang)—never US1/US2 alone

### Story independence

| Story |                  Independently testable when                   |
|-------|----------------------------------------------------------------|
| US1   | `view=home` mounts React shell; sections work without WB       |
| US2   | `view=widgetbuilder` mounts React WB; server APIs unchanged    |
| US3   | Inventory + absence of classic trees; modern paths still green |

---

## Notes

- **Do not** rewrite classic `home.jsp` / `widgetBuilder.jsp` as modern hosts (FR-017).
- **Do not** remove `perc_widget_library` or platform jQuery without inventory proof.
- **Do not** change Widget Builder package generation server algorithms unless fixing a blocking bug.
- **Do not** ship Home/WB user-visible chrome as English-only hardcoded React strings—use TMX + `I18N.message` (FR-021/023).
- Shell JSP final filenames are implementer choice; update inventory and tasks paths if names differ.
- Dual trees `cm/app` and `cm/pages` must stay in sync for rewire/delete.
- Prefer key names under `perc.ui.home…` / `perc.ui.widgetbuilder…` (or existing peer prefixes) when adding units to `CmsUi.tmx`.

---

## Task summary

|       Phase        |              Tasks               |  Count  |
|--------------------|----------------------------------|---------|
| Setup              | T001–T004 (+ T003a)              | 5       |
| Foundational       | T005–T011 (+ T010a)              | 8       |
| US1 Home           | T012–T027 (+ T020a)              | 17      |
| US2 Widget Builder | T028–T040 (+ T034a)              | 14      |
| US3 Removal        | T041–T053 (+ T050a/b/c)          | 16      |
| Polish             | T054–T058 (+ T056a/b/c)          | 8       |
| **Total**          | base + i18n/remediation suffixes | **~68** |

| Story |       Task IDs       | Count |
|-------|----------------------|-------|
| US1   | T012–T027, T020a     | 17    |
| US2   | T028–T040, T034a     | 14    |
| US3   | T041–T053, T050a/b/c | 16    |

**MVP scope**: Phases 1–2 + US1 (through T027, including T010a/T020a/T021 tmx)—feature-branch only; not shippable without US2+US3.  
**Format validation**: All tasks use `- [ ]`, IDs, story labels on US phases only, and concrete file paths.
