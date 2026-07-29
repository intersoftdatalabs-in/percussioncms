# Implementation Plan: Unified React Content Explorer

**Branch**: `992-react-content-explorer` | **Date**: 2026-07-19 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/992-react-content-explorer/spec.md`

## Summary

Replace the Web Management **miller-column Finder** (`perc_finder` / FancyTree columns embedded in `webmgt.jsp`) and retire **Desktop Content Explorer** for ordinary content admin. Ship a **Track B React** explorer (tree + detail list) plus a **reusable content browser** mountable from dialogs/hosts. Use existing **sitemanage path/search REST** for navigation and folder ops; extend or adapt **public REST action menus** and folder properties for full CE-depth menus/ACL. **Hard cut per phase** within the train (no production classic fallback). Finder and Desktop CE share the **same intermediate core-navigate gate**; menus, ACL UI, search, host migrations, and advanced CE tools (clipboard, site-copy wizards, dependency/IA views) complete as ordered phases **inside this feature**.

**Release constraint (locked):** Target product release is **8.2**. **Everything in scope for this feature is 8.2 scope.** **Functional parity** (capability matrix in-scope rows Done + SC-012) **blocks 8.2 GA**. Work MUST NOT be deferred to a post-8.2 product release.

## Technical Context

- **Language/Version**: Java 21 (branch `development` / feature off development); TypeScript + React 19 (WebUI Vite pipeline); Node 22 (Playwright + frontend-maven-plugin for `modules/perc-qa-automation`)
- **Owning Module(s)**:
  - `WebUI/` — primary UI (explorer shell, content browser component, Finder hard-cut wiring)
  - `projects/sitemanage/` — path management, folder properties/permissions, search (reuse first; small REST gaps only if proven)
  - `rest/` — `ActionMenuResource` (`/actions`) for configuration-driven menus (post-cutover US3)
  - `modules/DesktopContentExplorer/` — capability source + distribution retirement (no new JavaFX work)
  - `modules/perc-i18n/` — TMX keys for explorer/browser chrome
  - **`modules/perc-qa-automation/`** — Playwright + TestNG end-to-end test module. **Primary automated acceptance surface for SC-001..SC-011** in this feature. One Playwright spec per US, runnable against the live docker dev CMS at `http://localhost:9992`.
  - `system/` — **NOT a default touched module**. In scope **only when a specific in-scope host hard-cut proves it needs server-side wiring** (e.g. an action-page / content-browser dialog JSP that cannot be replaced via WebUI + sitemanage + REST). Each such case adds a per-host task in `tasks.md` US2; otherwise `system/` is untouched.
