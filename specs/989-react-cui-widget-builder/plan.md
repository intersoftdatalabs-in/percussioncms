# Implementation Plan: Migrate Home/Contributor UI and Widget Builder to Modern UI

**Branch**: `989-react-cui-widget-builder` | **Date**: 2026-07-17 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/989-react-cui-widget-builder/spec.md`

## Summary

Replace the legacy **Home / Contributor UI** (CUI iframe + Knockout/RequireJS under `cm/cui`) and **Widget Builder** (Backbone/jQuery packs under `cm/widgetbuilder`) with **Track B React + TypeScript** surfaces mounted via the existing `PercModernUI` bridge. Reuse **existing sitemanage REST** (contributor/content flows and `PSWidgetBuilderService` under `/widgetmanagement/widgetbuilder/*`). Ship as a **big-bang same-release cutover**: modern-only paths, **hard-delete** classic entry JSPs (no rewrite/redirect stubs), rewire `index.jsp` view map and nav, replace legacy-only client tests, and complete US3 removal using a **manual** durable inventory (`checklists/removal-inventory.md`) including **proven orphan** vendors (Backbone/Underscore/Backgrid when unused).

## Technical Context

- **Language/Version**: Java 21 (branch `development` / feature off development); TypeScript + React 19 (WebUI Vite pipeline)
- **Owning Module(s)**: `WebUI/` (primary UI); `projects/sitemanage/` (Widget Builder service + content REST—**no parallel server rewrite**); `modules/perc-i18n/` for TMX catalog updates (`CmsUi.tmx`)
- **AGENTS Hierarchy**: root `AGENTS.md`, `WebUI/AGENTS.md` (Track B bridge, Vite, test discipline)
- **Dependencies & Storage**: Existing REST/JSON (Fetch + CSRF via `WebUI/src/main/ts/api/client.ts`); session cookies same-origin; no new DB schema; Widget Builder enablement remains `WidgetBuilderActive` / `PSWidgetBuilderService.isWidgetBuilderEnabled()`
- **i18n**: Product **TMX** tables (FR-021–024). Runtime: shell loads `/Rhythmyx/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…`; React resolves via `I18N.message` or thin TS wrapper. New keys: structural locale parity (en-us + peer langs). Proof: **SC-008** manual key-presence checklist (`checklists/i18n-key-checklist.md`)—not required multi-locale Vitest.
- **Testing**: Vitest + Testing Library for React (`WebUI/src/test/ts`); replace `WebUI/src/test/js/percWidget*.test.js` legacy WB tests; optional service contract tests in sitemanage only if API surface changes (not expected); `./mvnw` / WebUI frontend test goals
- **Target Platform**: Windows, Linux, macOS product builds (portable paths in any new Java/scripts)
- **Project Type**: Hybrid J2EE WAR + modern frontend monorepo module
- **Performance Goals**: Home primary sections interactive under typical admin LAN; no new NFR beyond “parity with modern Dashboard patterns”
- **Constraints**: Constitution: no Spring Boot; FR-008 big-bang; FR-017 hard-cut JSPs; FR-016/019 manual orphan inventory; FR-021–023 TMX (not English-only React hardcoding); full jQuery product retirement out of scope; Dashboard gadget dual-path cleanup out of scope except shared nav/shell mount patterns
- **Scale/Impact**: All users of Home; admins/integrators with Widget Builder; install/upgrade web UI only

## Constitution Check

*Gate evaluation before research / after design. All must pass or be justified in Complexity Tracking.*

- [x] **I. Module-First Boundaries** — WebUI + sitemanage only; AGENTS applied
- [x] **II. Evidence Over Invention** — Cites `PercModernUI`, `registry.ts`, `PSWidgetBuilderService`, `index.jsp` views map, `perc_path_constants` WB URLs, CUI adaptor surface
- [x] **III. Test Discipline** — FR-011/018: modern UI and/or service-contract tests; legacy-only client tests removed same release
- [x] **IV. Contract & Integration Integrity** — Reuse existing widgetbuilder REST shapes; no `.ppkg`/schema break
- [x] **V. Safe Modernization** — Track B React only; no Spring Boot; localized UI swap
- [x] **VI. Security by Default** — CSRF via existing client; AuthZ via existing nav/admin views and WB enablement
- [x] **VII. Build & Dependency Hygiene** — Vite + Maven frontend plugin; JDK 21 via Maven wrapper
- [x] **VIII. Documentation & Operability** — Feature docs + removal inventory; **TMX-backed** Home/WB strings (reuse/add keys in `CmsUi.tmx`, `tmx.jsp` + `I18N.message`, SC-008 checklist)
- [x] **IX. PR Review Comment Resolution** — Applies to each story PR
- [x] **Complexity Budget** — No new top-level modules; no unjustified contract breaks

**Post-design re-check**: Still pass. Story checkpoint (constitution workflow) → prefer **one PR per user story** (US1 → US2 → US3) on the feature branch stack; release must contain all three (big-bang).

## Project Structure

### Documentation (this feature)

```text
specs/989-react-cui-widget-builder/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── home-deep-links.md
│   └── widget-builder-api.md
├── checklists/
│   ├── requirements.md
│   ├── removal-inventory.md
│   └── i18n-key-checklist.md        # SC-008 key-presence review
├── spec.md
└── tasks.md              # produced by /speckit-tasks
```

### Source Code (affected paths)

```text
modules/perc-i18n/
└── src/main/resources/i18n/CmsUi.tmx   # reuse + add perc.ui.* keys (FR-021/022)

WebUI/
├── src/main/ts/
│   ├── api/                         # existing client + csrf; add home/ + widgetbuilder/ API modules
│   ├── i18n/                        # NEW: thin wrapper over window I18N.message (FR-023)
│   ├── bridge.ts / registry.ts      # register HomeShell, WidgetBuilderApp
│   ├── home/                        # NEW: shell, sections Recent/Library/Search/Create
│   └── widgetbuilder/               # NEW: list/edit/validate/deploy UI
├── src/main/webapp/cm/
│   ├── app/index.jsp                # rewire views: home + widgetbuilder → new thin shells
│   ├── app/<new-home-shell>.jsp     # NEW: CSRF + tmx.jsp + modern bundle + PercModernUI.mount
│   ├── app/<new-wb-shell>.jsp       # NEW: same pattern for Widget Builder
│   ├── app/home.jsp                 # DELETE (US3 hard cut)
│   ├── app/widgetBuilder.jsp        # DELETE (US3 hard cut)
│   ├── pages/app/*                  # mirror paths: same rewire/delete as app/
│   ├── cui/ , pages/cui/            # DELETE exclusive CUI SPA (US3)
│   ├── widgetbuilder/               # DELETE exclusive WB client (US3)
│   └── app/widgetbuilder/           # DELETE if duplicate tree
├── src/test/ts/home/                # NEW Vitest coverage
├── src/test/ts/widgetbuilder/       # NEW Vitest coverage
└── src/test/js/percWidget*.test.js  # REMOVE with US3 (replaced)

projects/sitemanage/
└── .../widgetbuilder/service/       # REUSE only unless bugfix required for contracts
```

**Structure decision**: Follow WebUI Track B island pattern—**thin JSP shell + Vite `/cm/modern/` bundle + `PercModernUI.mount`**—not a full SPA takeover of all Web Management. Home is one registered React app with **client-side section routes** (Recent / Library / Search / Create). Widget Builder is a second registered React app. Classic JSP filenames are **not** reused (FR-017); view keys `home` / `widgetbuilder` may remain for nav stability while pointing at new shell JSPs.

## Implementation Phases (design → tasks)

### Phase A — Foundations (shared)

1. Scaffold `specs/.../checklists/removal-inventory.md` seed list (exclusive trees, candidates, tests).
2. Scaffold `specs/.../checklists/i18n-key-checklist.md` for SC-008.
3. Deep-link map table (contracts) implemented as thin server/view mapping without classic JSP stubs.
4. API TypeScript modules wrapping existing REST (typed DTOs aligned to data-model).
5. Thin TypeScript i18n helper delegating to `I18N.message` (FR-023).
6. Register `HomeShell` and `WidgetBuilderApp` in `registry.ts`; ensure Vite entry includes them.

### Phase B — US1 Modern Home (P1)

1. New Home shell JSP(s) with **tmx.jsp** + CSRF + modern bundle; rewire `index.jsp` `views.put("home", ...)`.
2. React shell with section navigation; map `initialScreen` query (library, list, search, newitem) → sections.
3. Implement sections against existing content REST (via patterns in `PercContributorUiAdaptor` / sitemanage)—**not** parent iframe `jQuery` adaptor after cutover.
4. Library: site/folder browse + open item (contributor parity).
5. Create: **full capability matrix** ([contracts/home-capability-matrix.md](./contracts/home-capability-matrix.md) §6)—type chooser + page/asset/blog wizards equal to CUI (not free-text-only forms); open/locate after create.
6. Open item from Recent/Library/Search via product path-based navigation (FR-001b).
7. Empty states, session/CSRF; **all user-visible chrome via TMX keys** (reuse/add in `CmsUi.tmx`, FR-021/022).
8. Vitest for shell routing, API error handling, wizard state machines, critical section smoke (i18n proof is SC-008, not multi-locale Vitest).
9. **Do not ship Home cutover as acceptance-complete** until matrix §6 MUST rows are Done (architecture-only MVP is insufficient for FR-001).

### Phase C — US2 Modern Widget Builder (P1)

1. New WB shell JSP(s) with **tmx.jsp** + CSRF + modern bundle; rewire `views.put("widgetbuilder", ...)`.
2. List summaries; create/edit definition (metadata, fields, display HTML, JS/CSS resources).
3. Validate + save via existing POST endpoints; last-write-wins UX (confirm + reload truth).
4. Deploy/package via existing deploy endpoint; respect enablement + admin/designer view gates.
5. **TMX keys** for WB chrome (reuse/add in `CmsUi.tmx`); wire via i18n helper.
6. Vitest for form validation display, save success, disabled/enabled gate.

### Phase D — US3 Removal (P2, same release)

1. Delete exclusive trees: `cm/cui`, `cm/pages/cui`, `cm/widgetbuilder`, `cm/app/widgetbuilder`, classic `home.jsp` / `widgetBuilder.jsp` (and pages mirrors), packed `perc_widgetBuilder.packed.min.*` references, CUI-local vendors.
2. Remove dead includes and build packaging entries for WB packed client if any.
3. Complete manual orphan inventory: Backbone / Underscore / Backgrid / RequireJS (global) only if **zero** remaining consumers; keep shared jQuery and `perc_widget_library` (still used by `webmgt` / `editAsset`).
4. Delete legacy-only JS unit tests; ensure modern suite green.
5. Sign off `removal-inventory.md` in PR/release review (manual gate; no CI absence-scan hard gate).
6. Smoke: main nav Home, Dashboard, WB (when enabled).

### Phase E — Verification

- UAT checklists: SC-001, SC-002, SC-005–007.
- **i18n key-presence checklist (SC-008 / FR-024)** signed in PR review.
- Inventory sign-off: SC-003.
- CI: SC-004 / FR-011.

## Complexity Tracking

No constitution violations requiring exceptions.

|                           Risk                            |                                                                                                 Mitigation                                                                                                 |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Home create wizards depend on many adaptor paths          | Follow [home-capability-matrix.md](./contracts/home-capability-matrix.md); port `PercContributorUiAdaptor` operations; P0 = §6 MUST rows before acceptance                                                 |
| Losing classic Home function while only modernizing stack | Treat matrix MUST as non-negotiable; redesign is stack/look, not a thinner product                                                                                                                         |
| Hard-cut JSP names break bookmarks                        | FR-013: map known URLs at dispatcher/nav level without stubs                                                                                                                                               |
| Dual trees `app/` vs `pages/`                             | Treat both as production-relevant; rewire/delete both in US3 inventory                                                                                                                                     |
| `war/` copies                                             | Generated/synced artifacts—clean via build and inventory; do not leave live CUI under war if packaged                                                                                                      |
| Story PR vs big-bang                                      | Stack US1→US2→US3 PRs on the feature branch; **do not merge US1/US2 alone to `development`/release without US3 in the same train**—classic JSPs left on disk until US3 must not ship to customers (FR-008) |
| Unmapped legacy URLs                                      | Dedicated moved/unavailable surface (FR-013); not log-only                                                                                                                                                 |
| Main-nav regression                                       | Explicit SC-005 / FR-020 smoke checklist in polish (Dashboard + non-Home tab)                                                                                                                              |

### Release gate (non-negotiable)

- **Shippable train** = US1 + US2 + US3 (inventory signed) together.
- Feature-branch intermediate state may rewire views while classic files remain until US3 deletes them.
- **Forbidden**: merge only US1 and/or US2 to a customer-facing branch while classic `home.jsp` / `widgetBuilder.jsp` / CUI / WB packs remain requestable.

## Generated design artifacts

|          Artifact           |                                                   Path                                                   |
|-----------------------------|----------------------------------------------------------------------------------------------------------|
| Research                    | [research.md](./research.md)                                                                             |
| Data model                  | [data-model.md](./data-model.md)                                                                         |
| Contracts                   | [contracts/](./contracts/) (includes [home-capability-matrix.md](./contracts/home-capability-matrix.md)) |
| Quickstart                  | [quickstart.md](./quickstart.md)                                                                         |
| Removal inventory (seed)    | [checklists/removal-inventory.md](./checklists/removal-inventory.md)                                     |
| i18n key checklist (SC-008) | [checklists/i18n-key-checklist.md](./checklists/i18n-key-checklist.md)                                   |

**Next command**: `/speckit-implement` (tasks already generated; i18n tasks added post-analyze)
