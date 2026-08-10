# #804 — Faculty Directory stale after full site publish (inventory)

**Issue:** [#804](https://github.com/intersoftdatalabs-in/percussioncms/issues/804)  
**Date:** 2026-08-07  
**Scope:** Code-path inventory only — **no product behavior change** in this slice.  
**Disposition:** QA could not reproduce; customer still reports. Overnight cannot ship a blind fix.

## Symptom (customer)

1. Remove a faculty member from the “Faculty Directory” asset / associated staff record.
2. Confirm the member is gone in CMS.
3. Run a **full site publish**.
4. Published Faculty Directory page still shows the member (example name: “Amy Kern”).

**Expected:** After removal + republish, member does not appear on the live directory page.

---

## Executive finding (critical for repro)

The stock **Directory** widget (`perc.widget.directory` / content type `percDirectory`) does **not** hold a list of members as AA (Active Assembly) children of the directory asset.

Membership is computed at **assembly time** by a **JSR-170 query** over all `percPerson` assets whose `personOrganization` / `personDepartment` fields match the directory asset’s org/dept filters.

|          Customer language           |                                                         What product actually does                                                         |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| “Faculty Directory asset”            | `percDirectory` asset: title, placeholder image, **organizationSearch**, **departmentID**                                                  |
| “Faculty member on the directory”    | Separate `percPerson` asset with `personOrganization` / `personDepartment` field values                                                    |
| “Remove member from directory asset” | Not an AA unlink. Typical real ops: recycle/delete person, archive person, clear/change org or dept fields, or change directory org filter |

If the customer only edited the directory asset UI without changing any `percPerson` that still matches the JCR query, full publish will correctly keep rendering that person.

---

## Package / content model

|              Artifact               |                                                         Path                                                         |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| Package                             | `modules/perc-packages/src/main/resources/Packages/perc.widget.directory/`                                           |
| Widget definition (JEXL + Velocity) | `…/sys__UserDependency--rxconfig/Widgets/percDirectory.xml`                                                          |
| Directory CT                        | `percDirectory` → table `CT_PERCDIRECTORY`                                                                           |
| Person CT                           | `percPerson` → table `CT_PERCPERSON`                                                                                 |
| Org / Dept CTs                      | `percOrganization`, `percDepartment`                                                                                 |
| Delivery CSS/JS                     | `…/sys__UserDependency--web_resources/widgets/directory/` (`perc-directory.js`, List.js client filter)               |
| Related list widgets (same finder)  | `perc.widgets.lists` (`simplePageAutoList`, `simpleTextAutoList`), `perc.widget.categoryList`, image auto-list, etc. |

### Directory asset fields (query drivers)

From `percDirectory` assembly JEXL:

- `organizationSearch` → `$orgSearchId`
  - `> 0`: one organization content id
  - `-1`: “all organizations”
  - `0` / unset: no people query
- `departmentID` → `$deptId`
  - `-1`: no department filter (department dropdown shown)
  - `> 0`: restrict to that department

### Person asset fields used by list

- `personOrganization`, `personDepartment`
- Display: `personLastName`, `personFirstName`, phone, email, office, image managed link, person page managed link, etc.

There is **no** relationship config in this package that attaches people as dependents of the directory widget instance for listing purposes. People are only related by **shared field values** (org/dept content ids).

---

## Assembly path (how the published list is built)

```text
Page assemble (publish or preview)
  └─ percDirectory widget Code (jexl) in percDirectory.xml
       ├─ $assets = $rx.pageutils.widgetContents(…, null, null)
       │     → AA/local asset bound to the Directory widget instance (the percDirectory item)
       ├─ Read organizationSearch / departmentID from that asset
       ├─ Build JSR-170 SQL query on rx:percPerson
       │     + "and rx:sys_contentstateid != 7"
       ├─ $finderName = "Java/global/percussion/widgetcontentfinder/perc_AutoWidgetContentFinder"
       └─ $directoryResults = $rx.pageutils.widgetContents(…, finderName, params)
            └─ PSAutoWidgetContentFinder.getContentItems
                 └─ PSAutoFinderUtils.getContentItems  (sys_autoFinderUtils bean)
                      └─ IPSContentMgr.createQuery + executeQuery  (JCR)
                 └─ PSWidgetContentFinder / PSContentFinderBase.filter(items, sourceItem.getFilter(), params)
                      └─ edition/item filter (typically "public" on publish)
  └─ Velocity #foreach ($person in $directoryResults) → HTML table/cards + Schema.org JSON-LD
  └─ Client JS (perc-directory.js + list.min.js) only filters/sorts already-rendered DOM
```

### Canonical person queries (from `percDirectory.xml`)

```text
# single org, all depts
select rx:sys_contentid, rx:sys_folderid from rx:percPerson
  where rx:personOrganization = :orgSearchId
    and rx:sys_contentstateid != 7
  order by rx:personLastName

# single org + department
… and rx:personDepartment = :deptSearchId …

# all orgs
… where rx:personOrganization != '#' and rx:sys_contentstateid != 7 …
```

`max_results` is hard-coded to **5000**.

### Finder implementation

|            Class             |                        Module                        |                                  Role                                  |
|------------------------------|------------------------------------------------------|------------------------------------------------------------------------|
| `PSAutoWidgetContentFinder`  | `projects/sitemanage/.../assembler/impl/finder/`     | Widget-facing auto finder; delegates to utils then applies item filter |
| `PSAutoFinderUtils`          | `system/services/.../assembly/impl/finder/`          | Runs JSR-170 query via content manager                                 |
| `PSPageUtils.widgetContents` | `projects/sitemanage/.../assembler/PSPageUtils.java` | Velocity/JEXL bridge used by all widgets                               |

Extension registration (Baseline package):  
`Java/global/percussion/widgetcontentfinder/perc_AutoWidgetContentFinder` →  
`com.percussion.pagemanagement.assembler.impl.finder.PSAutoWidgetContentFinder`.

---

## Workflow state `!= 7` (Archive hard-code)

Seed data (`RxffTableData.xml` `STATES` table):

|     WORKFLOWAPPID     | STATEID | STATENAME  | CONTENTVALID |
|-----------------------|---------|------------|--------------|
| 5 (default CM1-style) | 7       | Archive    | `u`          |
| 5                     | 5       | Public     | `y`          |
| 5                     | 6       | Quick Edit | `i`          |
| 4 (legacy-style)      | **5**   | Archive    | `u`          |
| 4                     | 3       | Public     | `y`          |

**Implication:** The directory query hard-codes **Archive = 7**. That is correct only for the default stepped workflow (app id 5). On workflow app 4 (or any custom workflow where Archive is not state id 7), the `!= 7` clause does **not** exclude archived people.

Secondary safety net at publish: the **public** item filter uses `sys_filterByPublishableFlag` with `sys_flagValues=y,i` (Public + Quick Edit), which **should** drop Archive (`u`) even if the query returned them. So:

- **Publish assembly** is less sensitive to the hard-coded 7 if the edition uses the standard public filter.
- **Preview / no filter / wrong filter** can still list archived people when Archive ≠ 7.
- Any path that loads public revision of a still-publishable person will still show them.

`PSPublicFilter` only rewrites revision to public-or-current; it does **not** drop unpublished items. Dropping is done by `sys_filterByPublishableFlag`.

---

## Recycle / delete path for a person asset

`PSRecycleService.recycleItem`:

1. Move folder relationship to **RecycledContent**.
2. If the item has a **Quick Edit** transition available (typically Live/public), **Archive** it (`TRANSITION_TRIGGER_ARCHIVE`).
3. Clear **page/template widget AA bindings** for that content id (`clearWidgetRelationshipsForRecycledItem`) — relevant for file/image widgets (#777 / #2238), **not** for auto-query directory lists.

Directory listing does **not** depend on AA owners of the person. Recycle therefore affects the directory only if:

- the person leaves the JCR result set (state / folder / fields), and
- assembly uses a filter that rejects unpublished items, and
- the **directory page** is actually re-assembled and re-delivered.

---

## Publish / incremental vs full site

### Full site

`PSSitePublishDao` creates a **Site Root Full** content list (`FULL_SITE` suffix) with:

- generator: site-root JCR search (`makeJcrSearchQuery(siteRoot)`)
- item filter: public item filter GUID

That edition re-assembles **pages under the site** (and a separate full asset list). When the Faculty Directory **page** is in that list, assembly re-runs the person query against current repository state.

### Incremental / change tracking (why person changes often miss the directory page)

`PSLivePublishChangeHandler`:

|           Event           |                                                       Behavior relevant to directory                                                       |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Page transition/update    | Queues that page                                                                                                                           |
| Asset transition (shared) | Queues **relationship owners** via `widgetAssetRelationshipService.getRelationshipOwners(assetId)` — pages/templates with AA to that asset |
| Asset delete              | Removes resource from queue; same owner walk for pages                                                                                     |
| Local asset update        | Owners only                                                                                                                                |

**There is no reverse index from “percPerson field change” → “pages whose Directory widget queries that person.”**  
Changing or archiving a person does **not** automatically queue the Faculty Directory page for **incremental** publish.

Customer reports **full** site publish, which should bypass that gap **if** the directory page is included and assembly succeeds. Incremental-only workflows would leave stale directory HTML indefinitely — call out in support playbooks.

Related: asset → incremental queue helpers also in `PSWorkflowHelper` / `PSItemService` when approving assets (still owner-based).

### Delivery

Published directory HTML is ordinary assembled output delivered by the site’s pub server type (`PSFileDeliveryHandler`, FTP/SFTP, S3, etc.). There is **no** DTS/metadata service that rebuilds faculty lists independently of CMS assembly. Client `perc-directory.js` only filters the already-published DOM; it cannot resurrect a person removed from HTML, nor hide a person still present in HTML without user interaction.

---

## Candidate stale-content paths (ranked for customer steps)

Customer steps: remove member → full site publish → still visible.

|   ID   |                                                                        Hypothesis                                                                         |        Fits full publish?        |                                         How to confirm on customer snapshot                                         |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------|
| **H1** | **Misunderstood model:** person still has matching `personOrganization` / `personDepartment`; “removed from directory asset” did not change person fields | Yes                              | DB: `CT_PERCPERSON` row for Amy Kern; compare org/dept ids to directory asset `organizationSearch` / `departmentID` |
| **H2** | **Public revision lag:** tip revision unlinked/archived but public revision still Live with old fields; public filter loads public rev                    | Yes                              | Check tip vs public revision, workflow state, CONTENTVALID; CMS preview vs publish filter                           |
| **H3** | **Wrong page / second source:** another page, shared template, second Directory widget, or static copy still has the list                                 | Yes                              | Search published site HTML for name; list all `percDirectory` widgets + page bindings                               |
| **H4** | **Edge/CDN / reverse-proxy / browser cache** serving old HTML after successful publish                                                                    | Yes (CMS correct)                | Compare delivered file on pub root vs HTTP response headers/body; purge CDN                                         |
| **H5** | **Full publish omitted directory page** (job partial failure, wrong site/server, content list filter, item not public)                                    | Partial                          | Pub log: was directory page assembled/delivered? Status of job                                                      |
| **H6** | **Hard-coded state 7** + custom workflow / recycle without Archive + weak filter                                                                          | Possible                         | Person still CONTENTVALID y/i after “removal”; Archive state id ≠ 7                                                 |
| **H7** | **JCR query / index inconsistency** returning ghost content ids                                                                                           | Rare                             | Run same SQL via content manager / diagnostics; compare CONTENTSTATUS                                               |
| **H8** | **Incremental-only** mistaken for full (or full site vs full asset only)                                                                                  | Operational                      | Confirm edition type used by customer                                                                               |
| **H9** | **AA stale relationship** (classic shared-asset binding)                                                                                                  | **Unlikely for stock Directory** | Directory people are not AA dependents of percDirectory; still check customizations                                 |

### What is **not** a primary stock path

- Recycler AA rebind chrome (`perc-recycled-asset`) — applies to widgets **bound** to a recycled content id; Directory list is query-based.
- Delivery-tier search/metadata cache rebuilding a faculty list without CMS publish.
- Client-side List.js cache of members across publishes (JS only filters current page DOM).

---

## Comparison: other auto-list widgets

Same finder class and pattern:

|           Widget package            |                           Query source                            |
|-------------------------------------|-------------------------------------------------------------------|
| `perc.widget.directory` / Directory | Hard-coded JCR over `percPerson` in widget XML                    |
| `perc.widgets.lists` Auto List      | Query string **stored on** the auto-list asset (`query` property) |
| `perc.widget.categoryList`          | JCR over `percPage`                                               |
| Image auto-list / blog index        | Similar auto finder                                               |

Any auto-list has the same **incremental publish blind spot**: content that appears only via query is not tracked as a relationship owner. Full site publish is the operational workaround unless a product fix adds reverse dependency tracking.

---

## Support / diagnostic checklist (no code change)

On a customer DB/snapshot or support session:

1. **Identify the page** that serves Faculty Directory (path + content id).
2. **Load widget asset** for the Directory instance (`percDirectory`): note `organizationSearch`, `departmentID`.
3. **Find “Amy Kern”** as `percPerson`: content id, state id, CONTENTVALID, tip vs public revision, `personOrganization`, `personDepartment`, folder path (Assets vs Recycling).
4. **Recycled?** `RecycledContent` relationship / path under `//Folders/$System$/Recycling`.
5. **Re-run mental query:** would the person match `percDirectory.xml` SQL + public filter?
6. **Pub log** for the full site job: assemble/deliver status for the directory page; timestamp vs “still visible”.
7. **On-disk / delivery target** HTML: does the published file still contain the name?
8. **HTTP** response: same HTML? Cache headers / CDN?
9. **Customizations:** custom Directory widget, non-stock package, third-party cache, mirrored static site.

---

## Product fix candidates (Slice 3 only — **do not implement without repro classification**)

Do **not** ship without evidence which hypothesis holds.

|       Class       |                                                   Possible change                                                   |                      Risk                      |
|-------------------|---------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| Query correctness | Exclude Archive by name/CONTENTVALID not hard-coded state 7; exclude recycled folder; require public flag in query  | Behavior change for all directory sites        |
| Incremental deps  | When `percPerson` (or org/dept) changes, queue pages that contain Directory widgets (catalog scan or reverse index) | Perf; may still need full publish for some ops |
| Assembly          | Drop people in recycler even if still public-flagged                                                                | Edge cases on restore                          |
| Ops               | Document that person field changes need full site publish or page-level publish of directory                        | Docs only                                      |
| Cache             | Document CDN purge after full publish                                                                               | Ops                                            |

---

## Residual work (planned slices)

|      Slice      |                               Goal                                |                                                                               Status                                                                               |
|-----------------|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1 (this PR)** | Inventory publish/cache/stale-relationship surfaces               | Done (docs) — PR #2336                                                                                                                                             |
| **2**           | Live / customer-env repro when snapshot available; classify H1–H9 | Classification: primary **H1**; product full-publish defect not repro without snapshot — #2334 / PR #2356 · [01-classification.md](./01-classification.md)         |
| **3**           | Minimal fix once classified                                       | **Ops runbook shipped** (docs-only per H1) — #2335 · [02-ops-runbook.md](./02-ops-runbook.md). Product query/publish change only if snapshot proves H6/H8 residual |

---

## Key file index (absolute-friendly repo paths)

```
modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percDirectory.xml
modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percPerson.xml
modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/perc-directory.js
modules/perc-packages/src/main/resources/Packages/perc.widgets.lists/sys__UserDependency--rxconfig/Widgets/simplePageAutoList.xml
projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/impl/finder/PSAutoWidgetContentFinder.java
projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java
projects/sitemanage/src/main/java/com/percussion/itemmanagement/PSLivePublishChangeHandler.java
projects/sitemanage/src/main/java/com/percussion/recycle/service/impl/PSRecycleService.java
projects/sitemanage/src/main/java/com/percussion/sitemanage/dao/impl/PSSitePublishDao.java
system/services/src/com/percussion/services/assembly/impl/finder/PSAutoFinderUtils.java
system/services/src/com/percussion/services/filter/impl/PSPublicFilter.java
system/business/src/com/percussion/rx/delivery/impl/PSFileDeliveryHandler.java
modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/RxffTableData.xml  (STATES)
modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml (PSX_ITEM_FILTER)
```

---

## Out of scope for this slice

- Product code or Velocity query changes
- Live customer DB access
- Automated integration tests of publish
- Closing #804 (remains open until fix + verification)

