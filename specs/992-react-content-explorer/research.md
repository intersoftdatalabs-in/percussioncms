# Research: Unified React Content Explorer

**Feature**: `992-react-content-explorer`  
**Date**: 2026-07-19  
**Status**: Phase 0 complete — open questions resolved for planning

## R1 — UI stack and mount pattern

**Decision**: Track B React 19 + TypeScript via existing Vite bundle and `window.PercModernUI.mount` / `registry.ts` (same as Dashboard, HomeShell, WidgetBuilderApp).

**Rationale**: Constitution and `WebUI/AGENTS.md` mandate Track B for strategic UI; bridge pattern already ships; avoids second SPA framework.

**Alternatives considered**:
- Rewrite Finder in jQuery only — does not meet CE-model or long-term consolidation goals.
- Full SPA replacement of all Web Management — out of scope; island mount is incremental and safer.
- JavaFX/web dual shell — rejected by spec (web replacement only).

## R2 — Navigation paradigm

**Decision**: Explorer-style **tree + detail list** as default; miller columns not retained as primary UX.

**Rationale**: Spec problem statement and clarifications; CE mental model preferred by users.

**Alternatives considered**: Keep miller columns as optional mode — deferred indefinitely; not in MVP or hard-cut bar.

## R3 — Server APIs for core navigate (hard-cut bar)

**Decision**: Reuse **sitemanage path management REST** as system of record for core navigate:

|     Capability     |                                 Evidence                                  |
|--------------------|---------------------------------------------------------------------------|
| Children / list    | `GET …/pathmanagement/path/folder/{path}`, `GET …/paginatedFolder/{path}` |
| Item by path/id    | `GET …/item/{path}`, `GET …/item/id/{id}`                                 |
| Add folder         | `GET …/addFolder|addNewFolder/{path}`                                     |
| Rename             | `POST …/renameFolder`                                                     |
| Move               | `POST …/moveItem`                                                         |
| Delete             | path delete endpoints used by `PercPathService.js`                        |
| Folder props / ACL | `GET …/folderProperties/{id}`, `POST …/saveFolderProperties`              |
| Search             | `…/searchmanagement/search/get` and `/get/extendedresults`                |

Client reference: `WebUI/war/services/PercPathService.js`, modern roots in `WebUI/src/main/ts/api/paths.ts`.

**Rationale**: Finder already uses these contracts; sufficient for US1 reduced actions without SOAP.

**Alternatives considered**:
- Browser → Desktop CE SOAP (`FindFolderChildren`, etc.) — rejected (not web-native; dual protocol).
- New parallel path API — unnecessary for core; only if proven gap.

## R4 — Folder ACL / permissions

**Decision**: US4 (post-cutover) uses `PSFolderProperties` + `PSFolderPermission` (+ ACL object) via existing save/find folder properties; UI mirrors CE security panel behavior (lockout warning client-side + server enforcement).

**Rationale**: Data model already on REST; CE Swing (`PSFolderSecurityPanel`) is UX reference only.

**Alternatives considered**: Redesign permission schema — out of scope per spec.

## R5 — Action menus (US3)

**Decision**: Prefer public REST `ActionMenuResource` (`@Path("/actions")` in `rest` module) for **menu discovery** (allowed actions/transitions/types). **Execute** actions via existing item/content/path REST or documented action URLs—not raw CE applet code. If execution gap remains after inventory, add thin sitemanage façade (Complexity Tracking).

**Rationale**: Product already exposes action menu REST; CE loads menus from server action config (`sys_ActionPage`, action manager)—web must not hardcode only Finder buttons long-term.

**Alternatives considered**:
- Fixed button set forever — fails FR-010 long-term.
- Port CE Swing menu builder to browser as-is — high cost; REST adaptor is cleaner.

**Hard-cut note**: At Finder hard cut, **ReducedAction set** only (FR-010a); full menus post-cutover.

## R6 — Cutover strategy

**Decision** (from clarify + release lock):
1. **Hard cut per phase** — no production dual primary path once phase ships.
2. **Finder + Desktop CE intermediate gate** = core navigate only (same bar) — for ordered work *within* the train.
3. **Host browser migrations** independent hard-cuts within the train.
4. Advanced CE tools **in this feature** as matrix phases after intermediate retirement.
5. **Target product release = 8.2.** All in-scope work is 8.2 scope. **Functional parity blocks 8.2 GA** (spec FR-029 / SC-012). Phases are **not** deferred to post-8.2 product releases.

**Rationale**: User-locked clarifications plus product release constraint; ordered engineering without shipping incomplete 8.2.

**Alternatives considered**: Time-boxed dual path; CE retirement only after full menus/ACL as *intermediate* gate — rejected in clarify. Multi-release deferral of parity — **rejected** by 8.2 lock.

## R7 — Where Finder lives today

**Decision**: Primary hard-cut surface is Web Management editor shell:

