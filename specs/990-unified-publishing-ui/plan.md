# Implementation Plan: Unified Publishing UI

**Branch**: `990-unified-publishing-ui` | **Date**: 2026-07-18 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/990-unified-publishing-ui/spec.md`  
**Inventory**: [research/inventory.md](./research/inventory.md)

## Summary

Consolidate three legacy publishing UIs—(1) **Rhythmyx Publishing Design** (JSF under `ui/publishing`), (2) **Rhythmyx Publishing Runtime** (JSF under `ui/pubruntime`), and (3) **modern CMS Publish** (Minuet/jQuery under `cm` publish views)—into one **Track B React + TypeScript Publishing shell** mounted via `PercModernUI`, reusing existing **sitemanage** publish/status/server REST for ops and **system** publisher / publishing web services / site manager services for design and edition runtime.

Deliver **feature parity** with progressive disclosure for ease of use (ops first; design/runtime fully available). **Phased cutover by surface**: ship and retire Minuet Publish after ops parity; retire JSF Design and Runtime after their respective parity. Do **not** reimplement the publishing engine in the browser.

## Technical Context

- **Language/Version**: Java 21 (`development`); TypeScript + React 19 (WebUI Vite pipeline)
- **Owning Module(s)**:
  - `WebUI/` — primary UI (shell, React app, JSP view rewire, retire classic clients)
  - `projects/sitemanage/` — ops REST (`/publish`, `/pubstatus`, `/servers`) — **reuse**; extend only for documented Design/Runtime API gaps
  - `system/` — `IPSPublisherService`, `IPSRxPublisherService`, `IPSPublishingWs`, `IPSSiteManager` — **reuse** as engine/design services
  - `modules/perc-i18n/` — TMX (`CmsUi.tmx` / `perc.ui.publish.*` keys)
- **AGENTS Hierarchy**: root `AGENTS.md`, `WebUI/AGENTS.md` (Track B; Rhythmyx Publishing ~28 JSF screens marked legacy/retiring)
- **Dependencies & Storage**: Existing REST/JSON + session cookies + CSRF (`WebUI/src/main/ts/api/client.ts`); Hibernate-backed publisher/sitemgr entities unchanged unless gap forces a thin REST DTO layer (no ad-hoc schema)
- **i18n**: TMX + `tmx.jsp` + `I18N.message` / TS wrapper (same as feature 989). Reuse `perc.ui.publish.*` keys heavily; add keys for Design/Runtime chrome
- **Testing**: Vitest + Testing Library (`WebUI/src/test/ts`); Playwright E2E integration tests (`modules/perc-qa-automation/frontend/tests/publishing/`); JUnit 5 for any new Java REST adapters; cross-platform path rules; service-contract tests if adapters added
- **Target Platform**: Windows, Linux, macOS
- **Project Type**: Hybrid J2EE WAR + modern frontend island (not full SPA takeover of all Web Management)
- **Performance Goals**: Status refresh no worse than Minuet polling; site/server lists usable for typical multi-site installs; large log lists remain paged/limited as today
- **Constraints**: Constitution (no Spring Boot; evidence over invention; test discipline); FR-013 engine reuse; secrets never logged (FR-016); dual trees `cm/app` + `cm/pages/app` + packaged `war/` hygiene
- **Scale/Impact**: All publishers/admins; navigation key `VIEW_PUBLISH` / `publish`; optional deep links to `/ui/publishing/*` and `/ui/pubruntime/*`

## Constitution Check

*Gate evaluation before research / after design.*

- [x] **I. Module-First Boundaries** — WebUI primary; sitemanage/system secondary; AGENTS applied
- [x] **II. Evidence Over Invention** — Cites inventory paths, `PercPublisherService` / path constants, `PSPubServerRestService`, `PSSitePublishStatusService`, `IPSPublishingWs`, `IPSSiteManager`, JSF trees, Track B `homeModern.jsp` pattern
- [x] **III. Test Discipline** — FR-017: Vitest for UI logic; JUnit for new adapters; no change without tests
- [x] **IV. Contract & Integration Integrity** — Prefer existing REST shapes; new design REST is additive thin façade over existing services (backward compatible)
- [x] **V. Safe Modernization** — Track B React only; no Spring Boot; incremental surface cutover
- [x] **VI. Security by Default** — CSRF; AuthZ via existing publish/design roles; secrets handling FR-016
- [x] **VII. Build & Dependency Hygiene** — Vite + Maven; JDK 21 via `Maven wrapper`
- [x] **VIII. Documentation & Operability** — Feature docs, retirement inventory, TMX, diagnosable status/logs (SC-008)
- [x] **IX. PR Review Comment Resolution** — Per story PR
- [x] **Complexity Budget** — Optional new sitemanage REST package for design/runtime **only if** JSF-only operations lack JSON APIs; justified below

**Post-design re-check**: Pass. Prefer **one PR per user story** (US1→US8) on feature branch; ops cutover can land before design/runtime PRs complete, with temporary dual entry only as documented.

## Project Structure

### Documentation (this feature)

```text
specs/990-unified-publishing-ui/
├── plan.md
├── research.md
├── research/inventory.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── capability-matrix.md
│   ├── ops-publish-api.md
│   ├── design-runtime-api.md
│   └── deep-links.md
├── checklists/
│   ├── requirements.md
│   ├── removal-inventory.md          # seed at implement; complete at US8
│   └── i18n-key-checklist.md         # seed at implement
├── spec.md
└── tasks.md                          # /speckit-tasks
```

### Source Code (affected paths)

```text
modules/perc-i18n/
└── .../i18n/CmsUi.tmx                # reuse + add publish/design/runtime keys

WebUI/
├── src/main/ts/
│   ├── api/
│   │   └── publishing/               # NEW typed clients (ops + design adapters)
│   ├── publishing/                   # NEW PublishingShell + sections
│   │   ├── PublishingShell.tsx
│   │   ├── sections/                 # Sites, Servers, Status, Logs, Design, Runtime
│   │   ├── components/
│   │   └── types.ts
│   ├── bridge.ts / registry.ts       # register PublishingShell
│   └── i18n/                         # reuse thin I18N wrapper if present from 989
├── src/main/webapp/cm/
│   ├── app/index.jsp                 # views.put("publish", "publishModern.jsp")
│   ├── app/publishModern.jsp         # NEW thin shell (tmx + modern bundle + mount)
│   ├── app/publish.jsp               # RETIRE after ops cutover (US8 ops portion)
│   ├── app/js/legacy/views/PercPublish*.js  # RETIRE with Minuet publish
│   ├── app/includes/minuetPublishTemplates/ # RETIRE with Minuet publish
│   └── pages/app/*                   # mirror rewire/delete
├── src/main/webapp/ui/
│   ├── publishing/                   # JSF Design — RETIRE after US4 cutover
│   └── pubruntime/                   # JSF Runtime — RETIRE after US5 cutover
├── src/test/ts/publishing/           # NEW Vitest
└── src/main/frontend/                # Vite entries as needed for modern bundle

modules/perc-qa-automation/
└── frontend/tests/publishing/        # NEW Playwright E2E Tests

projects/sitemanage/
├── .../sitemanage/service/impl/PSSitePublish*.java   # REUSE
├── .../pubserver/impl/PSPubServer*.java              # REUSE
└── .../publishingdesign/ (OPTIONAL NEW)              # thin REST façade if gap proven

system/
├── .../services/publisher/           # REUSE
├── .../rx/publisher/                 # REUSE
├── .../services/sitemgr/             # REUSE location schemes/contexts
└── .../webservices/publishing/       # REUSE IPSPublishingWs patterns
```

**Structure decision**: Single registered React app **`PublishingShell`** with **section routes** (Sites & servers | Status | Logs | Design | Runtime/Editions). Keep nav key `publish` / `VIEW_PUBLISH`. Thin JSP `publishModern.jsp` (name final at implement) mirrors `homeModern.jsp`. No full SPA replacement of all Web Management.

## Implementation Phases (design → tasks)

### Phase A — Foundations (shared)

1. Register `PublishingShell` in `registry.ts`; Vite ensures component in modern bundle.
2. Add `publishModern.jsp` (+ pages mirror) with CSRF, `tmx.jsp`, `perc-modern-ui.js`, `PercModernUI.mount('…','PublishingShell', props)`.
3. Typed API modules for **ops** paths from `perc_path_constants.js` (see [contracts/ops-publish-api.md](./contracts/ops-publish-api.md)).
4. Scaffold section shell with nav + empty states; i18n helper.
5. Seed `checklists/removal-inventory.md` and `i18n-key-checklist.md`.
6. Capability matrix tracking file ([contracts/capability-matrix.md](./contracts/capability-matrix.md)).

### Phase B — US1 Sites & run publish (P1)

1. Site list card/list + filter (parity with Minuet templates).
2. Site workspace; wire full + incremental publish + incremental preview queue.
3. Stop job from site context where applicable.
4. Vitest: site list filter, publish action state machine, error/forbidden handling.

### Phase C — US2 Status & logs (P1)

1. Status table + polling interval parity with `PercPublishStatusMinuetView`.
2. Logs filters, details drawer/panel, purge with confirmation.
3. Vitest: poll lifecycle, purge confirmation gate, progress display helpers.

### Phase D — US3 Publish servers (P1)

1. Server list CRUD UI; default Publish Now indicator.
2. Driver property forms: Local, FTP, FTPS, SFTP, S3, DB drivers (port Minuet templates → React forms with validation).
3. Environment helpers (EC2/regions/available publishing servers) when APIs return data.
4. Vitest: validation matrix per driver; secret fields not serialized to logs.

### Phase E — Ops cutover (part of US8)

1. Rewire `index.jsp` `views.put("publish", "publishModern.jsp")`.
2. Deep-link map for classic Minuet publish URLs.
3. Stop shipping Minuet publish exclusive clients after UAT (see removal inventory).

### Phase F — US4 Design (P2)

1. **API gap analysis implementation**: expose JSON REST façade over `IPSPublishingWs` + `IPSPublisherService` + `IPSSiteManager` for design objects lacking browser-callable JSON (see [contracts/design-runtime-api.md](./contracts/design-runtime-api.md) and research R3).
2. Design section IA: Sites → Editions / Content lists / Contexts / Delivery types.
3. Editors for each design object type including legacy content lists and location schemes.
4. Copy edition from other site; scheme path browser.
5. JUnit for façade; Vitest for design navigation and save validation.

### Phase G — US5 Runtime editions (P2)

1. Edition list; start/stop via façade / existing job APIs.
2. Demand publish UI.
3. Advanced log/site-record cleanup if still product-supported and not already in Status/Logs.
4. Tests for start/stop and demand work queue.

### Phase H — US6 Item actions coherence (P2)

1. Smoke/regression on `PercItemPublisherService` paths (may remain jQuery until separate migration)—**no regression**.
2. Ensure Publishing history / status deep links open modern shell sections when applicable.
3. Contract tests or scripted API smoke for publish-now / takedown / stage.

### Phase I — US7 Ease of use polish (P2)

1. Progressive disclosure defaults (ops landing); role-aware section visibility if product roles allow.
2. Empty states, keyboard paths, focus management on primary tables/actions.
3. Short in-app task guidance (not full help rewrite).

### Phase J — US8 Full retirement (P3)

1. Complete removal inventory sign-off for Design JSF, Runtime JSF, Minuet publish trees (`app` + `pages` + packaging).
2. URL mapping for `/ui/publishing/*` and `/ui/pubruntime/*`.
3. Remove exclusive classic assets; retain shared jQuery only if other screens need it.
4. Update WebUI AGENTS UI layer table when surfaces are gone.

### Phase K — US9 Ops residual & packaging (P2, post-analyze)

Added after `/speckit-analyze` on PR #1370: baseline shell shipped with matrix drift (OPS-18/20/22/23 In progress while tasks fully checked).

1. Incremental **approval** UI → `publishIncrementalWithApproval` (OPS-18).
2. Status column **sort** (OPS-20).
3. Logs **filters** + structured **item details** (OPS-22/23).
4. Optional deep faces packaging cleanup (RET-06).
5. UAT sign-off artifacts for SC-001/003/008.

Tasks: **T116–T127** in `tasks.md`.

## Complexity Tracking

|                        Item                        |                                                                                                                                                               Justification                                                                                                                                                               |
|----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Optional new sitemanage REST for Design/Runtime    | Design is largely JSF-bound; browser cannot safely call internal Java services. Thin JSON façade over **existing** `IPSPublishingWs` / `IPSSiteManager` / `IPSRxPublisherService` is required for FR-007/008 without inventing engine logic. Prefer one coherent `/publishingdesign` or extend existing services—decision R3 in research. |
| Phased cutover (not single big-bang for all three) | Spec assumption: ops value ships earlier; full design is larger. Final state still one UI. Document temporary dual entry in release notes.                                                                                                                                                                                                |
| US9 residual after US8 tasks closed                | Honest tracking: entry-path retirement ≠ full pixel parity. Keeps matrix as gate without inventing work as already done.                                                                                                                                                                                                                  |

## PR / story checkpoint

Constitution workflow: implement, test, commit, open PR per story; monitor review bots; resolve threads before next story. Stack on `990-unified-publishing-ui` off `development`. Baseline feature PR: **#1370**; US9 may land as follow-up commits on the same PR or a stacked PR.
