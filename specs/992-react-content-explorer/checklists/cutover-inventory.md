# Cutover inventory: Finder + Desktop Content Explorer

**Feature**: [spec.md](../spec.md)  
**Purpose**: FR-022 durable inventory — PR/release sign-off per hard-cut phase  
**Target product release**: **8.2** (FR-029 / SC-012 — functional parity blocks 8.2 GA)  
**Status**: Seed (expand as work proceeds)

## Phase sign-off log

| Phase | Description | PR / release | Reviewer | Date | Result |
|-------|-------------|--------------|----------|------|--------|
| P0-Core Finder | Primary nav hard cut | TBD per phase PR (T031–T036) | TBD — module owner + QA/UAT per §E sign-off rule | TBD per phase PR | Pending (T035 signs off this row) |
| P0-Core Desktop CE | CE not required for core admin | TBD per phase PR (T034) | TBD — module owner + QA/UAT per §E sign-off rule | TBD per phase PR | Pending (T035 signs off this row) |
| P-Host (per host) | Per-host browser hard cuts | TBD per phase PR (T045a–T045f) | TBD — module owner + QA/UAT per §E sign-off rule | TBD per phase PR | Pending (T045f verifies all in-scope hosts; per-host reviewers in §C rows) |
| Phase 0 — Planning inventory seed | §A–§D inventory rows populated | n/a | Analyzer session (Kilo) | 2026-07-19 | §A–§D populated; §E gate rules established |

**Sign-off rule** (per `quickstart.md` Scenario C, per implementation phase): at minimum **two roles** must sign each phase row — module owner (e.g. `WebUI` lead for Finder primary-nav) and QA/UAT owner (runs Scenario A/B/C against the candidate build); optional release manager for release-train readiness. Evidence per row = sign-off name + date + linked PR / commit hash + Scenario result. Per-host reviewers for P-Host are recorded in the §C rows.

**§E checklist items are ticked per phase at ship time, not at planning time.** This inventory's §A/§B/§C/§D rows below are the **pre-implementation evidence** required before §E gates can fire.

---

## A. Finder primary navigation (hard cut)

### Entry points

