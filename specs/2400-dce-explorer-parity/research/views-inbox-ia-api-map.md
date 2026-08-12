# Views + Inbox: SPA IA / API map (implement backlog)

**Grandparent epic:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Operator reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102)  
**Matrix Missing rows (docs):** [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) · [views-inbox-missing-disposition.md](./views-inbox-missing-disposition.md)  
**This slice:** [#3110](https://github.com/intersoftdatalabs-in/percussioncms/issues/3110) — inventory + recommended IA + PR-sized child backlog  
**Related:** [saved-search-execute-disposition.md](./saved-search-execute-disposition.md) (Views ≠ saved searches)  
**Inventory date:** 2026-08-11  
**Status:** Research complete. Matrix status stays **Missing** until product **IN / OUT / REDESIGN**. **No SPA feature implement in this PR.**

---

## Executive summary

| Capability | DCE | Public REST today | SPA Explorer product route | Matrix |
|------------|-----|-------------------|----------------------------|--------|
| **Views catalog (tree categories)** | System category **Views** → My / Community / All / Other Content | **List + detail only** (`GET /rest/views`, `GET /rest/views/{idOrName}`) — Developer UI-07 | **Absent** (View **menu** chrome is not the catalog) | **Missing** |
| **Views execute / list results** | Select view node → result list (field criteria or custom URL resource) | **No** `POST …/views/…/execute` (searches have execute; views do not) | **Absent** | **Missing** |
| **Inbox** | System **custom view** under `//Views//MyContent/Inbox` (`sys_cxViews/inbox`) | Same catalog surfaces Inbox as a `ViewDef` with `url` / custom-view flags; **no** CE Inbox shell entry | **Absent** | **Missing** |

**Product decision still required** (do **not** invent OUT): IN · OUT · REDESIGN — see #3102 / #3108. This note supplies the **concrete implement map** for when product chooses IN, and an **OUT template** for Inbox when product chooses OUT.

---

## 1. DCE inventory

### 1.1 Navigation tree (system)

Source: `system/cms/content/applications/sys_cx/ApplicationFiles/ContentExplorer.xsl`

```text
ContentExplorer (ROOT)
├── Sites
├── Folders
├── Views                          type=SystemCategory
│   ├── My Content                 type=SystemView  childrenurl=../sys_cxSupport/Views.html?sys_category=1
│   ├── Community Content          type=SystemView  childrenurl=../sys_cxSupport/Views.html?sys_category=2
│   ├── All Content                type=SystemView  childrenurl=../sys_cxSupport/Views.html?sys_category=3
│   └── Other Content              type=SystemView  childrenurl=../sys_cxSupport/Views.html?sys_category=4
└── Search Results                 (saved searches — separate category)
```

i18n keys: `psx.sys_cx.SystemCategory@Views`, `psx.sys_cx.SystemView@My Content` (and Community / All / Other).

### 1.2 Category → design parent

| `sys_category` | `PSSearch.ParentCategory` | Internal path prefix |
|----------------|---------------------------|----------------------|
| 1 | `MY_CONTENT` | `//Views//MyContent/` |
| 2 | `COMMUNITY_CONTENT` | `//Views//CommunityContent/` |
| 3 | `ALL_CONTENT` | `//Views//AllContent/` |
| 4 | `OTHER_CONTENT` | `//Views//OtherContent/` |

Enum: `PSSearch.ParentCategory` (`system/.../PSSearch.java`).

### 1.3 Category children load

| Piece | Path / role |
|-------|-------------|
| HTML entry | `sys_cxSupport/Views.html?sys_category={1-4}` |
| SQL (community-filtered) | `sys_cxSupport.xml` — `PSX_SEARCHES.TYPE = 'View'` AND `PARENTCATEGORY = :sys_category` AND community property in `(-1, session community)` |
| Node XSL | `sys_cxSupport/ApplicationFiles/Views.xsl` — each `View` → tree `Node` with `sys_displayformat`, `sys_search`; special **iconkey** for `Inbox` / `Outbox` |

### 1.4 Built-in / custom-URL views (Inbox family)

Design fixture: `system/webservices/test/.../PSSearches_Views.xml` and runtime app `sys_cxViews`.

| Internal name (typical) | Role | Custom URL (typical) | Notes |
|-------------------------|------|----------------------|-------|
| **Inbox** | Assignments / to-do for current user roles | `../sys_cxViews/inbox.xml` | **Not a separate top-level CE root** — child of **Views → My Content** |
| Outbox | Related workflow outbox | `../sys_cxViews/outbox.xml` | Same pattern |
| Recent | Recent items | `../sys_cxViews/recent.xml` | Same pattern |
| Field-criteria views | e.g. View_All | (no custom URL / standard view) | Executable via design search engine path |

Constants (`PSContentExplorerConstants`):

| Constant | Value |
|----------|--------|
| `PATH_MYCONTENT` | `//Views//MyContent/` |
| `PARAM_PATH_INBOX` | `//Views//MyContent/Inbox` |
| `PARAM_PATH_OUTBOX` | `//Views//MyContent/Outbox` |
| `PARAM_PATH_RECENT` | `//Views//MyContent/Recent` |

**IA implication:** SPA “Inbox” parity is **Views catalog parity + ability to run the Inbox custom view**, not a free-floating notifications tray unless product **REDESIGN**s.

### 1.5 View vs search object model

Both design catalogs store definitions as `PSSearch` rows with different `TYPE`:

| | Views | Saved searches |
|--|-------|----------------|
| Type | `TYPE_VIEW` (`"View"`) | user/standard/custom search types |
| Standard | `isView() && !isCustomApp()` → `isStandardView()` | field-criteria searches |
| Custom URL | `isCustomView()` | `isCustomSearch()` |
| Parent category | 1–4 (My/Community/All/Other) | search categories (separate tree under Search Results) |

---

## 2. Public REST / adaptor inventory (today)

### 2.1 Views catalog (UI-07) — list/detail plus standard execute (#3115)

| Method | Servlet path | WebUI client path | Resource / adaptor |
|--------|--------------|-------------------|--------------------|
| `GET` | `/rest/views` | `/services/views` | `ViewResource.listViews` → `IViewAdaptor.listViews` |
| `GET` | `/rest/views/{idOrName}` | `/services/views/{idOrName}` | `ViewResource.getView` → `findViewByKey` |
| `POST` | `/rest/views/{idOrName}/execute` | `/services/views/{idOrName}/execute` | `ViewResource.executeView` → `IViewAdaptor.executeView` (standard views only; custom URL → 400 / #3118) |

| Layer | Path |
|-------|------|
| Resource | `rest/src/main/java/com/percussion/rest/views/ViewResource.java` |
| Adaptor interface | `rest/.../views/IViewAdaptor.java` |
| Wire DTO | `ViewDef`, `ViewFieldSummary` |
| Production adaptor | `projects/sitemanage/.../ViewAdaptor.java` → `IPSUiDesignWs.findViews` / `loadViews` |
| Tests | `ViewResourceTest`, `TestViewAdaptor`, `ViewAdaptorMapTest`, `ViewAdaptorSafeKeyTest` |
| Developer SPA | `WebUI/.../api/developer/viewsApi.ts`, `ViewsPanel.tsx`, `ViewDetailPanel.tsx` |
| PATHS | `PATHS.VIEWS` → `${SERVICES_ROOT}/views` |

**`ViewDef` fields used for IA:** `name`, `label`, `parentCategory`, `displayFormatId`, `url`, `standardView` / `customView` / `view`, `fields[]`, `designGaps[]` (detail only).

**Design gaps stamped by adaptor (detail):** create/update/delete unsupported; field criterion edit unsupported; searches are separate catalog (UI-06).

**Safety:** `ViewAdaptor.isSafeViewKey` rejects blank, `..`, `/`, `\`, NUL (same pattern as searches).

**HTTP:** 200 list/detail; 404 missing; 503 adaptor null; 500 unexpected.

### 2.2 Searches execute peer (do **not** call this a Views execute)

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/rest/searches/{idOrName}/execute` | Design-search façade; **rejects custom URL searches** (`isCustomSearch()` → 400) |
| Client | `POST /services/searches/{id}/execute` | Explorer `SearchPanel` saved-search run |

`SearchAdaptor` designGaps explicitly: *“Views are a separate catalog (Developer Views / UI-07)”*.  
Calling search execute with a view name is **not** a supported product contract (different design catalog + GUID type `VIEW_DEF`).

### 2.3 Gaps vs DCE run

| Need | Status |
|------|--------|
| List views (all communities filter server-side today) | **Present** (Developer + REST) |
| Filter list by `parentCategory` (1–4) for tree children | **Client can filter** `ViewDef.parentCategory`; optional query param later if payload large |
| Execute **standard** view (field criteria) | **Missing** public REST — recommend façade peer of searches |
| Execute **custom URL** view (Inbox/Outbox/Recent/…) | **Missing** — searches façade explicitly refuses custom URL; needs dedicated strategy |
| Community scoping parity with DCE SQL | REST list uses design WS load (session user); re-verify community filter vs DCE `sys_community` property when implementing |
| Explorer product-route tree | **Missing** |
| Playwright / a11y on Explorer Views | **Missing** |

---

## 3. Recommended SPA IA (if product chooses **IN**)

### 3.1 Navigation model (prefer DCE parity)

```text
ExplorerTree (product shell)
├── Sites / Folders / … (existing)
└── Views                          NEW system category node (i18n)
    ├── My Content                 group by parentCategory=1
    │   ├── Inbox                  custom view — special icon / testid
    │   ├── Outbox                 optional v1
    │   ├── Recent                 optional v1
    │   └── … other My Content views
    ├── Community Content          parentCategory=2
    ├── All Content                parentCategory=3
    └── Other Content              parentCategory=4
```

**Selection behavior:** selecting a **view leaf** runs that view and replaces (or overlays) the main **detail list** with result rows (same open/reveal affordances as saved-search results / folder list). Selecting a **category** expands children only (no execute).

**Do not:**

- Put design Views only under Developer and call Explorer “done”
- Conflate with `ExplorerMenuBar` **View** menu (Refresh / panel toggles)
- Conflate with display-format column selector
- Treat saved-search catalog as Views catalog

### 3.2 Alternate **REDESIGN** IA (only with product acceptance)

| Option | Shape | When |
|--------|-------|------|
| R1 | Single “Views” picker under Search panel (flat list, group headers) | Prefer smaller chrome; loses DCE tree muscle memory |
| R2 | Inbox-only shortcut in chrome; full Views catalog deferred | Product wants Inbox urgently without full catalog |
| R3 | OUT Explorer Views; operators use Developer catalog only | Explicit OUT + sign-off issue (see §6) |

Any redesign must update gap-matrix status + acceptance **before** implement.

### 3.3 Component / module sketch (implement children)

| Layer | Suggested home | Peer pattern |
|-------|----------------|--------------|
| Client API | `WebUI/.../api/contentExplorer/viewsApi.ts` (or extend developer API with execute) | `searches` execute + `viewsApi` list |
| Tree | `ExplorerTree` / shell nav data source | Sites/folders loaders |
| Results | Reuse detail list / search result row open-reveal | `SearchPanel` saved-search results |
| Shell wiring | `ContentExplorerShell` | Saved search / translations panels |
| i18n | `EXPLORER_MSG` (+ existing `psx.sys_cx.*` keys if mapped) | [i18n-a11y-hard-gate.md](../checklists/i18n-a11y-hard-gate.md) |
| Playwright | `modules/perc-qa-automation/frontend/tests/explorer-views*.spec.js` | `explorer-saved-search.spec.js` |

---

## 4. Recommended REST / execute disposition (if product **IN**)

### 4.1 Standard (field-criteria) views — **façade** (recommended)

Mirror saved-search disposition:

| | |
|--|--|
| **Disposition** | **Façade** `POST /rest/views/{idOrName}/execute` |
| **Rationale** | Same as searches: design object carries operators, DF, max results; client Map runtime loses operators; views already load as full `PSSearch` in `ViewAdaptor` |
| **Body / result** | Reuse or twin `SearchExecuteRequest` / `SearchExecuteResult` shapes (Explorer-ready rows) — prefer shared DTO package only if it does not blur catalog boundaries |
| **Reject** | Unsafe keys (same as list); missing view → 404 |
| **Companion closure** | rest resource + `IViewAdaptor` method + sitemanage impl + Mockito + Spring test stub + sitemanage unit tests + module `mvnw clean install` |

Sketch:

```text
POST /rest/views/{idOrName}/execute
  body: { startIndex?, maxResults?, sortColumn?, sortOrder?, folderPath? … }
  → { children: SearchResultItem[], totalCount, startIndex, viewName, displayFormatId }
```

**Implementation note:** load via `IPSUiDesignWs` **views** path (or existing `findViewByKey` → full `PSSearch`), then same executable search runner as `SearchAdaptor.runDesignSearch`. Do **not** route through `listSearches` / search catalog keys.

### 4.2 Custom-URL views (Inbox / Outbox / Recent / …) — **separate strategy**

| Option | Meaning | Recommendation |
|--------|---------|----------------|
| **C1** | New public runner that invokes classic app resource (`sys_cxViews/inbox` etc.) and maps rows to Explorer items | Needed for true Inbox parity |
| **C2** | v1: 400 with clear message for `customView`; document residual | Acceptable **only** if product accepts Inbox **Partial** until C1 |
| **C3** | OUT custom-URL family; keep standard views only | Requires product OUT for Inbox (and peers) |

**Default backlog assumption if Views IN:** ship **§4.1** first; file/keep Inbox child for **C1** unless product OUT/REDESIGN.

### 4.3 Catalog REST “if needed”

| Enhancement | Needed? |
|-------------|---------|
| Existing list/detail | **Sufficient** for v1 tree (client group by `parentCategory`) |
| `?parentCategory=` query | Optional performance / parity polish |
| Write CRUD | **Out of scope** for Explorer parity (Developer later) |

---

## 5. Inbox disposition map

### 5.1 What Inbox **is** in DCE

- A **named system view** under **Views → My Content**, not a separate CE root.
- Implemented as **custom URL** view → `sys_cxViews` inbox resource (SQL over workflow assignment / show-in-inbox roles).
- Workflow model also has `ShowInInbox` on assigned roles (`PSAssignedRole`) — server data for who sees items in that view.

### 5.2 What Explorer has today

- Workflow **transition** menus on selection (`itemWorkflowApi`) — **not** an Inbox list.
- No tree node, no `//Views//MyContent/Inbox` path, no custom-URL execute.

### 5.3 Product choices (template)

| Choice | Matrix outcome | Engineering follow-through |
|--------|----------------|----------------------------|
| **IN (parity)** | Views **Missing→Partial→Present** with Inbox as leaf under My Content; or separate Inbox row Present when custom-URL execute works | Implement Views tree + §4.1 + custom-URL runner (C1) for Inbox at minimum |
| **IN (Inbox shortcut only)** | Inbox **Partial/Present** via R2; Views may stay Missing until full catalog | Explicit REDESIGN acceptance on #3102/#2400 |
| **OUT** | Gap-matrix **Explicit OUT** + sign-off issue (peer #2829 style) | No Explorer implement; document operator path (e.g. DCE-only / other) |
| **Silent omit** | **Forbidden** | — |

### 5.4 OUT reason template (copy into sign-off issue when product chooses OUT)

```markdown
## Product OUT: Explorer Inbox parity

**Parent:** #3102 / #2400
**Capability:** CE Inbox (DCE `//Views//MyContent/Inbox` / sys_cxViews inbox)
**Decision:** OUT for 8.2 SPA Explorer
**Reason:** <product one-paragraph reason>
**Operator alternative:** <e.g. remain on DCE; use workflow queues elsewhere; …>
**Re-open criteria:** product sign-off + typed REST/UI acceptance for custom-URL views or redesign
**Related Views catalog:** <IN still / OUT still / separate>
**Signed by:** <name> **Date:** <ISO date>
```

---

## 6. PR-sized child backlog (filed from #3110)

Matrix stays **Missing** until product marks IN/OUT. Children are ready so IN does not stall on planning. **Do not start implement children without product IN** (or explicit REDESIGN acceptance) except pure docs/OUT sign-off.

| Order | Issue | Title | Modules | Acceptance (summary) | Depends |
|------:|-------|-------|---------|----------------------|---------|
| V1 | [#3115](https://github.com/intersoftdatalabs-in/percussioncms/issues/3115) | Views REST execute façade (standard views) | `rest`, `projects/sitemanage` | `POST /rest/views/{idOrName}/execute`; safe keys; 404/400/503; Mockito + Spring stub + adaptor tests; module clean install; custom views **documented** (400 or deferred to Inbox child) | Product **IN** (or spike-only if product asks) |
| V2 | [#3116](https://github.com/intersoftdatalabs-in/percussioncms/issues/3116) | Explorer Views tree + run results UI | `WebUI` (contentExplorer) | Tree category nodes 1–4; list from `GET /services/views`; run via V1; results open/reveal; Vitest shell; i18n keys; **no** Present matrix flip without QA | V1 + product **IN** |
| V3 | [#3117](https://github.com/intersoftdatalabs-in/percussioncms/issues/3117) | Explorer Views Playwright + a11y surface | `modules/perc-qa-automation` | Surface spec soft-skip when no fixtures; `expectNoSeriousA11yViolations`; testids stable | V2 |
| I1 | [#3118](https://github.com/intersoftdatalabs-in/percussioncms/issues/3118) | Inbox: implement custom-URL execute + shell leaf **or** signed OUT | `rest`/`sitemanage`/`WebUI` **or** docs-only OUT | Either C1 runner + My Content → Inbox leaf + tests, **or** OUT note under Explicit OUT with template §5.4 | Product **IN** for implement **or** product **OUT** for docs |

Also tracked on **#3102 / #3110 / #2400 Agent progress**.

### Size / anti-padding

- V1 / V2 / V3 / I1 are independently reviewable PRs (same grain as saved-search #2505–#2507).
- Do **not** split V2 into micro “add label only” issues.
- If product **OUT** both Views and Inbox, close implement children as not-planned and keep only signed OUT docs (peer #2829).

---

## 7. Matrix / plan interaction

| Doc | Role |
|-----|------|
| [gap-matrix.md](../contracts/gap-matrix.md) | Rows **Views (DCE navigation category)** + **Inbox** stay **Missing** until product disposition (#3108). Do **not** claim Present from Developer catalog or View menu. |
| [views-inbox-missing-disposition.md](./views-inbox-missing-disposition.md) | Why Missing + product options |
| **This note** | API/IA map + child implement backlog |
| [plan.md](../plan.md) | Points here for Views/Inbox follow-on order |
| [saved-search-execute-disposition.md](./saved-search-execute-disposition.md) | Peer façade pattern; explicitly excludes Views execute |

---

## 8. What this slice (#3110) does **not** do

- SPA Explorer Views/Inbox UI
- REST execute implementation
- Product IN/OUT/REDESIGN sign-off
- Flipping matrix to Present/Partial/OUT without product
- Architecture navons (#3092)
- Reopening P-Trans OUT (#2829)

---

## 9. Linked trackers

| Role | Issue |
|------|-------|
| Grandparent parity | [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400) |
| Operator reality-check | [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102) |
| Matrix Missing docs | [#3108](https://github.com/intersoftdatalabs-in/percussioncms/issues/3108) |
| False Present vs Failed QA | [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109) |
| This IA/API map + child filing | [#3110](https://github.com/intersoftdatalabs-in/percussioncms/issues/3110) |
| Views V1 execute REST | [#3115](https://github.com/intersoftdatalabs-in/percussioncms/issues/3115) |
| Views V2 Explorer tree/UI | [#3116](https://github.com/intersoftdatalabs-in/percussioncms/issues/3116) |
| Views V3 Playwright/a11y | [#3117](https://github.com/intersoftdatalabs-in/percussioncms/issues/3117) |
| Inbox I1 implement or OUT | [#3118](https://github.com/intersoftdatalabs-in/percussioncms/issues/3118) |
| Developer Views catalog (not CE nav) | [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690) UI-07 |

---

## Change log

| Date | Note |
|------|------|
| 2026-08-11 | #3110: full DCE/REST/SPA inventory; recommended IA; execute façade disposition; Inbox as Views→My Content custom view; PR-sized V1–V3 + I1 backlog; matrix remains Missing. |

> Co-Authored by Grok Build using grok-4.5 with agent main.