- **Dev runtime (operational, 2026-07-19)**: Docker compose stack (`docker-compose.yml`) bringing up `percussion-cms-dts` against a host-side install at `/opt/Percussion`. Install is run once on host via `scripts/install-cms-dev.py`; container is service-only. Default DB is **Derby** (dev/test). MySQL mode is deferred pending [issue #1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) (collation bug). CMS reachable at `http://localhost:9992/Rhythmyx/login` with Admin/Editor/Contributor credentials auto-discovered from `/opt/Percussion/var/config/generated/passwords`. UI development does **not** require MySQL — the backend DB type is not a UI implementation concern.
- **AGENTS Hierarchy**: root `AGENTS.md`, `WebUI/AGENTS.md` (Track B bridge, Vite, tests), **`modules/perc-qa-automation/AGENTS.md` (QA automation profile)**; module AGENTS when touching sitemanage/rest
- **Dependencies & Storage**:
  - Path REST: `/services/pathmanagement/path/*` (`PSPathService`) — folder children, paginatedFolder, move, rename, add, delete, folderProperties, saveFolderProperties
  - Search REST: `/services/searchmanagement/search/*`
  - Public actions REST: `/actions` (`rest` module `ActionMenuResource`) for later menu phase
  - Session cookies + CSRF via `WebUI/src/main/ts/api/client.ts` / existing OWASP token pattern; **`RX_USEBASICAUTH: true` header** to opt REST/JSON endpoints into Basic-auth flow (required by `tests/helpers/auth.js`)
  - **No new DB schema** for core navigate or ACL (reuse `PSFolderProperties` / `PSFolderPermission` / `PSObjectAcl`)
- **Testing**: **Playwright + TestNG** (`modules/perc-qa-automation/frontend/tests/`) for E2E against the live CMS; **Vitest + Testing Library** (`WebUI/src/test/ts/`) for component logic with mocked API; axe-core a11y gate (T082a); `./mvnw` for the full mvn build (JDK 21); cross-platform path rules for any Java/scripts. Vitest alone does not satisfy FR-023 for SC-001..SC-011; Playwright is required for the live-CMS acceptance loop.
- **Target Platform**: Windows, Linux, macOS product builds (dev CMS currently Linux only)
- **Project Type**: Hybrid J2EE WAR + modern frontend (island mount via `PercModernUI`)
- **Performance Goals**: SC-005 — folder with ≥500 children: select folder → list usable and open item within **10s** on standard office network. Strategy: use `paginatedFolder` for **server-side pagination** (avoid loading full child set); apply **client-side virtualization** for the rendered list. Fixture (≥500 children, single folder, content type) created via `scripts/create-large-folder-fixture.sh`; measurement method (p95 from folder-select → first item open) defined in `quickstart.md` Scenario B and asserted by Playwright spec `tests/us1-perf-sc005.spec.js`.
- **Constraints**:
  - Constitution: no Spring Boot; safe modernization; TMX for user chrome
  - Spec clarifications: hard cut per phase; core-navigate **intermediate** gate for Finder **and** Desktop CE; independent host hard-cuts; advanced CE **in-matrix**; **8.2 GA blocked by functional parity** (FR-029 / SC-012)
  - Do not introduce browser→SOAP permanent dependency; CE SOAP is reference-only
  - Full jQuery WebUI retirement out of scope except Finder exclusive surface after hard cut
  - **[Issue #1387 FolderAdaptor ClassCastException](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) MAY block tasks that hit the affected REST endpoints** (`/rest/folders/by-path/...`, `/rest/items/...`). Mitigations: mock the API in Vitest; flip Playwright `test.skip` → `test(...)` on fix; if a Phase 3+ task cannot proceed past the bug, escalate into this feature's scope so the fix lands as part of 8.2.
  - **[Issue #1388 MySQL/collation install bug](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) is NOT a blocker** — UI development runs against Derby in the dev runtime. MySQL is deferred.
- **Scale/Impact**: All Web Management content editors; Desktop CE users; dialog hosts (AA, editors) over independent phases **all before 8.2 GA**; install/upgrade primarily web UI + docs/distribution for CE client
- **Target release**: **8.2** — incomplete functional parity **blocks** the release

## Constitution Check

*Gate evaluation before research / after design.*

- [x] **I. Module-First Boundaries** — WebUI + sitemanage (+ rest for menus later); AGENTS applied; **`modules/perc-qa-automation/` is the new E2E test module** with its own `AGENTS.md` (QA-automation agent profile)
- [x] **II. Evidence Over Invention** — Cites `PSPathService`, `PSFolderProperties`, `PercPathService.js`, `webmgt.jsp` Finder include, `ActionMenuResource`, `PercModernUI` / `registry.ts`, Desktop CE classes as UX inventory only; **Playwright specs probe the live CMS REST surface** so evidence comes from real responses, not invented payloads
- [x] **III. Test Discipline** — **Two-layer test strategy:** (a) Vitest + Testing Library for component-level logic (tree expand, list pagination, ReducedAction confirm, lockout warning) with mocked API; (b) **Playwright + TestNG in `modules/perc-qa-automation/`** for E2E against the live docker dev CMS — **one spec per US, all SC-001..SC-011 assertions run via Playwright** against `http://localhost:9992`. Replace legacy-only Finder automation when hard-cut; expand as phases land. Any new sitemanage / `rest` façade must add a service-contract integration test (T052a) runnable via `./mvnw` on JDK 21. axe-core a11y gate (T082a) per US1/US2/US3 component spec.
- [x] **IV. Contract & Integration Integrity** — Prefer existing path/search/folder property REST; document gaps before new endpoints; no `.ppkg`/schema break for core; **REST contracts are asserted by Playwright specs hitting the live CMS** (login + folder children + item search)
- [x] **V. Safe Modernization** — Track B React islands; no Spring Boot; no full SPA rewrite of all WebUI
- [x] **VI. Security by Default** — Server-side folder permission enforcement remains authoritative; UI hides/disables; ACL save reuses `saveFolderProperties`; CSRF on mutating calls. Any new endpoint must add a threat-model note (T052b) covering zip-slip / XXE / CSRF / server-side AuthZ, and no secrets logged. **Playwright specs use Basic auth via `RX_USEBASICAUTH: true` header** (matches the 8.2 CMS REST auth contract) and read Admin/Editor/Contributor passwords from `/opt/Percussion/var/config/generated/passwords` (host bind-mount, not argv).
- [x] **VII. Build & Dependency Hygiene** — Vite + Maven frontend; JDK 21 via Maven wrapper; **`modules/perc-qa-automation/pom.xml` adds `frontend-maven-plugin` to install Node + Playwright**, runs `npm ci` in `generate-resources`; testng 6.9.6 explicit (parent pom doesn't manage it on `development`)
- [x] **VIII. Documentation & Operability** — Feature contracts, cutover inventory, capability matrix, TMX keys; **Playwright README updates** (`modules/perc-qa-automation/README.md`) document the docker runtime + auto-discovery flow
- [x] **IX. PR Review Comment Resolution** — Applies to each story PR; explicit per-PR subtasks (T027, T029a, T045a–T045f, T047, T057, T064, T070, T081) require inline reply with mitigation commit hash AND `gh api graphql resolveReviewThread` for every review thread
- [x] **Complexity Budget** — No new top-level product module; optional thin REST adapters justified only by proven gaps (Complexity Tracking). `modules/perc-qa-automation/` is a *test* module (no production code), not counted against the product complexity budget.

**Post-design re-check**: Pass. Prefer **one PR train per phase/story** on the feature branch (US1 core explorer → US6 hard cut → US2 browser → hosts → US3/4/5 → US7 matrix rows). **Each US is paired with a Playwright spec** that runs against the live docker dev CMS in CI. US6 intermediate hard cut only after SC-001/SC-005/SC-006 (and SC-007 when CE retired). **8.2 GA** requires full matrix Done and SC-012 (functional parity blocks release). Known upstream REST bug ([issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387)) is documented as `test.skip` in Playwright specs; if a Phase 3+ task cannot proceed past it, the fix is escalated into this feature scope.

## Project Structure

### Documentation (this feature)

```text
specs/992-react-content-explorer/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── path-api.md                 # sitemanage path REST used by explorer
│   ├── content-browser-host.md     # host integration contract (US2)
│   ├── action-menu-api.md          # public REST actions (US3)
│   └── capability-matrix.md        # CE/Finder capability phases (normative inventory)
├── checklists/
│   ├── requirements.md
│   └── cutover-inventory.md        # Finder touchpoints + CE retirement (FR-022)
├── spec.md
└── tasks.md                        # produced by /speckit-tasks
```

### Source Code (affected paths)

```text
WebUI/
├── src/main/ts/
│   ├── api/
│   │   ├── paths.ts                # EXTEND: pathmanagement + search URLs
│   │   ├── client.ts               # REUSE CSRF fetch
│   │   └── contentExplorer/        # NEW: typed path/folder/browser API wrappers
│   ├── i18n/                       # REUSE thin I18N.message wrapper
│   ├── bridge.ts / registry.ts     # register ContentExplorerShell, ContentBrowser
│   ├── contentExplorer/            # NEW: shell, tree, detail list, reduced actions
│   └── contentBrowser/             # NEW: embeddable navigate/search/select
├── src/main/webapp/cm/app/
│   ├── webmgt.jsp                  # hard-cut: replace Finder include with modern mount / layout
│   ├── includes/finder.jsp         # DELETE or stop including after Finder hard cut
│   ├── includes/finder_js.jsp      # DELETE or stop including after Finder hard cut
│   └── <optional explorer shell>.jsp  # if explorer is a dedicated view vs panel in editor
├── war/widgets/perc_finder*.js     # retire from production entry after hard cut (inventory)
└── src/test/ts/contentExplorer/    # NEW Vitest
    contentBrowser/

projects/sitemanage/
└── pathmanagement/…                # REUSE; only change if contract gaps proven

rest/
└── actions/ActionMenuResource.java # REUSE for US3; verify authz + payload fit

modules/DesktopContentExplorer/     # no feature work; distribution/docs retirement in US6
modules/perc-i18n/…/CmsUi.tmx       # add perc.ui.explorer.* / browser keys
```

**Structure decision**: Track B **island pattern** — React apps registered in `registry.ts`, mounted via `PercModernUI.mount` from Web Management shell. Explorer replaces the Finder panel region in `webmgt.jsp` (or successor layout) rather than a second desktop runtime. Content browser is a **second registered component** with a documented host props contract (open as dialog or inline). Shared path API module feeds both explorer and browser (FR-009).

## Implementation Phases (design → tasks)

### Phase A — Foundations

1. Seed [capability-matrix.md](./contracts/capability-matrix.md) and [cutover-inventory.md](./checklists/cutover-inventory.md) from Finder + CE inventory.
2. Extend `api/paths.ts` + `api/contentExplorer/*` for path/folder/paginated/move/rename/delete/create typed clients.
3. Register `ContentExplorerShell` and `ContentBrowser` in `registry.ts`.
4. TMX key plan for chrome (tree, list, reduced actions, errors).
5. Vitest harness patterns aligned with Home/Dashboard tests.
6. **Bring up docker dev runtime** (T012f): host-side install to `/opt/Percussion` via `scripts/install-cms-dev.py`; container via `docker compose up -d cms-dts`. Verifies CMS reachable at `http://localhost:9992/Rhythmyx/login`.
7. **Bring up Playwright** (T012g): `cd modules/perc-qa-automation/frontend && npm ci && npx playwright install chromium`. Verifies `tests/login.spec.js` passes against the live CMS.

### Phase B — US1 Core explorer (P1) — **hard-cut gate**

1. Tree + detail list UI; load roots/children via `findChildren` / `paginatedFolder`.
2. Open/preview using existing product navigation patterns (path/id → editor).
3. **ReducedAction set** (FR-010a): create folder, rename, copy/move (via `moveItem` + server semantics), delete with confirm.
4. Permission-aware empty/error states; session/CSRF handling.
5. Performance: pagination + virtualization for SC-005.
6. Vitest: tree navigation, list load, action happy/error paths (mocked API).
7. **Playwright spec `tests/us1-core-explorer.spec.js`** against live CMS: login → mount ContentExplorerShell → assert explorer placeholder → drive tree/list/ReducedAction flows. Asserts SC-001. The folder-by-path REST is currently affected by [issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387); the spec covers the mount + chrome path with `test.skip` on the folder-by-path assertion until the bug lands.

### Phase C — US6 Finder (+ optional Desktop CE) hard cut (P3)

1. Complete cutover inventory for Finder production entry points (`webmgt.jsp`, includes, `$.perc_finder` callers for **primary nav only**).
2. Hard-cut primary nav: no miller-column Finder as supported production path.
3. Desktop CE: docs/distribution deprecate/remove for ordinary admin when same bar met (same or sequential release).
4. Deep-link map known URLs → modern explorer; unavailable message for unknown.
5. Replace legacy-only tests for retired surfaces.
6. **Playwright spec `tests/us6-hard-cut.spec.js`** against live CMS: navigate to primary content entry → assert no miller Finder chrome → sign-off check. Asserts SC-006.

### Phase D — US2 Reusable content browser (P1, independent of hard cut)

1. Embeddable component: navigate, optional search, single/multi select, filters, confirm/cancel.
2. Host contract doc + TypeScript props; pilot one host (prefer a low-risk dialog or Home Library if 989 ready).
3. Host hard-cuts **independently** (FR-008a).
4. **Per-host Playwright specs** (`tests/host-asset-picker.spec.js`, `host-page-picker.spec.js`, `host-aa-contentbrowser-dialog.spec.js`, `host-folder-picker.spec.js`, optional `host-home-library.spec.js`) — one spec per host asserting that host's dialog flow completes navigate → select → confirm and the host receives a valid `SelectionResult`. Asserts SC-002 per host.

### Phase E — US3 Menus, US4 ACL, US5 Search (P2, post-cutover capability)

1. US3: consume `ActionMenuResource` / adaptor; context menu + toolbar; keyboard access.
2. US4: folder properties/security UI on `folderProperties` + `saveFolderProperties`; lockout warning.
3. US5: explorer + browser search via searchmanagement; open/reveal in tree.
4. **Playwright spec `tests/us3-menus.spec.js`** — drives a checklist of ≥10 high-value actions (open, edit, force check-in, transition, properties, copy/move/delete, etc., enumerated in `contracts/capability-matrix.md` P-Menu). Asserts SC-003.
5. **Playwright spec `tests/us4-acl.spec.js`** — admin edits a folder's permission/ACL and a second user session refreshes; asserts SC-004.
6. **Playwright spec `tests/us5-search.spec.js`** — search from explorer and browser; open/reveal. Asserts SC-005 (combined with US1 perf spec).

### Phase F — US7 Advanced CE matrix (P3, post-cutover)

1. Clipboard multi-item copy/paste.
2. Site/subfolder copy wizards (reuse sitemanage site copy services where present).
3. Dependency viewer + IA/relationship views.
4. SC-011: all matrix rows Done or scheduled with owners.
5. **Playwright spec `tests/us7-advanced.spec.js`** — one assertion per P-Adv row (clipboard, site copy, subfolder copy, dependency, IA/relationship). Asserts SC-011.

## Complexity Tracking

|                                                                                           Violation / stretch                                                                                            |                                                                                                                                                                                                                                                                                               Justification & alternatives                                                                                                                                                                                                                                                                                                |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Possible new REST for action **execution** (not only menu listing) if public `/actions` only returns definitions                                                                                         | Prefer wrapping existing itemmanagement/content endpoints first; add thin sitemanage façade only if CE action URLs cannot be invoked safely from browser. **Any new REST or façade endpoint must come with (a) a service-contract test under `rest/src/test/java` (REST resource) or `projects/sitemanage/src/test/java` (façade) per constitution III/IV, runnable via `./mvnw -pl <module> -am test` on JDK 21; (b) a threat-model note (zip-slip, XXE, CSRF, server-side AuthZ) per constitution VI, recorded in PR evidence under `docs/ai-generated/release/992-8.2-parity-evidence.md` (or PR description).** |
| Finder embedded deeply via `$.perc_finder()`                                                                                                                                                             | Hard cut is **primary nav** first; remaining callers inventoried and migrated in host phases or temporary adapters—do not expand dual UI.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Upstream REST bug [issue #1387 FolderAdaptor ClassCastException](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) returns **500** on `/rest/folders/by-path/...` and `/rest/items/...` | Mitigations: (a) Vitest specs mock the API and pass independently of the bug; (b) Playwright specs document the failure with `test.skip(...)` + `BUG:` note; flipping `test.skip` → `test(...)` is the SC-008 evidence when the fix lands. **If a Phase 3+ task cannot proceed past this bug, escalate the fix into this feature's scope** so the fix is part of the 8.2 delivery.                                                                                                                                                                                                                                        |
| MySQL install blocked by [issue #1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) (collation bug at `PSX_DISPLAYFORMATPROPERTY_VIEW`)                                            | **Not a blocker** for UI development. The dev runtime uses Derby. The UI exercises REST contracts only; DB type is invisible to the React app. MySQL mode is deferred until the connector + collation fix lands.                                                                                                                                                                                                                                                                                                                                                                                                          |
| `modules/perc-qa-automation/` adds a Maven module                                                                                                                                                        | Test-only module (no production code). Required for the SC-001..SC-011 automated acceptance loop. Counts as test infrastructure, not product complexity.                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |

## Artifacts

|                                 Artifact                                 |                Purpose                |
|--------------------------------------------------------------------------|---------------------------------------|
| [research.md](./research.md)                                             | Decisions: stack, APIs, cutover, gaps |
| [data-model.md](./data-model.md)                                         | Client/server entities                |
| [contracts/path-api.md](./contracts/path-api.md)                         | Path REST contract                    |
| [contracts/content-browser-host.md](./contracts/content-browser-host.md) | Host embed contract                   |
| [contracts/action-menu-api.md](./contracts/action-menu-api.md)           | Menu REST for US3                     |
| [contracts/capability-matrix.md](./contracts/capability-matrix.md)       | Phased CE/Finder capabilities         |
| [quickstart.md](./quickstart.md)                                         | Validation scenarios                  |
| [checklists/cutover-inventory.md](./checklists/cutover-inventory.md)     | FR-022 inventory seed                 |

