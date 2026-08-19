# Gap matrix: Desktop Content Explorer → SPA Explorer

**Parent:** #2400  
**Evidence date:** 2026-08-19 (Partial **citation residual** [#3619](https://github.com/intersoftdatalabs-in/percussioncms/issues/3619) after #3577 — [partial-citation-residual-after-3577.md](../research/partial-citation-residual-after-3577.md); never **Present**. Prior: Partial citation refresh [#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577) — [closed-qa-partial-citation-refresh.md](../research/closed-qa-partial-citation-refresh.md). Inbox + Views **Partial** — product-route proof #3561. Prior: Views + Inbox **Missing** #3108; false-Present→Partial reconcile #3109 / parent [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102); closeout #2794 / #2827 / #2828 / #2829 on 2026-08-10)  
**Rule:** Status reflects **product route** (`ExplorerRoute` → `ContentExplorerShell`), not isolated components in the registry. Silent omission is not allowed — unplanned capabilities must be **Missing**, **Partial**, **Present**, or signed **OUT**.  
**QA gate:** **Present** requires closed human QA (or an explicit defend note). Open **Failed** / open QA on a row → keep **Partial** until pass. Agent merge alone is insufficient (#3102 / #3109). Closed **QA: Failed** (reconcile) is **not** a Pass.

Legend: **Present** | **Partial** | **Missing** | **OUT**

## Navigation & chrome

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Sites/folders tree | `PSNavigationTree` | `ExplorerTree` + path APIs | **Partial** (code on route; H2 operator proof closed — never Present without Passed human QA) | Code present · original Sites-load [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) **closed** · dual-run QA [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) **Passed** · operator proof [#3575](https://github.com/intersoftdatalabs-in/percussioncms/issues/3575) **closed** ([PR #3591](https://github.com/intersoftdatalabs-in/percussioncms/pull/3591) merged) · H2 demo-sites qa-health [#3592](https://github.com/intersoftdatalabs-in/percussioncms/issues/3592) **closed** ([PR #3602](https://github.com/intersoftdatalabs-in/percussioncms/pull/3602) merged) |
| Detail list of children | Main list | `DetailList` + `paginatedFolder` | Present | — (list chrome present; useful content depends on tree/Sites #2989) |
| Full product shell composition | Frame + panels | Shell composes search, menus, DF, security on product route | **Partial** (merged; original Failed QA closed after residuals — never Present) | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) (merged) · original QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed 2026-08-18** · residual [#3208](https://github.com/intersoftdatalabs-in/percussioncms/issues/3208) / cluster [PR #3278](https://github.com/intersoftdatalabs-in/percussioncms/pull/3278) · retest [#3264](https://github.com/intersoftdatalabs-in/percussioncms/issues/3264) **Passed** |
| Menu bar (Content / View / Help) | `ContentExplorerMenu.xml` | `ExplorerMenuBar` on product shell (nested Content/View/Help; view tools under View) | **Partial** (merged; original Failed QA closed after clipboard residual — never Present) | #2731 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · original QA [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) **Failed, closed 2026-08-18** · residual [#3544](https://github.com/intersoftdatalabs-in/percussioncms/issues/3544)/[#3551](https://github.com/intersoftdatalabs-in/percussioncms/issues/3551) via cluster [PR #3557](https://github.com/intersoftdatalabs-in/percussioncms/pull/3557) |
| Server-driven toolbar/context menus | Action manager + server menus | `ActionToolbar` (nested MENU dropdowns) + `ContextMenu` (row right-click nested MENU, same catalog) + `actionMenuApi` wired in shell | **Partial** (merged; nested MENU residual landed — never Present) | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) · nested #2730 / [PR #2782](https://github.com/intersoftdatalabs-in/percussioncms/pull/2782) · original QA [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) **Failed, closed 2026-08-18** · residuals [#3379](https://github.com/intersoftdatalabs-in/percussioncms/issues/3379)/[#3500](https://github.com/intersoftdatalabs-in/percussioncms/issues/3500)/[#3560](https://github.com/intersoftdatalabs-in/percussioncms/issues/3560) / [PR #3565](https://github.com/intersoftdatalabs-in/percussioncms/pull/3565) · row context MENU [#3629](https://github.com/intersoftdatalabs-in/percussioncms/issues/3629) · prior QA [#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856) **Passed** · [#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988) **Passed** |
| Reduced create/rename/move/copy/delete | Folder actions | `ReducedActions` in shell | Present | — |
| Open / preview item | Open handlers | `openInEditor` + product `openPreviewItem` (page render / asset view URL) from selection | **Partial** (merged; original Failed QA closed after residuals — never Present) | #2733 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · original QA [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) **Failed, closed 2026-08-18** · residuals [#3457](https://github.com/intersoftdatalabs-in/percussioncms/issues/3457)/[#3456](https://github.com/intersoftdatalabs-in/percussioncms/issues/3456)/[#3458](https://github.com/intersoftdatalabs-in/percussioncms/issues/3458) |
| Multi-select list | Selection model | `multiSelectedIds` / `DetailList` multi-select in shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) · **Defend:** no Failed QA on multi-select itself; clipboard row separate |
| Display formats for columns | Display format catalog | `GET /rest/displayformats?validForFolder=true` + shell selector + `displayProperties` | **Partial** (merged; original Failed QA closed after residuals — never Present) | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) · original QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed 2026-08-18** · residual [#3208](https://github.com/intersoftdatalabs-in/percussioncms/issues/3208) / retest [#3264](https://github.com/intersoftdatalabs-in/percussioncms/issues/3264) **Passed** · remaining operator proof [#3618](https://github.com/intersoftdatalabs-in/percussioncms/issues/3618) open ([PR #3621](https://github.com/intersoftdatalabs-in/percussioncms/pull/3621)) |
| View options / refresh | View menu | `ExplorerMenuBar` View → Refresh + panel toggles + display format; always-visible refresh residual | **Partial** (merged; original Failed QA closed after residuals — never Present) | #2731 · #2733 · [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · original QA [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741)/[#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) **Failed, closed 2026-08-18** · residuals [#3544](https://github.com/intersoftdatalabs-in/percussioncms/issues/3544)/[#3457](https://github.com/intersoftdatalabs-in/percussioncms/issues/3457)/[#3456](https://github.com/intersoftdatalabs-in/percussioncms/issues/3456)/[#3458](https://github.com/intersoftdatalabs-in/percussioncms/issues/3458) |
| **Views (DCE navigation category)** | DCE system category **Views** (`ContentExplorer.xml` → `sys_cxSupport/Views.html`; My / Community / … view categories 1–4) | Product **IN**. `ViewsCatalogTree` is on `ContentExplorerShell` (`spa.jsp?entry=explorer`): My / Community / All / Other groups + runnable leaves. Not the same as **View menu chrome**, **display formats**, or **saved searches**. Developer Views (UI-07 / `#1690`) is still not CE navigation parity. Human QA still required — **never Present** from agent merge. | **Partial** | Operator symptoms [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) · matrix docs [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) · V2 tree [#3116](https://github.com/intersoftdatalabs-in/percussioncms/issues/3116) / cluster [PR #3252](https://github.com/intersoftdatalabs-in/percussioncms/pull/3252) · product-route proof [#3561](https://github.com/intersoftdatalabs-in/percussioncms/issues/3561) · research [views-inbox-missing-disposition.md](../research/views-inbox-missing-disposition.md) |
| **Inbox** | DCE Inbox (operator inbox / assignment surface in CE navigation). **IA:** system custom view at `//Views//MyContent/Inbox` (`sys_cxViews/inbox`) — **not** a CE root. | Product **IN** (#3118). Operator path **Views → My Content → Inbox** on `ContentExplorerShell`. C1 `POST /services/views/{idOrName}/execute` ([#3239](https://github.com/intersoftdatalabs-in/percussioncms/issues/3239) / [PR #3245](https://github.com/intersoftdatalabs-in/percussioncms/pull/3245)) and Explorer leaf ([#3240](https://github.com/intersoftdatalabs-in/percussioncms/issues/3240) / cluster [PR #3252](https://github.com/intersoftdatalabs-in/percussioncms/pull/3252)) are **both** on the product route. Empty assignment list is HTTP 200 success. Playwright `explorer-inbox.spec.js` / `explorer-views-catalog.spec.js` must not soft-skip when the leaf exists (#3561). Never **Present** without closed human QA. | **Partial** | Operator symptoms [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) · matrix docs [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) · implement [#3118](https://github.com/intersoftdatalabs-in/percussioncms/issues/3118) / C1 [#3239](https://github.com/intersoftdatalabs-in/percussioncms/issues/3239) / leaf [#3240](https://github.com/intersoftdatalabs-in/percussioncms/issues/3240) / docs+PW [#3241](https://github.com/intersoftdatalabs-in/percussioncms/issues/3241) / product-route proof [#3561](https://github.com/intersoftdatalabs-in/percussioncms/issues/3561) · research [views-inbox-ia-api-map.md](../research/views-inbox-ia-api-map.md) |

## Search

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Simple / extended search | `PSSearchDialog` | `SearchPanel` toggle in product shell | **Partial** (merged; original Failed QA closed after residuals — never Present) | #2407 · [PR #2412](https://github.com/intersoftdatalabs-in/percussioncms/pull/2412) · original QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed 2026-08-18** · residual [#3208](https://github.com/intersoftdatalabs-in/percussioncms/issues/3208) · search chrome [#2858](https://github.com/intersoftdatalabs-in/percussioncms/issues/2858) **Passed** · free-text [#2966](https://github.com/intersoftdatalabs-in/percussioncms/issues/2966) **Passed** · remaining operator proof [#3617](https://github.com/intersoftdatalabs-in/percussioncms/issues/3617) open ([PR #3620](https://github.com/intersoftdatalabs-in/percussioncms/pull/3620)) |
| Open / reveal from results | Search results | Callbacks on panel | Present | #2407 · **Defend:** panel callbacks remain code-present; operator value gated by search/saved-search QA |
| Saved searches catalog + run | CE saved search | Catalog `GET /rest/searches` + execute façade `POST /services/searches/{idOrName}/execute` + `SearchPanel` picker/run on product shell | **Partial** (merged; original Failed QA closed; H2 proof closed; human QA #2645 still open — never Present) | #2409 · A–D #2504–#2507 · [PR #2579](https://github.com/intersoftdatalabs-in/percussioncms/pull/2579) / [#2592](https://github.com/intersoftdatalabs-in/percussioncms/pull/2592) / [#2606](https://github.com/intersoftdatalabs-in/percussioncms/pull/2606) / [#2644](https://github.com/intersoftdatalabs-in/percussioncms/pull/2644) · original QA [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) **Failed, closed 2026-08-18** · residuals [#3205](https://github.com/intersoftdatalabs-in/percussioncms/issues/3205)/[#3199](https://github.com/intersoftdatalabs-in/percussioncms/issues/3199)/[#3517](https://github.com/intersoftdatalabs-in/percussioncms/issues/3517) · retest [#3234](https://github.com/intersoftdatalabs-in/percussioncms/issues/3234) **Passed** · [#3237](https://github.com/intersoftdatalabs-in/percussioncms/issues/3237) **Passed** · [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Passed** · operator proof [#3576](https://github.com/intersoftdatalabs-in/percussioncms/issues/3576) **closed** ([PR #3593](https://github.com/intersoftdatalabs-in/percussioncms/pull/3593) merged) · remaining human QA [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) open (**To Be Tested**) |
| Search in ContentBrowser hosts | Dialogs | `ContentBrowser` mounts shared `SearchPanel` when `enableSearch` (asset picker host on); catalog + free-text + saved execute reuse Explorer APIs | **Present** | #2793 · [PR #2798](https://github.com/intersoftdatalabs-in/percussioncms/pull/2798) (merged) · QA [#2799](https://github.com/intersoftdatalabs-in/percussioncms/issues/2799) To Be Tested · **Defend Present (host mount only):** #3102/#3109 did not cite host-picker failure; product-shell search/saved-search remain Partial above |

## Security & properties

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Folder ACL view/edit | `PSFolderSecurityPanel` | `FolderSecurityPanel` toggle on product shell with identities + saveFolderProperties | **Present** | #2410 · [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) (merged) · QA [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) |
| Object ACL editor (full) | CE ACL dialogs | **Not an Explorer shell residual.** CD-19 product work lives on Developer `ObjectAclSection` mounts (design-object peers). Code slices B1–B5 merged on main; do **not** re-implement under #2400. Status stays **Partial** only while human QA remains open (not Missing / not false Explorer backlog). | **Partial** (cross-epic pointer; code merged; human QA open) | **Cross-epic:** [#2274](https://github.com/intersoftdatalabs-in/percussioncms/issues/2274) (slice B of [#2262](https://github.com/intersoftdatalabs-in/percussioncms/issues/2262)) · grandparent [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) · B1–B5 #2281/#2282/#2283/#2604/#2605 merged · residual peer PW #2642 merged · human QA [#2640](https://github.com/intersoftdatalabs-in/percussioncms/issues/2640)/[#2643](https://github.com/intersoftdatalabs-in/percussioncms/issues/2643)/[#2672](https://github.com/intersoftdatalabs-in/percussioncms/issues/2672) open · docs disposition #2828 |
| Folder properties (community, locale, DF, workflow) | `PSFolder*Panel` | `FolderPropertiesEditor` inside `FolderSecurityPanel` on product shell | **Present** | #2410 · [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) (merged) · QA [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) |

## Advanced / power user

| Capability | DCE source | Explorer / REST today | Status | Slice |
|------------|------------|------------------------|--------|-------|
| Clipboard cut/copy/paste multi | `PSClipBoard` | `ClipboardPanel` + multi-select wired in product shell | **Present** | #2408 · [PR #2522](https://github.com/intersoftdatalabs-in/percussioncms/pull/2522) (merged) |
| Site copy wizard | CE wizards | `SiteCopyWizard` on product shell via Content → Site Copy (`content-site-copy`); source prefilled from `/Sites/<name>` | **Present** | #2767 · [PR #2773](https://github.com/intersoftdatalabs-in/percussioncms/pull/2773) (merged) |
| Subfolder copy wizard | CE wizards | `SubfolderCopyWizard` on product shell via Content → Subfolder Copy (`content-subfolder-copy`); source prefilled from active/selected folder path | **Present** | #2792 · [PR #2796](https://github.com/intersoftdatalabs-in/percussioncms/pull/2796) (merged) |
| Dependency viewer | `PSDependencyViewer` | `DependencyViewer` + relationship summary REST mounted in product Explorer shell (View → Dependencies; #2768) | **Present** | #2768 · [PR #2775](https://github.com/intersoftdatalabs-in/percussioncms/pull/2775) (merged) · advanced chrome (phase 4) |
| IA / relationships view | Managers | `RelationshipsView` on product shell (View → IA Relationships; selected item + REST summary) | **Partial** (merged; original QA Failed closed after id-bind residual — never Present) | #2769 · [PR #2777](https://github.com/intersoftdatalabs-in/percussioncms/pull/2777) (merged) · original QA [#2778](https://github.com/intersoftdatalabs-in/percussioncms/issues/2778) **Failed, closed 2026-08-18** · residual [#3546](https://github.com/intersoftdatalabs-in/percussioncms/issues/3546) via cluster [PR #3557](https://github.com/intersoftdatalabs-in/percussioncms/pull/3557) |
| Translation workflow | CE translation | **REST Present:** `GET|POST /rest/content-explorer/translations` (#2429 / [PR #2601](https://github.com/intersoftdatalabs-in/percussioncms/pull/2601)). **Explorer UI Present (slice C #2430):** `TranslationsPanel` in `ContentExplorerShell` (locale list + create-variant; Vitest + Playwright `explorer-translations.spec.js`). In-flight queue + content-locale session are intentional **OUT** (signed #2829 — [p-trans-out-disposition.md](../research/p-trans-out-disposition.md)). Inventory: [p-trans-api-inventory.md](../research/p-trans-api-inventory.md). Human QA [#2649](https://github.com/intersoftdatalabs-in/percussioncms/issues/2649). | **Present** (locales + create; OUT rows under Explicit OUT) | #2411 → #2428 / #2429 / #2430 · OUT residual #2829 |
| Workflow transitions in menus | CE workflow | `itemWorkflowApi` + `workflowMenuActions` merge Workflow group into toolbar/context menus; invoke `transitionWithComments` for `workflow-transition:*` triggers | **Present** | #2732 · absorbed [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) · QA [#2743](https://github.com/intersoftdatalabs-in/percussioncms/issues/2743) |

## Explicit OUT (for now)

| Capability | Reason |
|------------|--------|
| JavaFX desktop window / native file chooser parity | Platform desktop; web redesign for file ops |
| DCE help JavaHelp topics 1:1 | Product help site / in-app help later |
| DCE-only packaging residual | Decommission program after parity |
| In-flight translation queue (`translationState=inFlight`) | **OUT** signed #2829 — not 8.2 SPA parity; reopen only with product sign-off + typed REST/search contract. See [p-trans-out-disposition.md](../research/p-trans-out-disposition.md). |
| Content-locale session context (path APIs re-issued under content locale) | **OUT** signed #2829 — per-item locale + create-variant sufficient; session model is redesign. Same OUT note. |

## Implementation notes

### 2026-08-19 Partial citation residual after #3577 (#3619 / #3102)

**This slice:** [#3619](https://github.com/intersoftdatalabs-in/percussioncms/issues/3619) · **Research:** [partial-citation-residual-after-3577.md](../research/partial-citation-residual-after-3577.md)

[#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577) / [PR #3594](https://github.com/intersoftdatalabs-in/percussioncms/pull/3594) refreshed Slice cells for reconcile-closed Failed QA. Residual after that merge: (1) the **#3109 snapshot table** still read as live (**Failed** / **open**), and (2) proof tickets cited as **open** had since closed. Live GitHub 2026-08-19. **No row flipped to Present.** Views / Inbox / saved-search stay **Partial**.

| Capability | Status | Live citation change (2026-08-19) |
|------------|--------|-----------------------------------|
| Sites/folders tree | stays **Partial** | [#3575](https://github.com/intersoftdatalabs-in/percussioncms/issues/3575) **closed** ([PR #3591](https://github.com/intersoftdatalabs-in/percussioncms/pull/3591) merged); [#3592](https://github.com/intersoftdatalabs-in/percussioncms/issues/3592) **closed** ([PR #3602](https://github.com/intersoftdatalabs-in/percussioncms/pull/3602) merged). Original [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) **closed**; [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) **Passed**. No Passed human QA for Sites/Assets tree. |
| Full product shell / menu / preview / toolbar | stays **Partial** | Original Failed QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588)/[#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741)/[#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745)/[#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) remain **CLOSED Failed** (not open). Do not cite as live Failed. |
| Display formats | stays **Partial** | Remaining operator proof [#3618](https://github.com/intersoftdatalabs-in/percussioncms/issues/3618) **open** ([PR #3621](https://github.com/intersoftdatalabs-in/percussioncms/pull/3621)). Original [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed**. |
| Simple / extended search | stays **Partial** | Remaining operator proof [#3617](https://github.com/intersoftdatalabs-in/percussioncms/issues/3617) **open** ([PR #3620](https://github.com/intersoftdatalabs-in/percussioncms/pull/3620)). Original [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed**. |
| Saved searches catalog + run | stays **Partial** | [#3576](https://github.com/intersoftdatalabs-in/percussioncms/issues/3576) **closed** ([PR #3593](https://github.com/intersoftdatalabs-in/percussioncms/pull/3593) merged); [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Passed**; [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) **Failed, closed**. Remaining human QA [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) **open** (To Be Tested). |
| Views / Inbox | stays **Partial** | Unchanged — never Present from this docs slice. |

**Do not:** treat reconcile-closed Failed qa-tasks as Passed; flip Partial → Present from this docs PR; close remaining human QA (#2645); rewrite the 2026-08-11 #3109 snapshot as if it were live.

### 2026-08-18 Partial citation refresh after closed Failed-QA residuals (#3577 / #3102)

**This slice:** [#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577) · **Research:** [closed-qa-partial-citation-refresh.md](../research/closed-qa-partial-citation-refresh.md)

Reconcile close on 2026-08-18 shut unassigned **QA: Failed** tickets after implement residuals landed. Matrix Slice cells still said **Failed** / **open** as if those tickets were live. Citations now match GitHub. **No row flipped to Present.** Views / Inbox stay **Partial**.

| Capability | Status | Citation change (2026-08-18) |
|------------|--------|------------------------------|
| Sites/folders tree | stays **Partial** | [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) closed; [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) **Passed**; then-remaining [#3575](https://github.com/intersoftdatalabs-in/percussioncms/issues/3575) / [#3592](https://github.com/intersoftdatalabs-in/percussioncms/issues/3592) (both **closed** 2026-08-19 — see #3619 note above) |
| Full product shell composition / display formats / simple search | stays **Partial** | [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed, closed**; residual [#3208](https://github.com/intersoftdatalabs-in/percussioncms/issues/3208); retest [#3264](https://github.com/intersoftdatalabs-in/percussioncms/issues/3264) **Passed** (not a Present license) |
| Menu bar / View options | stays **Partial** | [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741)/[#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) **Failed, closed**; residuals [#3544](https://github.com/intersoftdatalabs-in/percussioncms/issues/3544)/[#3456](https://github.com/intersoftdatalabs-in/percussioncms/issues/3456)–[#3458](https://github.com/intersoftdatalabs-in/percussioncms/issues/3458) |
| Open / preview item | stays **Partial** | [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) **Failed, closed**; listing/preview/console residuals landed |
| Server-driven toolbar | stays **Partial** | [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) **Failed, closed**; nested MENU [#3560](https://github.com/intersoftdatalabs-in/percussioncms/issues/3560) / [PR #3565](https://github.com/intersoftdatalabs-in/percussioncms/pull/3565); row context MENU [#3629](https://github.com/intersoftdatalabs-in/percussioncms/issues/3629) |
| Saved searches catalog + run | stays **Partial** | [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) **Failed, closed**; [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Passed**; then-remaining [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) open + [#3576](https://github.com/intersoftdatalabs-in/percussioncms/issues/3576) (**closed** 2026-08-19 — see #3619; #2645 still open) |
| IA / relationships view | **Present → Partial** | [#2778](https://github.com/intersoftdatalabs-in/percussioncms/issues/2778) **Failed, closed** (no Pass); residual [#3546](https://github.com/intersoftdatalabs-in/percussioncms/issues/3546). #3109 said re-audit if Failed. |

**Do not:** treat reconcile-closed Failed qa-tasks as Passed; flip Partial → Present from this docs PR; close remaining human QA (#2645).

### 2026-08-18 Inbox + Views product-route proof (#3561 / #3118) — Missing → Partial

**Rule:** Inbox may move **Missing → Partial** only when **both** custom-URL execute (C1) **and** the Explorer **Views → My Content → Inbox** leaf are on the product route. Views may move **Missing → Partial** when the catalog tree is on `ContentExplorerShell`. **Never Present** from agent merge or docs/Playwright alone (#3102 / #3109 QA gate).

| Evidence (2026-08-18) | On `main` product route? |
|-----------------------|--------------------------|
| Views catalog tree (`ViewsCatalogTree`) | Yes — `spa.jsp?entry=explorer` / `ContentExplorerShell` (#3116 / cluster [PR #3252](https://github.com/intersoftdatalabs-in/percussioncms/pull/3252)) |
| C1 `POST /services/views/{idOrName}/execute` Inbox family | Yes — [#3239](https://github.com/intersoftdatalabs-in/percussioncms/issues/3239) / [PR #3245](https://github.com/intersoftdatalabs-in/percussioncms/pull/3245) |
| Explorer Inbox leaf | Yes — Views → My Content → Inbox (`ensureInboxInMyContent` stub when catalog omits the row) |
| Surface Playwright | `explorer-inbox.spec.js` / `explorer-views-catalog.spec.js` — **do not** soft-skip when the leaf/tree exists; empty list HTTP 200 is success |

**Status:** Views + Inbox **Partial** (this slice). Human QA remains open — do **not** flip to Present.

### 2026-08-12 Inbox docs + Playwright (#3241 / #3118) — still Missing

**Rule:** Inbox may move **Missing → Partial** only when **both** custom-URL execute (C1) **and** the Explorer **Views → My Content → Inbox** leaf are on the product route. **Never Present** from agent merge or docs/Playwright alone (#3102 / #3109 QA gate).

| Evidence (2026-08-12) | On `main` product route? |
|-----------------------|--------------------------|
| Operator docs (this slice) | Yes — `product-docs/8.2/admin/content-explorer.md` + REST Inbox-family notes |
| C1 `POST /services/views/{idOrName}/execute` Inbox family | In-flight [#3239](https://github.com/intersoftdatalabs-in/percussioncms/issues/3239) / [PR #3245](https://github.com/intersoftdatalabs-in/percussioncms/pull/3245) — **not** both merged with leaf |
| Explorer Inbox leaf | Open [#3240](https://github.com/intersoftdatalabs-in/percussioncms/issues/3240) |
| Surface Playwright | `modules/perc-qa-automation/frontend/tests/explorer-inbox.spec.js` — soft-skip when leaf or assignments missing |

**Status stays Missing** until execute + leaf exist together. After that merge, flip this row to **Partial** (not Present) and keep human QA open.

### 2026-08-11 Views + Inbox Missing rows (#3108 / #3102)

**Problem:** Operator reality-check [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) found that **Views (DCE tree category)** and **Inbox** were **silent omissions** on this matrix. Chrome rows for View **menu** / display formats / saved searches do **not** cover the design Views catalog navigation, and Inbox had no row at all.

**Disposition (docs only — no product sign-off invent):**

| Capability | Status | Why not OUT |
|------------|--------|-------------|
| Views (DCE navigation category) | **Missing** | Product has not signed IN / OUT / REDESIGN. Overnight agents must **not** invent OUT. Until product decides, track as Missing and plan via #3110. |
| Inbox | **Missing** | Same rule — silent omission banned; no implement without product IN or signed OUT. |

**Do not confuse with Partial/Present chrome:**

| Matrix row | What it is | What it is not |
|-------------|------------|----------------|
| View options / refresh | Menu chrome (Refresh, panel toggles) | Not design Views catalog tree |
| Display formats for columns | Column DF selector | Not Views navigation category |
| Saved searches catalog + run | CE saved searches | Views are a **separate** catalog (see [saved-search-execute-disposition.md](../research/saved-search-execute-disposition.md): *Views are a separate catalog (Developer Views / UI-07)*) |

**Research note:** [views-inbox-missing-disposition.md](../research/views-inbox-missing-disposition.md) — maps operator symptoms → matrix rows → related QA/bugs → product decision options.

**Next (not this PR):** Product IN/OUT on Views + Inbox (#3102 acceptance); if IN, PR-sized implement children via #3110. Sibling #3109 reconciles false Present vs Failed human QA on other rows.

### 2026-08-11 false Present vs open Failed QA reconcile (#3109 / #3102)

**Parent reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) · **This slice:** [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109)  
**Research note:** [false-present-qa-reconcile.md](../research/false-present-qa-reconcile.md)

Operator/QA evidence showed several gap-matrix rows still **Present** while human QA was **Failed** or still open. Per #3102 rule — *Present without closed human QA is not release-ready* — those rows are flipped to **Partial** with QA links. No Explorer chrome re-implement in this docs slice.

**Snapshot (2026-08-11).** The **Failed** / **open** words below are **as-of that date**. Live GitHub state is in the capability tables above and [#3619](https://github.com/intersoftdatalabs-in/percussioncms/issues/3619) / [partial-citation-residual-after-3577.md](../research/partial-citation-residual-after-3577.md). Do **not** cite this snapshot as current: [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588)/[#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741)/[#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745)/[#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783)/[#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) are **CLOSED Failed** (reconcile 2026-08-18); [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729)/[#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856)/[#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988)/[#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) **Passed**; [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) **closed**. [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) remains **open** (To Be Tested).

| Capability | Was | Now | QA / operator evidence (2026-08-11 snapshot) |
|------------|-----|-----|-------------------------------------|
| Sites/folders tree | Present | **Partial** | [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) p1 Sites not loaded / no create; related folder QA [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) |
| Full product shell composition | Present | **Partial** | [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** |
| Display formats for columns | Present | **Partial** | Same shell QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** |
| Simple / extended search | Present | **Partial** | Same shell QA [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** |
| Menu bar (Content / View / Help) | Present | **Partial** | [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) open |
| View options / refresh | Present | **Partial** | [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) · [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) open |
| Open / preview item | Present | **Partial** | [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) open |
| Server-driven toolbar/context menus | Present | **Partial** | [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) · [#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856) · [#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988) open; operators still report flat label buttons |
| Saved searches catalog + run | Present | **Partial** | [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) **Failed** · [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) open · [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Failed** |

**Explicitly defended Present (not flipped this slice):**

| Capability | Why still Present |
|------------|-------------------|
| Multi-select list | #3102 symptoms target toolbar/tree/search, not multi-select selection model; no Failed QA tracker cited for multi-select alone |
| Detail list of children | List chrome remains; Sites hierarchy failure is tracked on tree row (#2989), not a separate false Present for `DetailList` |
| Open / reveal from results | Callback wiring; operator outcome gated by Partial search/saved-search rows |
| Search in ContentBrowser hosts | Host `enableSearch` mount not cited as Failed in #3102; shell search/saved-search already Partial |
| Reduced create/rename/move/copy/delete | Not in #3102 false-Present table |
| Clipboard / site-subfolder wizards / dependency / relationships | Not in #3102 false-Present table for this slice (separate open QA may exist; re-audit if Failed) |
| Translation workflow (locales + create) | Intentional Present + Explicit OUT (#2829); human QA [#2649](https://github.com/intersoftdatalabs-in/percussioncms/issues/2649) Blocked — do not re-open OUT rows here |
| Object ACL editor (full) | Already **Partial** cross-epic (#2828) |

**Do not:** close human QA issues from this docs PR; re-implement Explorer chrome; flip back to Present until the linked QA issues pass (or product re-defends with evidence).

### 2026-08-10 Object ACL cross-epic disposition (#2828)

**Disposition:** Object ACL editor (full) is **not** open Explorer implement backlog under #2400. Product path is Developer-module `ObjectAclSection` under the CD-19 / community-visibility ACL epic chain.

| Layer | Tracker | State (2026-08-10) |
|-------|---------|-------------------|
| Grandparent | [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) Developer post-P0 | open (broader epic) |
| Parent | [#2262](https://github.com/intersoftdatalabs-in/percussioncms/issues/2262) Community visibility + full ACL | open |
| Slice B (CD-19 full ACL) | [#2274](https://github.com/intersoftdatalabs-in/percussioncms/issues/2274) | open tracker; implement children done |
| B1 specials | #2281 / [PR #2293](https://github.com/intersoftdatalabs-in/percussioncms/pull/2293) | **merged** |
| B2 default ACL prefs | #2282 / [PR #2294](https://github.com/intersoftdatalabs-in/percussioncms/pull/2294) | **merged** |
| B3 design/runtime model | #2283 / [PR #2342](https://github.com/intersoftdatalabs-in/percussioncms/pull/2342) | **merged** |
| B4 peer mounts | #2604 / [PR #2639](https://github.com/intersoftdatalabs-in/percussioncms/pull/2639) | **merged**; QA [#2640](https://github.com/intersoftdatalabs-in/percussioncms/issues/2640) open |
| B5 prefs + product-path Playwright | #2605 / [PR #2641](https://github.com/intersoftdatalabs-in/percussioncms/pull/2641) | **merged**; QA [#2643](https://github.com/intersoftdatalabs-in/percussioncms/issues/2643) open |
| Residual peer PW | #2642 / [PR #2671](https://github.com/intersoftdatalabs-in/percussioncms/pull/2671) | **merged**; QA [#2672](https://github.com/intersoftdatalabs-in/percussioncms/issues/2672) open |

**Matrix status rule for this row:** keep **Partial** while human QA is open — do **not** invent **Present** without QA closeout. Do **not** file new Explorer ObjectAclSection work under #2400; track only under #2274 / #2262 / #1690. Folder ACL on Explorer shell (#2410) remains a separate **Present** row.

### 2026-08-10 P-Trans OUT disposition + matrix Present (#2829)

- **#2829 residual (docs):** formal product **OUT** for in-flight translation queue + content-locale session under #2411. Research note: [p-trans-out-disposition.md](../research/p-trans-out-disposition.md). Inventory + residual questions updated.
- **Translation workflow** matrix row flipped **Partial → Present** for in-scope operator surface (item locales + create-variant). OUT capabilities listed under **Explicit OUT** — not silent Partial debt.
- **No implement** of translation queue / session without product re-open. Human QA for Present surface remains **#2649**.
- **Epic #2400 remaining-open criteria** documented in the OUT note (human QA set + product OUT; no agent implement spam for OUT rows).

### 2026-08-10 Present re-confirm after Subfolder/ContentBrowser merges (#2827)

Re-audited `main` after #2792 / [PR #2796](https://github.com/intersoftdatalabs-in/percussioncms/pull/2796) and #2793 / [PR #2798](https://github.com/intersoftdatalabs-in/percussioncms/pull/2798) landed (and after #2794 / [PR #2800](https://github.com/intersoftdatalabs-in/percussioncms/pull/2800) matrix closeout). **No status flips required** — product-route evidence still matches **Present**:

| Capability | Status | Product-route evidence on main |
|------------|--------|--------------------------------|
| Subfolder copy wizard | **Present** | `ContentExplorerShell` Content → Subfolder Copy (`content-subfolder-copy`) mounts `SubfolderCopyWizard`; Vitest shell/menu + Playwright `explorer-subfolder-copy.spec.js`; human QA #2797 |
| Search in ContentBrowser hosts | **Present** | `ContentBrowser` mounts shared `SearchPanel` when `enableSearch`; residual host `assetPickerModern.jsp` sets `enableSearch: true`; Vitest `ContentBrowser.test.tsx`; human QA #2799 |

Stale **Partial** / open-PR wording for those two chrome rows is incorrect after merge. Remaining intentional **Partial**: object ACL **cross-epic pointer** to #2274 / #2262 / #1690 (B1–B5 merged; human QA open — see #2828 note above), not Explorer implement backlog. Translation in-flight/session moved to **Explicit OUT** (#2829).

### 2026-08-10 closeout re-audit (#2794)

Re-audited product route on `main` after thrash merges. Matrix flips / cite updates:

| Capability | Was | Now | Evidence on main |
|------------|-----|-----|------------------|
| Workflow transitions in menus | Partial | **Present** | #2732 absorbed by cluster [PR #2748](https://github.com/intersoftdatalabs-in/percussioncms/pull/2748) (`ContentExplorerShell` + `workflowMenuActions` + `itemWorkflowApi`) |
| Menu bar / preview / view refresh | Present | **Present** (cites) | Same cluster #2748 absorbs #2731 + #2733 |
| Site copy | Present | **Present** (cites) | [PR #2773](https://github.com/intersoftdatalabs-in/percussioncms/pull/2773) merged |
| Relationships | Present | **Present** (cites) | [PR #2777](https://github.com/intersoftdatalabs-in/percussioncms/pull/2777) merged |
| Dependency viewer | Present | **Present** (cites) | [PR #2775](https://github.com/intersoftdatalabs-in/percussioncms/pull/2775) merged (no longer leave Partial) |
| Saved searches catalog + run | Partial | **Present** | #2505–#2507 merged; `SearchPanel` execute on product shell |
| Folder ACL + folder properties | Partial | **Present** | #2410 / [PR #2599](https://github.com/intersoftdatalabs-in/percussioncms/pull/2599) merged |
| Subfolder copy | Partial | **Present** | #2792 / [PR #2796](https://github.com/intersoftdatalabs-in/percussioncms/pull/2796) merged on product shell |
| ContentBrowser host search | Partial | **Present** | #2793 / [PR #2798](https://github.com/intersoftdatalabs-in/percussioncms/pull/2798) merged |
| Translation (in-flight/session) | Partial | **Present** + Explicit OUT | #2829 signed OUT for queue/session; locales+create Present (#2430) |

No product code residual filed this run — all Present rows above have product-route evidence on `main`.

### 2026-08-10 refresh (ContentBrowser search host #2793)

- **#2793 Search in ContentBrowser hosts:** `enableSearch` mounts shared `SearchPanel` (not a stub input). Open → selection for confirm; Reveal → folderPath navigation. Transport seams optional for Vitest. Primary residual host `assetPickerModern.jsp` sets `enableSearch: true`. Playwright `host-asset-picker` soft-skips when host fixture lacks the flag. Matrix row **Present**.

### 2026-08-10 refresh (site copy shell #2767)

- **#2767 Site copy wizard in Explorer shell chrome:** Content → Site Copy mounts `SiteCopyWizard` on `ContentExplorerShell` when selection is under `/Sites/<name>` (pure `sitePath` helper). Vitest shell/menu wiring + Playwright `explorer-site-copy.spec.js` (soft-skip multi-site submit on H2). Matrix row **Present**.

### 2026-08-10 refresh (subfolder copy shell #2792)

- **#2792 Subfolder copy wizard in Explorer shell chrome:** Content → Subfolder Copy mounts `SubfolderCopyWizard` on `ContentExplorerShell` when a non-root folder path is in context (pure `folderPath` helper; prefers selected folder row). Submit remains existing `pathApi.moveItem({copy:true})`. Vitest shell/menu wiring + Playwright `explorer-subfolder-copy.spec.js` (soft-skip multi-folder submit on H2). Matrix row **Present**.

### 2026-08-10 refresh (dependency viewer #2768)

- **#2768 Dependency viewer in Explorer shell:** View → Dependencies toggle on `ExplorerMenuBar`; `ContentExplorerShell` mounts `DependencyViewer` for the selected content item (relationship summary REST loaders reused; optional `loadDependencySummary` test seam). Select-item hint when no eligible selection. Unique panel/hint ids (`explorer-dependencies-panel` / `explorer-dependencies-hint`) mirror site-copy aria-controls. Vitest shell wiring + Playwright `explorer-dependencies.spec.js` (soft-skip deep counts without fixtures). Matrix row **Present**.

### 2026-08-10 refresh (menu bar #2731 + relationships #2769)

- **#2731 Explorer DCE top menu bar:** `ExplorerMenuBar` (Content / View / Help) on `ContentExplorerShell`; view tools nested under View (not multi-row flat buttons). `ActionToolbar` renders `children[]` as nested dropdowns (coordinates with #2730). Vitest + Playwright `explorer-menu-bar.spec.js`. Matrix row **Present**.
- **#2769 Relationships view in Explorer shell:** View → IA Relationships toggles `RelationshipsView` for the selected item (REST `/content-explorer/relationships/{id}/summary`); select-item hint when none. Vitest shell + menu bar; Playwright `explorer-relationships.spec.js`. Matrix row **Present**. Sibling DependencyViewer shell mount is **Present** (#2768).

### 2026-08-09 refresh (P-Trans UI #2430)

- **#2429 / PR #2601 merged:** public REST create-variant + item-locale list.
- **#2430 Explorer UI:** `TranslationsPanel` + shell toggle consumes REST; Vitest + surface Playwright. Later #2829: matrix **Present** for locales/create; in-flight + session under Explicit OUT.
- **#2407 / PR #2412 merged:** product shell composition, server action toolbar/context menu, search panel toggle, folder-valid display formats + column path, folder security toggle → **Present** (human QA then still open on #2588; later **Failed, closed 2026-08-18** — row is **Partial**, see #3109 / #3577 / #3619).
- **#2408 / PR #2522 merged:** multi-select list + clipboard panel in shell → **Present**.
- **#2504 / PR #2579 merged:** saved-search execute disposition = **façade**; later slices #2505–#2507 completed execute + UI + Playwright → matrix **Present** as of #2794 closeout.

### 2026-08-08 baseline

- Highest leverage was **compose shell** using existing components + `rest/actions` + `rest/displayformats` (landed).
- REST enhancement: `GET /rest/displayformats?validForFolder=true` (and optional `validForViewsAndSearches`).
- Re-audit after each slice merges; flip Partial → Present only when product route demonstrates the capability.
- **P-Trans (#2411):** inventory under `research/p-trans-api-inventory.md` (PR #2431). Public REST create-variant + item-locale list landed; Explorer UI locales/create Present; in-flight queue + session content-locale **OUT** signed #2829 ([p-trans-out-disposition.md](../research/p-trans-out-disposition.md)).
