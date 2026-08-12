# Architecture / Navigation editor — CM1 feature inventory + API map

**Status:** design complete (Slice A — inventory only; **no UI implement**)  
**Issue:** [#3093](https://github.com/intersoftdatalabs-in/percussioncms/issues/3093) (Slice A)  
**Parent epic:** [#3092](https://github.com/intersoftdatalabs-in/percussioncms/issues/3092)  
**Date:** 2026-08-11  
**Operator:** night-issue-prs / Grok (model grok-4.5)

---

## 1. Problem statement

CM1 **Architecture** (site navigation / **navon** tree editor) has **no React + TypeScript** implementation. Top nav deep-links to the legacy page (`/cm/app/?view=arch` → `siteArchitecture.jsp`), which keeps the old chrome and is **non-functional** as the primary operator path under the modern SPA shell.

Operators need a first-class SPA surface to manage site navigation trees (sections / navons / landing pages / section links / external links) with the same standards as Explorer, Publish, Admin, and Developer.

**This document (Slice A)** inventories CM1 capabilities, maps every operator action to REST/services, documents the broken top-nav path, and recommends SPA information architecture for Slice B. **Out of scope:** any UI implementation.

---

## 2. Legacy surface inventory

### 2.1 Entry points

| Surface | Path / artifact | Notes |
|---------|-----------------|-------|
| Top nav **Architecture** | `WebUI/src/main/ts/app/layout/TopNav.tsx` | Hard `<a href="/cm/app/?view=arch">` — **not** a SPA `NavLink` |
| Top-nav visibility | `topNavConfig.ts` → `topNavItemIds` | Shown when `isAdmin \|\| isDesigner` (same gate as Publish / Design / Developer) |
| Dispatcher | `WebUI/src/main/webapp/cm/app/index.jsp` | `legacyViews.put("arch", "siteArchitecture.jsp")` — **not** in `spaViews[]` |
| Role gates (server) | `index.jsp` `adminViews` / `designerViews` | `arch` requires Admin **or** Designer |
| Legacy host JSP | `…/cm/app/siteArchitecture.jsp` | Full CM1 chrome: header, Finder, site map canvas |
| Packed assets | `perc_architecture.packed.min.js` / `.css` | Built from `static-bundles.json` + Vite legacy config |
| SPA routes | `WebUI/src/main/ts/app/routes.tsx` | **No** `/architecture` (or alias) |
| SPA entry allowlist | `deepLinks/allowlists.ts` `SPA_ENTRIES` | **No** `architecture` entry |
| Homepage type | `IPSUserService.HOMEPAGE_TYPE_ARCHITECTURE` | Canonical type `"Architecture"`; aliases `arch`, `architecture`, `site_arch`, `sitearch` |

**Canonical webapp tree:** `WebUI/src/main/webapp/cm/…` (tests and SPA cutover assert this tree). `WebUI/war/…` may lag as a deploy/sync artifact — prefer `src/main/webapp` for source truth.

### 2.2 Page composition (`siteArchitecture.jsp`)

On ready:

1. Reads `site` request parameter; validates via `SecureStringUtils.isValidString`.
2. If `site` is non-empty: initializes `$("#perc_site_map").perc_site_map({ site, onChange → finder.refresh })`.
3. If `site` is empty: shows inline help only (`perc.ui.site.architecture@Work On Navigation` or create-site message when `hasSites` is false).
4. Always mounts legacy Finder via `$.Percussion.PercFinderView()` with `openedObject=PERC_SITE`.
5. Includes CM1 header with `mainNavTab=architecture`.

**Debug mode** (`?debug=true`) loads individual scripts; production uses packed min bundles.

### 2.3 Core widget: `perc_site_map`

**Files:**

- `WebUI/src/main/webapp/cm/widgets/perc_site_map.js` (and war twin)
- CSS: `perc_site_map.css`

**UX model:** horizontal multi-level **site map** (not a left-rail tree). Root section expands; children appear as boxes on lower levels with connector lines. Features:

| Capability | UI affordance | Notes |
|------------|---------------|-------|
| Load root + expand | Auto on init | `getRootSection` then `expandSection` → `getChildren` |
| Expand / collapse section | Arrow on box | Loads children; removes deeper levels on collapse |
| Select section | Click box | Enables Move menu item |
| Add child section | “+” image button | Opens `perc_newSectionDialog` |
| Configure section | gear/config button | Level > 1 → `perc_editSectionDialog`; root → `perc_editSiteSectionDialog` |
| Delete section / link | delete image button | Section link / external link / convert-to-folder paths differ |
| Drag-reorder / reparent | jQuery UI draggable + drop areas | `SECTION_MOVE` |
| Assign landing page | Drop Finder PAGE listing onto section box | `replaceLandingPage` |
| Move section (menu) | Action menu “Move Section” | `PercSectionTreeDialog` pick target parent |
| Copy site | Action menu Copy | `PercCopySiteDialog` + site service (site-level, not navon edit) |
| Delete site | Action menu Delete | Site delete with dirty check (site-level) |

Section type badges: regular section, **Section Link**, **External Link** (and server enum also has **blog**).

### 2.4 Dialogs / plugins (architecture pack)

From `static-bundles.json` → `jslibMin/perc_architecture.packed.js`:

| Script | Role |
|--------|------|
| `perc_sectionServiceClient.js` | JSON client for all section REST ops |
| `PercSiteService.js` | Site list / import / copy / delete helpers |
| `PercFolderService.js` | Folder listing for convert-folder landing pages |
| `perc_newSectionDialog.js` | Create: section, section link, external link, convert folder |
| `perc_editSectionDialog.js` | Section preferences (title, folder, target window, CSS classes, login groups) |
| `perc_editSiteSectionDialog.js` | Root / site-level section edit |
| `PercEditSectionLinksDialog.js` | Edit section-link / external-link targets |
| `PercSectionTreeDialog.js` | Tree picker (move target, section-link target) |
| `PercCopySiteDialog.js` | Copy site |
| `perc_newsitedialog.js` | Create site (when no sites / from architecture) |
| `perc_save_as.js`, ChangePw / email dialogs | Shared CM1 chrome helpers |

Related (not architecture-exclusive): Finder widgets, path selection dialog, path constants.

### 2.5 New Section dialog — create modes

`perc_newSectionDialog` type select:

| Mode | UI fields | Service call |
|------|-----------|--------------|
| **Section & landing page** | page name, title, template, target window | `POST …/section/create` (`CreateSiteSection`) |
| **Section link** | target section (tree browse) | `GET …/section/createSectionLink/{target}/{parent}` |
| **External link** | link text, URL, target window | `POST …/section/createExternalLinkSection` |
| **Convert folder** | folder path + landing page select | `POST …/section/createSectionFromFolder` |

### 2.6 Edit Section dialog — properties

Loads `GET …/section/properties/{id}` → edits → `POST …/section/update` with `SiteSectionProperties`:

- `title`, `folderName`, `target` (`_self` / `_blank` / `_top` / `_parent`)
- `cssClassNames` (nav widget classes)
- `requiresLogin`, `allowAccessTo` (secure section groups)
- `folderPermission` (folder ACL)
- Flags: `secureSite`, `secureAncestor`, `siteRootSection`

### 2.7 Content model (navons / Managed Nav)

A **site section** is the CM1 authoring abstraction over three repository pieces (see `PSSiteSection` javadoc):

1. **Folder** under `//Sites/{site}/…`
2. **Landing page** (page item; link title often mirrors nav title)
3. **Navon** content item (`percNavon`; Managed Nav also knows classic `rffNavon`)

| Concept | Detail |
|---------|--------|
| Content type (product) | `percNavon` — label “Navon” (public REST content-types examples) |
| Related type | `percNavImage` — nav image assets |
| Classic FF | `rffNavon` still appears in docs/examples |
| Runtime assembly | `modules/extensions-nav` FastForward Managed Nav (`IPSManagedNavService`, navtree extensions) |
| Touch config | `touchItemConfig.xml` targets `percNavon`, `percNavImage` |
| Section types (`PSSectionTypeEnum`) | `section`, `sectionlink`, `externallink`, `blog` |
| Target window (`PSSectionTargetEnum`) | `_self`, `_blank`, `_top`, `_parent` |
| DTO | `PSSiteSection`: `id` (navon GUID string), `title`, `folderPath`, `childIds`, `sectionType`, `target`, `externalLinkUrl`, login/CSS fields |
| Tree DTO | `PSSectionNode` for full-tree load |
| Thin aggregate | `PSSiteArchitecture` — site name + section list via `/siteArchitecture/{id}` |

**Authoring vs runtime:** Architecture UI mutates structure through **sitemanage section REST**. Publishing/nav widgets consume Managed Nav at assembly time. Epic non-goal: redesign Managed Nav runtime.

---

## 3. API / service matrix

**Base (legacy JS):** `/Rhythmyx/services/sitemanage` (`SERVICES.SITEMGT` in `perc_path_constants.js`).  
**Modern SPA clients** should resolve services root the same way as Explorer (`detectServicesRoot()` / shared `paths.ts` pattern) — typically `/Rhythmyx/services/…` or proxy-equivalent.

**Primary resource:** `PSSiteSectionRestService` `@Path("/section")`  
**Impl:** `PSSiteSectionService` / `IPSSiteSectionService` in `projects/sitemanage`.

### 3.1 Action → endpoint map

| Operator action | Client (`Perc_SectionServiceClient` / map) | HTTP | Path (under sitemanage) | Request / response | Gap? |
|-----------------|--------------------------------------------|------|-------------------------|--------------------|------|
| Load root section | `getRootSection(site)` | GET | `/section/root/{siteName}` | → `PSSiteSection` | **OK** |
| Load children | `getChildren(section)` | POST | `/section/childSections` | body `PSSiteSection` → list | **OK** (POST with section body is legacy shape) |
| Load full tree | `getTree(siteid)` | GET | `/section/tree/{siteName}` | → `PSSectionNode` | **OK** (prefer for SPA read-only) |
| Load one section | `getSection(id)` | GET | `/section/{id}` (`LOAD_PATH`) | → `PSSiteSection` | **OK** |
| Section properties | edit dialog | GET | `/section/properties/{id}` | → `PSSiteSectionProperties` | **OK** |
| Create section + landing | `create` | POST | `/section/create` | `PSCreateSiteSection` | **OK** |
| Create external link | `create` (alt) | POST | `/section/createExternalLinkSection` | `PSCreateExternalLinkSection` | **OK** |
| Create section link | `createSectionLink` | GET | `/section/createSectionLink/{target}/{parent}` | → `PSSiteSection` | **OK** but **GET for mutation** — SPA should keep wire parity or add POST later |
| Create from folder | `convertFolder` | POST | `/section/createSectionFromFolder` | `PSCreateSectionFromFolderRequest` | **OK** |
| Update section props | `edit` | POST | `/section/update` | `PSSiteSectionProperties` | **OK** |
| Update section link | `updateSectionLink` | POST | `/section/updateSectionLink` | `PSUpdateSectionLink` | **OK** |
| Update external link | `updateExternalLink` | POST | `/section/updateExternalLink/{sectionGuid}` | `PSCreateExternalLinkSection` | **OK** |
| Move / reorder | `move` | POST | `/section/move` | `PSMoveSiteSection` (`sourceId`, `targetId`, `targetIndex`) | **OK** |
| Replace landing page | `replaceLandingPage` | POST | `/section/replaceLandingPage` | `PSReplaceLandingPage` | **OK** |
| Delete section | `deleteSection` | DELETE | `/section/{id}` | void | **OK** |
| Delete section link | `deleteSectionLink` | GET | `/section/deleteSectionLink/{section}/{parent}` | `PSNoContent` | **OK** but **GET for mutation** |
| Convert section → folder | `convertSectionToFolder` | DELETE | `/section/convertToFolder/{id}` | void | **OK** |
| Blogs for site | (section service) | GET | `/section/blogs/{siteName}` | list | Optional (blog type) |
| Blog posts | | GET | `/section/blogPosts/{id}` | | Optional |
| All blogs | | GET | `/section/allBlogs` | | Optional |
| Site architecture aggregate | path const `SITE_ARCHITECTURE` | GET | `/siteArchitecture/{id}` | `PSSiteArchitecture` | Thin; map uses **section** APIs primarily |
| Site list / summary | `PercSiteService` | existing site REST | `/sitemanage/…` site resources | Needed for site picker | **OK** (reuse Publish/Explorer site list patterns) |
| Folder browse (convert) | `PercPathService` / pathmanagement | existing | pathmanagement | Needed for convert-folder | **OK** |
| Delete site / copy site | site services + dialogs | existing | site REST | Architecture chrome, not core nav tree | **OK** — may stay site admin / Publish |

### 3.2 Gaps / tech notes (no blocker for Slice B shell)

| Gap | Severity | Recommendation |
|-----|----------|----------------|
| No public typed TS client for section REST | Medium | Slice C/D: add `WebUI/src/main/ts/api/architecture/sectionApi.ts` mirroring pathmanagement style |
| `createSectionLink` / `deleteSectionLink` use GET | Low | Keep wire-compatible first; optional later POST aliases |
| `childSections` is POST with full section body | Low | Keep as-is; SPA tree load should prefer `/section/tree/{site}` |
| Jackson / XML dual `@Produces` | Info | Prefer `Accept: application/json` like other SPA clients |
| Nested JSON root names (`SiteSection`, `CreateSiteSection`, …) | Info | Match existing Betwixt/Jackson root naming used by CM1 |
| `/siteArchitecture/{id}` underused by UI | Info | Optional; do not block on it |
| Blog section type | Low | Support in DTO; full blog UX can follow after core section parity |
| Secure section groups UI | Medium | Needs roles/groups catalog already used by folder props / users |

**Conclusion:** Core nav authoring **does not require new server REST** for CM1 parity. Prefer existing sitemanage section APIs. Add REST only if SPA discovers non-public endpoints or needs cleaner contracts (POST aliases, bulk move).

---

## 4. Broken top-nav path — repro and root causes

### 4.1 Repro (current 8.2 SPA shell)

1. Sign in as Admin or Designer.
2. From SPA chrome, click top-nav **Architecture** (`data-testid="nav-architecture"`).
3. Browser navigates to **`/cm/app/?view=arch`** (full page load — leaves SPA shell).
4. `index.jsp` treats `arch` as **legacy view** → forwards `siteArchitecture.jsp`.
5. **No `site=` query param** is supplied by TopNav.
6. JSP branch with empty site renders **help text only** inside `#perc_site_map` (no map, no sections).
7. Legacy Finder/header mount; operator cannot manage navons from a working modern editor.

Even when a site is later selected via Finder (if that path still works in the environment), UX remains **old CM1 chrome**, packed jQuery UI 1.8, and depends on Finder + architecture pack integrity.

### 4.2 Root-cause stack

| # | Cause | Evidence |
|---|-------|----------|
| 1 | Architecture not on SPA | `routes.tsx` / `SPA_ENTRIES` omit architecture; `index.jsp` `legacyViews` keeps `arch` |
| 2 | TopNav hard-links out of SPA | `TopNav.tsx` `<a href="/cm/app/?view=arch">` vs `NavLink` for Home/Explorer/… |
| 3 | Missing site context | JSP requires `site` query param; TopNav never passes it |
| 4 | Legacy host fragility | Depends on packed min bundles, Finder, `JavaScriptServlet` CSRF, jQuery widgets |
| 5 | Product expectation mismatch | Operators expect SPA-parity feature; receive dead-end legacy page |

### 4.3 What still works (server-side)

Section REST and Managed Nav services are **not** the primary failure mode. Inventory shows a complete mutation API. The break is **front-door + host page + missing SPA**, not absence of `/section/*`.

---

## 5. Recommended SPA information architecture (Slice B input)

### 5.1 Routes and deep links

| Item | Recommendation |
|------|----------------|
| Client route | `/architecture` (primary) |
| Site-scoped route | `/architecture/:siteName` (optional; encode safely) |
| SPA entry token | `architecture` (add to `SPA_ENTRIES` + `index.jsp` `spaViews` when cut over) |
| Legacy `?view=arch` | Redirect to `spa.jsp?entry=architecture` (and preserve `site` if present) once shell ships |
| Homepage type | Keep `HOMEPAGE_TYPE_ARCHITECTURE` / `"Architecture"`; resolve to SPA entry |

### 5.2 Role gates

Match current product gates (do not widen or narrow without product decision):

| Layer | Gate |
|-------|------|
| Top nav | `isAdmin \|\| isDesigner` (`topNavConfig.ts` — already groups Architecture with Publish/Design) |
| Server `index.jsp` | `arch` in `adminViews` + `designerViews` |
| SPA route guard | Same as Publish/Design: Admin or Designer; others → unavailable / default home |
| REST | Existing sitemanage security on section/site services (no new ACL surface for Slice B) |

### 5.3 Shell layout (IA sketch)

```
AppLayout
└── ArchitectureShell          // product chrome, title, a11y landmarks
    ├── SitePicker             // required context; list sites user can see
    ├── Toolbar                // refresh, expand/collapse, optional Create site (Admin)
    └── NavTreePanel           // keyboard tree (not only visual map)
        ├── Tree (read → C)
        ├── Context menu / actions (mutations → D)
        └── Drawers/modals: New Section, Edit Section, Move, Section Link, External Link, Landing (→ D/E)
```

**Visual map vs tree:** CM1 uses a spatial site-map. For SPA, prefer an **accessible tree** as primary (keyboard, ARIA tree), with optional later map visualization. Parity is **capabilities**, not pixel-perfect boxes.

### 5.4 Client module layout (suggested)

```
WebUI/src/main/ts/
  architecture/
    ArchitectureShell.tsx
    NavTree.tsx
    SitePicker.tsx
    dialogs/…                 // Slice D/E
  api/architecture/
    sectionApi.ts             // typed section REST
    types.ts                  // PSSiteSection mirrors
  app/routes/ArchitectureRoute.tsx
  app/layout/TopNav.tsx       // NavLink to /architecture
  app/deepLinks/allowlists.ts // + architecture
```

Register lazy shell in `registry.ts` like Admin/Publish/Explorer.

### 5.5 Slice mapping (B–G) — implementation checklist

| Slice | Issue | Deliverable | Depends on this inventory |
|-------|------:|-------------|---------------------------|
| **B** SPA shell + routing | #3094 | Route `/architecture`, TopNav `NavLink`, entry allowlist, role gate, empty/site-picker shell, `?view=arch` → SPA redirect when ready | §5 |
| **C** Read-only nav tree | #3095 | Site picker + load `GET /section/tree/{site}` (or root+children), render accessible tree, refresh | §3 root/tree/children |
| **D** Structure editing | #3096 | Create section, edit props, move/reorder, delete, convert to folder | §3 create/update/move/delete |
| **E** Landing & section-link parity | #3097 | Replace landing (page pick), section link create/edit/delete, external link create/edit | §3 landing + link endpoints; Finder/page pick patterns |
| **F** a11y + i18n + docs + QA | #3098 | TMX `perc.ui.*`, landmarks/keyboard tree, product-docs, Playwright smoke | All UI slices |
| **G** Legacy retirement | #3099 | Remove/redirect `siteArchitecture.jsp` pack entry; drop legacy TopNav href; retire packed architecture bundle when unused | After F parity |

Detailed per-slice acceptance: see `slice-checklist.md` in this folder.

### 5.6 Non-goals (reaffirmed)

- Explorer folder trees / Design template library replacement  
- Managed Nav assembly / publishing widget redesign  
- Desktop Content Explorer applet  
- Shipping UI in this Slice A PR  

---

## 6. Key source pointers (absolute-ish repo paths)

| Area | Path |
|------|------|
| Top nav | `WebUI/src/main/ts/app/layout/TopNav.tsx`, `topNavConfig.ts` |
| Routes / entries | `WebUI/src/main/ts/app/routes.tsx`, `deepLinks/allowlists.ts` |
| Dispatcher | `WebUI/src/main/webapp/cm/app/index.jsp` |
| Legacy page | `WebUI/src/main/webapp/cm/app/siteArchitecture.jsp` |
| Site map widget | `WebUI/src/main/webapp/cm/widgets/perc_site_map.js` |
| Section client | `WebUI/src/main/webapp/cm/services/perc_sectionServiceClient.js` |
| Path constants | `WebUI/src/main/webapp/cm/plugins/perc_path_constants.js` |
| Dialogs | `WebUI/src/main/webapp/cm/plugins/perc_*Section*.js`, `PercSectionTreeDialog.js`, … |
| Pack list | `WebUI/src/main/resources/minify/static-bundles.json` |
| REST | `projects/sitemanage/.../PSSiteSectionRestService.java` |
| Service | `…/PSSiteSectionService.java`, `IPSSiteSectionService.java` |
| DTOs | `…/data/PSSiteSection.java`, `PSCreateSiteSection.java`, `PSMoveSiteSection.java`, … |
| Architecture REST | `…/PSSiteArchitectureDataRestService.java` |
| Managed Nav | `modules/extensions-nav`, `IPSManagedNavService` |
| SPA cutover test | `WebUI/src/test/ts/app/spaCutover.test.ts` (asserts `legacyViews.put("arch", …)`) |

---

## 7. Acceptance (Slice A) — traceability

| # | Acceptance criterion | Where satisfied |
|---|----------------------|-----------------|
| 1 | Written inventory of CM1 Architecture capabilities and dialogs | §2 |
| 2 | API/service matrix (action → endpoint / gap) | §3 |
| 3 | Documented repro of broken top-nav path | §4 |
| 4 | Recommended SPA IA (route name, role gates) for Slice B | §5 |

---

## 8. Follow-on (not in this PR)

- Implement Slice B (#3094) per §5.  
- Do **not** track epic progress in this markdown after open — use GitHub parent #3092 `## Agent progress (night-issue-prs)` only.  

> Co-Authored by Grok Build using grok-4.5 with agent main.