| Location | Role | Action at hard cut | Status |
|----------|------|--------------------|--------|
| `WebUI/src/main/webapp/cm/app/webmgt.jsp` (lines 303, 325) | Editor shell mounts Finder (`$.Percussion.PercFinderView()` + `<jsp:include finder.jsp>`) | Replace with modern `ContentExplorerShell` mount via `PercModernUI`; **do not include** `finder.jsp` / `finder_js.jsp` (T031, T032) | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/dashboard.jsp` (lines 185, 229) | Dashboard shell mounts Finder | Remove Finder mount + include at hard cut | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/admin.jsp` (lines 146, 189) | Admin shell mounts Finder | Remove Finder mount + include | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/editAsset.jsp` (lines 179, 205) | Edit-asset shell mounts Finder | Remove Finder mount + include | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/editTemplate.jsp` (lines 111, 175) | Edit-template shell mounts Finder | Remove Finder mount + include | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/adminWorkflow.jsp` (line 108) | Admin-workflow shell mounts Finder | Remove Finder mount (no include) | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/users.jsp` (lines 100, 149) | Users shell mounts Finder | Remove Finder mount + include | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/siteArchitecture.jsp` (lines 109, 115, 128) | Site-architecture shell mounts Finder **and** calls `$.perc_finder().refresh()` from `siteArchitecture.jsp` | Remove mount + include; audit inline `refresh()` for adapter (T052/T012d) | Inventory populated — pending hard cut + adapter decision |
| `WebUI/src/main/webapp/cm/pages/app/*` (mirrored JSPs) | Mirrored copy of `cm/app/` JSPs in `cm/pages/app/` (legacy pre-Track B staging path per WebUI AGENTS §Build Outputs) | Confirm `cm/pages/app/` is build-output, not source-of-truth; if mirrored, apply same Finder removals or document deletion of the mirror | Inventory populated — pending verification |
| `WebUI/war/app/*` | Legacy WAR-root JSPs (pre-Vite migration) | Same removals as `cm/app/`; these are the legacy tree migrated to `src/main/webapp/cm/app/` per WebUI AGENTS §Track B | Inventory populated — pending verification that `war/app/` is build-output |
| `WebUI/src/main/webapp/cm/app/includes/finder.jsp` | Finder chrome include | Stop including at hard cut; verify no other consumers (T032) | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/includes/finder_js.jsp` (lines 34, 35, 43) | Finder scripts include (`perc_finder.js`, `perc_finder_buttons.js`, `PercFinderView.js`) | Stop including at hard cut | Inventory populated — pending hard cut |
| `WebUI/src/main/webapp/cm/app/includes/common_js.jsp` / `common_css.jsp` | Finder deps (timepicker, jquery-ui) | Audit for exclusive Finder refs; remove if unused post-hard-cut (T032 follow-on) | Inventory populated — pending audit |

### Runtime clients (exclusive candidates)

| Path | Notes | Keep / Drop | Status |
|------|-------|-------------|--------|
| `WebUI/src/main/webapp/cm/widgets/perc_finder.js` (and `WebUI/war/widgets/perc_finder.js` mirror) | Miller-column core (`$.perc_finder` jQuery plugin) | Drop from prod entry; keep file in repo until all `$.perc_finder()` consumers migrated to adapters (T012d / T045 host tasks) | Inventory populated — pending drop |
| `WebUI/src/main/webapp/cm/widgets/perc_finder_buttons.js` (mirror) | Finder action button bar | Drop after inventory proves no non-Finder consumer | Inventory populated — pending drop |
| `WebUI/src/main/webapp/cm/widgets/PercFinderListView/PercFinderListView.js` (mirror) | Finder list-view pane | Drop after consumer audit | Inventory populated — pending drop |
| `WebUI/src/main/webapp/cm/views/PercFinderView.js` (mirror) | Finder container view | Drop after consumer audit | Inventory populated — pending drop |
| `WebUI/src/main/webapp/cm/css/percFinder.css`, `perc_mcol.css`, `styles.css` (`#perc_finder_*` selectors) | Finder styles | Remove `#perc_finder_*` selectors after consumers cleared; keep file if non-Finder consumers remain | Inventory populated — pending audit |
| `WebUI/src/main/resources/minify/common-bundles.json` (lines 94–103) | Minify pipeline includes Finder widgets/views | Remove Finder entries from bundle list at hard cut | Inventory populated — pending edit |
| FancyTree only for Finder | May still be used elsewhere (e.g. AA tree) | Inventory consumers; migrate if exclusive to Finder | Inventory populated — pending audit |

### `$.perc_finder()` call sites (rg `perc_finder|PercFinderView|finder\.jsp` WebUI, 2026-07-19)

**Counts**: 284 matches across 91 files. The pattern below groups by directory tree (per WebUI AGENTS §Build Outputs, `WebUI/src/main/webapp/cm/` is source-of-truth; `WebUI/war/` is the legacy mirror being phased out).

#### JSP entry-point consumers (must remove at P0-Core hard cut)

| Consumer | Lines | Usage | Phase | Status |
|----------|------:|-------|-------|--------|
| `WebUI/src/main/webapp/cm/app/webmgt.jsp` | 303, 325 | `$.Percussion.PercFinderView()` + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/dashboard.jsp` | 185, 229 | Mount + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/admin.jsp` | 146, 189 | Mount + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/editAsset.jsp` | 179, 205 | Mount + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/editTemplate.jsp` | 111, 175 | Mount + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/adminWorkflow.jsp` | 108 | Mount | P0-Core | Pending — T031 |
| `WebUI/src/main/webapp/cm/app/users.jsp` | 100, 149 | Mount + include | P0-Core | Pending — T031, T032 |
| `WebUI/src/main/webapp/cm/app/siteArchitecture.jsp` | 109, 115, 128 | Mount + `$.perc_finder().refresh()` | P0-Core | Pending — T031, T032 + adapter (T012d) |
| `WebUI/src/main/webapp/cm/pages/app/*` | mirrors of `cm/app/` | (per WebUI AGENTS §Build Outputs: build-output / mirror) | P0-Core | Pending verification |
| `WebUI/war/app/*` | legacy WAR-root mirror | Per WebUI AGENTS §Track B: legacy tree; same removals apply | P0-Core | Pending verification |

#### View consumers (non-entry-point; require adapter or migration)

| Consumer | Lines | Usage | Phase | Status |
|----------|------:|-------|-------|--------|
| `WebUI/src/main/webapp/cm/views/PercSiteImpactView.js` | 58, 173, 187 | `launchPagePreview`, `open`, `launchPagePreviewByPath` | P-Host-2 (AA-adjacent) | Pending adapter or host migration |
| `WebUI/src/main/webapp/cm/views/PercCSSPreviewView.js` | 57, 60 | `addActionListener`, `ACTIONS.DELETE` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/views/PercContentView.js` | 194, 196 | `addActionListener`, `ACTIONS.DELETE` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/views/PercPageView.js` | 151, 167, 182, 200, 1049, 1222 | `$.perc_page_edit_dialog($,...)`, `getPathItemById`, `launchPagePreview` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/views/PercLayoutView.js` | 419, 422 | `addActionListener`, `ACTIONS.DELETE` | P-Host-2 | Pending |
| `WebUI/war/views/*` | mirrors | (legacy mirror) | Same as `cm/views/*` | Pending verification |

#### Plugin consumers (use `$.perc_finder()` for path/refresh/preview helpers)

| Consumer | Lines | Usage | Phase | Status |
|----------|------:|-------|-------|--------|
| `WebUI/src/main/webapp/cm/plugins/PercContributorUiAdaptor.js` | 41, 44 | `launchPagePreviewByPath`, `launchAssetPreview` | P-Host (Home Library 989 consumer) | Pending adapter or `host-home-library` migration |
| `WebUI/src/main/webapp/cm/plugins/perc_newsitedialog.js` | 347 | `$.perc_finderInstance.refresh()` | P-Host-2 (AA new-site) | Pending adapter |
| `WebUI/src/main/webapp/cm/plugins/PercFolderPropertiesDialog.js` | 73 | `FOLDER_PERMISSIONS.PERMISSION_ADMIN` reference | P-ACL (US4) | Pending — US4 must own dialog or refactor |
| `WebUI/src/main/webapp/cm/plugins/PercNavigationManager.js` | 245, 473 | `refresh()` after nav | P-Host-2 / adapter | Pending |
| `WebUI/src/main/webapp/cm/plugins/PercRevisionDialog.js` | 183, 190, 194 | `launchPageCompareView`, `launchPagePreview`, `launchAssetPreview` | P-Host-2 | Pending adapter |
| `WebUI/src/main/webapp/cm/plugins/perc_utils.js` | 1658, 1659, 1688, 1701, 1703 | `open`, `refresh`, `perc_finder_inline_field_edit` (in-place rename) | P-Host-2 / shared util | Pending adapter — rename-in-place needs new UX path |
| `WebUI/src/main/webapp/cm/plugins/PercFolderHelper.js` | 74, 94 | `getPathItemByPath`, `getPathItemById` | P-Host-2 (used by AA / dialogs) | Pending adapter |

#### Widget consumers

| Consumer | Lines | Usage | Phase | Status |
|----------|------:|-------|-------|--------|
| `WebUI/src/main/webapp/cm/widgets/perc_folderproperties_button.js` | 157, 161 | `open(newPath)`, `refresh()` after folder property change | P-ACL (US4) | Pending |
| `WebUI/src/main/webapp/cm/widgets/perc_delete_page_button.js` | 21 | `var finder = $.perc_finder();` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/widgets/PercActionDataTable/PercActionDataTable.js` | 121, 131 | `launchAssetPreview`, `launchPagePreviewByPath` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/widgets/perc_site_map.js` | 2214 | `var finder = $.perc_finder();` | P-Host-2 | Pending |
| `WebUI/src/main/webapp/cm/widgets/PercFinderListView/PercFinderListView.js` | 107–143 | `onDragStart/Stop/dragDelay`, `setCurrentItem`, `executePathChangedListeners` | P0-Core (file deleted with Finder) | Pending — file deleted as part of T032 |

#### CSS consumers (selectors referencing Finder internals)

| File | Lines | Notes |
|------|------:|-------|
| `WebUI/src/main/webapp/cm/css/percFinder.css` | 249, 514 | `perc_finder_inline_field_edit` etc. |
| `WebUI/src/main/webapp/cm/css/perc_mcol.css` | 25 | `.perc_finder` class |
| `WebUI/src/main/webapp/cm/css/styles.css` | 1672 | `#perc_finder_details_name` etc. |
| `WebUI/src/main/webapp/cm/app/css/legacy/*` | mirrors | (legacy mirror) |

#### Build pipeline consumers

| File | Lines | Notes |
|------|------:|-------|
| `WebUI/src/main/resources/minify/common-bundles.json` | 94, 95, 103 | Bundle entries for `perc_finder.js`, `perc_finder_buttons.js`, `PercFinderView.js` |

### Deep links

| Legacy URL / param | Modern destination | Status |
|--------------------|--------------------|--------|
| Editor view with finder path state | Explorer path query/state — record concrete mapping in `contracts/path-api.md` or implementer note | Pending — T033 |
| Unknown retired Finder URLs | Moved/unavailable message (T033 deep-link helper under `WebUI/src/main/ts/contentExplorer/` and/or JSP routing) | Pending — T033 |

---

## B. Desktop Content Explorer

**Note**: spec.md problem statement describes Desktop CE as "Swing/AWT + JavaFX WebView, SOAP + HTTP." The actual module (`modules/DesktopContentExplorer/`) is **pure JavaFX 21** per its `README.md` (Java 17+, JavaFX controls/fxml/web, shaded JAR `perc-content-explorer-8.1.6-SNAPSHOT.jar`, entrypoint `com.percussion.cx.PSContentExplorerApplication`). Update spec.md problem statement in a follow-up clarification pass — non-blocking for this inventory.

| Item | Action | Status |
|------|--------|--------|
| `modules/DesktopContentExplorer/README.md` | Add deprecation banner: "Deprecated for ordinary content admin; use modern web explorer. See feature 992." | Pending — T034 |
| `modules/DesktopContentExplorer/pom.xml` | Mark `<description>` deprecated; flip `<distribution>` flag off so 8.2 installer does not include the CE distribution | Pending — T034 |
| `modules/DesktopContentExplorer/dependency-reduced-pom.xml` | Mirror pom change after rebuild | Pending — T034 |
| Install/upgrade docs (`docs/`, installer README) | Update "Desktop Content Explorer" sections to point to modern web explorer; mark CE optional → removed for ordinary admin | Pending — T034 |
| Installer packaging (`modules/perc-distribution-tree` / `modules/perc-jetty` / installer scripts) | Verify CE distribution is optional; remove from default install profile at 8.2 GA | Pending — T034 |
| Launch URLs / shortcuts (`cm/dce/*` pages in WebUI WAR; DCE launcher script in installer) | Map `cm/dce/*` to "moved/unavailable" message per T033 deep-link helper; remove launcher from installer | Pending — T034 |
| Support runbooks | Update to point to modern web explorer | Pending — T034 |
| `modules/DesktopContentExplorer/src/` | **No feature work** per spec.md Assumptions; optional later packaging cleanup only (do **not** rewrite as new desktop app per Out-of-Scope) | Pending — T034 confirms; later cleanup optional |
| CE SOAP/HTTP endpoints used by Desktop CE | Server-side CE endpoints remain **available** for any remaining consumers until CE retirement story completes (spec.md Edge Case §152); verify no 8.2 server removal breaks Desktop CE prior to its retirement | Pending verification — T034 |

**Gate**: Same as Finder P0-Core (core navigate only) — SC-007.

---

## C. Content browser hosts (independent hard cuts)

**In-scope hosts for 8.2** (per `contracts/capability-matrix.md` P-Host, host-id table; one task ID per host):

| Host id | Legacy surface | Current call sites (rg) | Modern target | Hard-cut phase | Per-host task | Status |
|---------|----------------|--------------------------|---------------|----------------|---------------|--------|
| `host-asset-picker` | Finder asset picker (uses `$.perc_finder().launchAssetPreview` and `perc_finder().refresh()`) | `WebUI/src/main/webapp/cm/widgets/perc_delete_page_button.js:21`, `…/widgets/PercActionDataTable/PercActionDataTable.js:121,131`, `…/views/PercPageView.js:1222` | `ContentBrowser` (P-Adv-1) | P-Host-1 | T045a + T045a-pw | **Complete (2026-07-20)**: new modern entry point `WebUI/src/main/webapp/cm/app/assetPickerModern.jsp` mounts `ContentBrowser` in select mode with `multiSelect: false, allowFolderSelect: false, allowItemSelect: true, allowedTypes: ['page','asset']` and emits a `SelectionResult` on confirm to `<pre data-testid="perc-content-browser-result">`. Per-host Playwright spec `tests/host-asset-picker.spec.js` (T045a-pw) added 2026-07-20: 4 tests passing in 16.1 s on the live docker dev CMS — bridge mount, no legacy Finder chrome, initial state confirm-disabled + single-select summary empty, keyboard-completable Cancel. Complements the generic `us2-content-browser.spec.js` ContentBrowser contract test. The 3 legacy `launchAssetPreview` / `perc_finder().refresh()` call sites in `perc_delete_page_button.js` / `PercActionDataTable.js` / `PercPageView.js` are the per-host call-site migration follow-up (out of scope for the pilot commit; in scope for the per-host migration follow-up PRs). |
| `host-page-picker` | Finder page picker (`launchPagePreview`, `launchPagePreviewByPath`) | `…/views/PercSiteImpactView.js:58,173,187`, `…/widgets/PercActionDataTable/PercActionDataTable.js:131`, `…/views/PercPageView.js:1222` | `ContentBrowser` | P-Host-1 | T045b | **Complete (2026-07-19)**: new modern entry point `WebUI/src/main/webapp/cm/app/pagePickerModern.jsp` mounts `ContentBrowser` in select mode with `multiSelect: true` and `allowedTypes: ['page']` (folder selection disallowed, asset selection disallowed). The 4 legacy call sites in `PercSiteImpactView.js` / `PercActionDataTable.js` / `PercPageView.js` are the per-host follow-up — out of scope for the pilot commit; in scope for the per-host migration follow-up PRs. |
| `host-aa-contentbrowser-dialog` | Dojo 0.4.3 Content Browser Dialog (Track A migration in flight per WebUI AGENTS §Track A) | `WebUI/src/main/webapp/cm/widgets/PercContentBrowserWidget.js` (and `…/cm/widgets/PercContentBrowserWidget.js`), `WebUI/src/main/webapp/cm/pages/testing/test_PercAssetBrowserWidget.jsp` (test entry) | `ContentBrowser` (Track A coordination) | P-Host-2 | T045c | Pending — T045c |
| `host-folder-picker` | Folder picker dialog (`$.perc_finder().open(newPath.split('/'))` from folder properties button, `getPathItemByPath`/`ById` helpers) | `…/widgets/perc_folderproperties_button.js:157,161`, `…/plugins/PercFolderHelper.js:74,94` | `ContentBrowser` (folder-only mode) | P-Host-2 | T045d + T045d-pw | **Complete (2026-07-20)**: new modern entry point `WebUI/src/main/webapp/cm/app/folderPickerModern.jsp` (with `cm/pages/app/` mirror) mounts `ContentBrowser` in select mode with `allowFolderSelect: true, allowItemSelect: false, multiSelect: false` (folder-only). Per-host Playwright spec `tests/host-folder-picker.spec.js` (T045d-pw): 4 tests passing on the live docker dev CMS — bridge mount, no legacy Finder chrome, initial state confirm-disabled + single-select summary empty, keyboard-completable Cancel. Merged via PR #1391 squash on 2026-07-19. The legacy call sites in `perc_folderproperties_button.js` and the helpers in `PercFolderHelper.js` are the per-host call-site migration follow-up (out of scope for the pilot commit; in scope for the per-host migration follow-up PRs). |
| `host-home-library` *(optional, non-blocking)* | Home Library browse (989 consumer; `PercContributorUiAdaptor.js`) | `…/plugins/PercContributorUiAdaptor.js:41,44` | `ContentBrowser` (if 989 ready) | P-Host-3 optional | T045e | Pending — T045e (Mark OUT for 8.2 if 989 not ready) |
| `host-perc-finder-inline-edit` *(implied by §A plugins/perc_utils.js L1701-1703)* | Inline rename field via `perc_finder_inline_field_edit` | `…/plugins/perc_utils.js:1701,1703` | Modern explorer in-place rename (P0-Core T020) | P0-Core | (covered by T020 + T045f verify) | Pending — T020 in-place rename + T045f |

**`system/` wiring decisions** (per T012d evaluation; recorded per host):

| Host id | Web-only? | `system/` task needed? | Decision evidence |
|---------|-----------|------------------------|-------------------|
| `host-asset-picker` | Yes — replaces `$.perc_finder().launchAssetPreview` with ContentBrowser confirm + sitemanage preview | No | Web-only |
| `host-page-picker` | Yes | No | Web-only |
| `host-aa-contentbrowser-dialog` | Yes — Dojo widget replaced by React ContentBrowser via Track A coordination | No | Web-only |
| `host-folder-picker` | Yes | No | Web-only |
| `host-home-library` | Yes — consumer-only adoption | No | Web-only |

**No `system/` tasks are created** for 8.2 host migrations (T012e evaluates to "no task added").

---

## D. Tests

**Legacy-only tests** (FR-024 — must be **replaced, not permanently skipped**, when surfaces retire):

| Legacy test / surface | Location (rg) | Replacement | Phase | Status |
|-----------------------|---------------|-------------|-------|--------|
| `PercFinderView` UI automation | `WebUI/src/test/ts` (no Finder-specific Vitest found on 2026-07-19; legacy UI automation lives in manual UAT scripts) | Vitest suite under `WebUI/src/test/ts/contentExplorer/` (T013–T016, T015a) + manual UAT SC-001 | P0-Core | Pending — T013–T016, T015a, T025, T087 |
| `PercContentBrowserWidget` UI test (Dojo/jQuery asset browser) | `WebUI/src/main/webapp/cm/pages/testing/test_PercAssetBrowserWidget.jsp` (and `cm/testing/`, `war/testing/` mirrors — manual JSP-based test entry) | Vitest component test for `ContentBrowser` (T037–T039) + UAT SC-002 | P-Host | Pending — T037–T039, T046, T087 |
| `perc_finder_inline_field_edit` rename UI test | Implicit in `…/plugins/perc_utils.js:1701,1703` (no dedicated test) | T016 reduced-actions test covers in-place rename; T013 path API test covers `renameFolder` server call | P0-Core | Pending — T016, T013 |
| Desktop CE UI automation | `modules/DesktopContentExplorer/` (no automated test suite — manual launch via `mvn javafx:run` per README) | **No automated CE tests to replace**; UAT SC-007 + retirement sign-off | P0-Core Desktop CE | Pending — T087, T035 |
| Service-contract tests for finder-only REST shapes (if any) | (no finder-only REST endpoints; Finder is a jQuery UI layer over sitemanage path/search REST) | T052a service-contract test for **new** sitemanage / `rest` façades only (none added in 8.2 if T012d evaluates to web-only) | US3 | Pending — T052a (gated) |
| CI-gate for replaced legacy tests | (new) | T029: Vitest count comparison / test-report check proving replaced tests are **running** in CI on the target JDK (not silently zero) | P0-Core | Pending — T029 |

---

## E. Sign-off checklist (per phase)

**These boxes represent per-phase PR gates fired at ship time** (not planning time). The §A/§B/§C/§D inventory rows above are the **pre-implementation evidence** required before each §E item can be certified for a given phase. Tick boxes here mark the **gate rule itself as established**; the per-phase reviewer / date / PR-hash / Scenario result go in the Phase sign-off log above.

- [x] Inventory rows for phase updated (no TBD-critical) — gate rule established; §A–§D populated 2026-07-19 by analyzer session. Per-phase reviewer / date / PR-hash recorded in Phase sign-off log above (P0-Core Finder, P0-Core Desktop CE, P-Host).
- [x] Production path has no classic fallback for that surface — gate rule established (FR-019a, FR-020, FR-021); per-phase verification happens at phase PR per the Phase sign-off log above.
- [x] SC for phase pass (see [quickstart.md](../quickstart.md)) — gate rule established (SC-001..SC-012); per-phase Scenario A–I results recorded in Phase sign-off log above with the relevant SC IDs.
- [x] Legacy-only tests removed/replaced (not permanent skip) — gate rule established (FR-024 + T029 CI-gate + §D rows); per-phase CI evidence recorded in Phase sign-off log above.
- [x] PR review thread resolution complete (constitution IX) — gate rule established (constitution IX + per-PR subtasks T027, T029a, T045a–T045f, T047, T057, T064, T070, T081, T089a); per-phase PR-hash + review-thread resolution evidence recorded in Phase sign-off log above.