- `WebUI/.../webmgt.jsp` includes `finder.jsp` / `finder_js.jsp`, initializes `$.Percussion.PercFinderView()`
- Widgets: `perc_finder.js`, `PercFinderTree.js`, `PercFinderListView`, `PercFinderView.js`
- Many plugins call `$.perc_finder()` — inventory in cutover checklist; primary nav hard cut ≠ instant deletion of every call site if hosts still need adapters until their phase

**Rationale**: Evidence from source; inventory-driven retirement avoids big-bang of entire jQuery WebUI.

## R8 — Content browser hosts

**Decision**: Ship reusable React `ContentBrowser` with typed host contract; migrate hosts independently. Known legacy: Dojo/AA `ContentBrowserDialog.jsp`, Finder-based pickers, asset browser widgets. Prefer modern mount or temporary adapter that does not reintroduce miller columns for hard-cut hosts.

**Rationale**: Spec US2 + FR-008a; Track A Dojo work is separate but hosts may dual-track carefully.

## R9 — i18n

**Decision**: TMX via existing `tmx.jsp` + `I18N.message` (or Home’s thin TS wrapper); new keys under `perc.ui.explorer.*` / `perc.ui.contentBrowser.*` in `CmsUi.tmx` with structural locale parity.

**Rationale**: Matches 989 and constitution VIII.

## R10 — Testing strategy

**Decision**:
- Vitest unit/component tests for tree/list/actions/browser selection (mocked fetch).
- Manual UAT scripts for SC-001, SC-005, SC-006/007 hard cuts.
- Expand FR-023 automated coverage as phases land; no permanent skip of replaced Finder-only tests after hard cut.
- Service contract tests only if REST contracts change.

## R11 — Desktop CE module fate

**Decision**: No feature development in Swing/JavaFX. After hard cut, update install docs/distribution so ordinary content admin does not require CE; module may remain in repo until packaging cleanup task (matrix row).

**Rationale**: Spec retirement = web replacement; avoiding large binary delete in first hard-cut PR reduces risk—inventory tracks packaging.

## Open items deferred to implementation (not blocking plan)

|                         Item                          |                       Handling                        |
|-------------------------------------------------------|-------------------------------------------------------|
| Exact action **execution** map (action id → REST/URL) | Inventory during US3; gap → façade                    |
| Concurrent ACL save policy                            | Server last-write / validation; surface server errors |
| Home Library adoption of ContentBrowser               | Coordinate with 989; non-blocking for US1             |
| Display format full column parity                     | FR-027 matrix follow-on                               |
| Site copy wizard REST completeness                    | US7 matrix research spike                             |

## R12 — Security surface (T011 — implementer note, 2026-07-19)

Findings from the analyzer session while scaffolding Phase 2. Carry into US1 implementation and per-PR threat-model notes:

- **CSRF**: All mutating path calls go through `api/client.ts` `apiFetch`, which already attaches the OWASP CSRFGuard token header. **No secret, password, or token may be logged.** (Constitution VI.)
- **Session expiry**: `apiFetch` should treat 401 as re-login UX (TMX key `perc.ui.explorer.sessionExpired` — add in T023). Avoid blank panels.
- **Folder AuthZ**: Server is authoritative. UI MUST NOT enable actions the server will reject. The ReducedAction set (FR-010a) calls `delete` / `move` / `rename` / `copy` which require folder WRITE or ADMIN — gate visibility on `PSPathItem.accessLevel`.
- **ACL save**: `saveFolderProperties` (FR-014 / FR-015 / FR-016) — UI warns before save if current user would lose access; server remains authoritative. Lockout-self detection belongs in client (`aclLockout.test.ts`, T058).
- **No new REST / façade needed for core navigate or ACL** — reusing `PSPathService` + `PSFolderProperties` is sufficient. T052a/T052b only fire if US3 / US7 action execution proves a gap.
- **Path encoding**: `pathApi.ts` `encodePath` encodes each `/`-separated segment to keep the JAX-RS `{path:.*}` pattern intact and to avoid path-traversal mistakes on multi-segment paths.
- **Cross-platform**: No new Java I/O or shell-only paths in this feature. Server-side work only if T012d evaluates a `system/` task (none added for 8.2).

## References (repo)

- `projects/sitemanage/.../PSPathService.java`
- `projects/sitemanage/.../PSFolderProperties.java`, `PSFolderPermission.java`, `PSPathItem.java`
- `WebUI/war/services/PercPathService.js`
- `WebUI/src/main/ts/api/paths.ts`, `registry.ts`, `bridge.ts`
- `WebUI/src/main/webapp/cm/app/webmgt.jsp`, `includes/finder*.jsp`
- `rest/src/main/java/com/percussion/rest/actions/ActionMenuResource.java`
- `modules/DesktopContentExplorer/...` (UX inventory)
- `docs/ai-generated/tasks/#000-unified-ui-plan/`

