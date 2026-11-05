# Removal Inventory: Legacy Publishing UIs

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Durable proof for US8 / FR-015 and RET-06 packaging retirement.  
**Status**: Updated 2026-11-05 (#1820 packaging verification + docs: absence enforced, inventory closed).

|              Slice               | Issue |                              Role                               |   Status    |
|----------------------------------|-------|-----------------------------------------------------------------|-------------|
| Parent RET-06                    | #1372 | Packaging: retire residual JSF publishing/pubruntime deep pages | Open        |
| A — Inventory / consumer audit   | #1817 | Faces-config + grep audit + deletion checklists (this update)   | **Done**    |
| B — Design deep-page delete      | #1819 | Delete exclusive `ui/publishing/**` deep JSPs (keep redirects)  | **Done**    |
| C — Runtime deep-page delete     | #1818 | Delete exclusive `ui/pubruntime/**` deep JSPs (keep redirects)  | **Done**    |
| D — Packaging / WAR verification | #1820 | Installer/WAR assertions (absence enforced) + inventory closure + product-docs note | **Done**    |
| UAT gate                         | #1371 | SC-001 / SC-003 / SC-008 sign-off before product deletes        | Open        |

Child D **verification** (#1820): packaging unit tests enforce the retirement (redirect stubs only), inventory tables below are closed, and the operator note lives in `product-docs/8.2/admin/publishing.md`. Deep-page deletes landed via #1819 (Design) and #1818 (Runtime); UAT sign-off remains tracked on #1371.

\*Hard gate: no product deep-page deletes without #1371 UAT sign-off or explicit human ack on #1372. Also rewire non-nav residual consumers listed below before deleting the pages they call.

Detailed audit notes (grep tables, historical faces dump pointers):  
`docs/ai-generated/tasks/1817-publishing-faces-inventory/audit.md`

## Surface C — Minuet / CMS Publish

|                  Path / asset                  |            Action             | Status |            Notes            |
|------------------------------------------------|-------------------------------|--------|-----------------------------|
| `WebUI/.../cm/app/index.jsp` views.put publish | Rewired to modern SPA publish | Done   | `spaViews` includes publish |
| `WebUI/.../cm/pages/app/index.jsp`             | Same                          | Done   | Dual tree                   |
| `WebUI/.../cm/app/publish.jsp`                 | Redirect 301 → modern shell   | Done   | Bookmarks preserved         |
| `WebUI/.../cm/pages/app/publish.jsp`           | Mirror redirect               | Done   |                             |
| `PercPublishMinuetView.js`                     | Deleted                       | Done   | Exclusive Minuet UI         |
| `PercPublishStatusMinuetView.js`               | Deleted                       | Done   |                             |
| `PercPublishLogsMinuetView.js`                 | Deleted                       | Done   |                             |
| `minuetPublishTemplates/*` (app + pages)       | Deleted                       | Done   |                             |
| `vite.legacy.config.ts` perc_publish entry     | Removed                       | Done   | No remaining consumers      |
| `PercPublisherService.js`                      | **Retained**                  | Keep   | Item + shared publish APIs  |
| `PercItemPublisherService.js`                  | **Retained**                  | Keep   | Item publish-now (US6)      |
| `PercPublishingHistoryDialog.js`               | Retained + modern link        | Done   | Link to section=logs        |

**Sign-off**: US8 implementation 2026-07-19 / feature branch `990-unified-publishing-ui`

## Surface A — JSF Publishing Design

|                Path / asset                |              Action              |      Status       |                      Notes                      |
|--------------------------------------------|----------------------------------|-------------------|-------------------------------------------------|
| `ui/publishing/index.jsp`                  | Redirect → modern Design section | Done              | 301 → `/cm/app/?view=publish&section=design`    |
| `dce_header.jsp` Design link               | Points to modern Design          | Done              | No deep faces URL                               |
| Remaining `ui/publishing/*.jsp` deep pages | **Deleted** (#1819) — `index.jsp` redirect retained | Done (RET-06) | Product **nav** callers: **zero** (audit #1817); publish.jsp consumer rewired #1843 |

**Tracked files (2026-08-04)** — `WebUI/src/main/webapp/ui/publishing/` (28 JSPs):

|                  File                   |         Faces historical?         | Product-nav caller |                    Non-nav residual                    |                        #1819 disposition                         |
|-----------------------------------------|-----------------------------------|--------------------|--------------------------------------------------------|------------------------------------------------------------------|
| `index.jsp`                             | entry                             | n/a (redirect)     | —                                                      | **KEEP** redirect                                                |
| `error.jsp`                             | error page                        | none               | self-includes                                          | Delete with Design faces set                                     |
| `menu.jsp`                              | nav chrome                        | none               | faces tree only                                        | Delete                                                           |
| `PubDesignAuthentication.jsp`           | include                           | none               | included by Design pages                               | Delete with Design pages                                         |
| `publish.jsp`                           | demand publish UI (not faces nav) | none               | Seed rewired (#1843) → `../publisher/demandpublishing` | **KEEP** until #1819 (do not delete here); no seed consumer left |
| `AddContextVariable.jsp`                | yes                               | none               | —                                                      | Delete                                                           |
| `AssociateContentlist.jsp`              | yes                               | none               | —                                                      | Delete                                                           |
| `ContentlistEditor.jsp`                 | yes                               | none               | —                                                      | Delete                                                           |
| `ContentlistView.jsp`                   | yes                               | none               | —                                                      | Delete                                                           |
| `ContextEditor.jsp`                     | yes                               | none               | —                                                      | Delete                                                           |
| `ContextList.jsp`                       | yes                               | none               | —                                                      | Delete                                                           |
| `DeliveryTypeEditor.jsp`                | yes                               | none               | —                                                      | Delete                                                           |
| `DeliveryTypeList.jsp`                  | yes                               | none               | —                                                      | Delete                                                           |
| `EditionEditor.jsp`                     | yes                               | none               | —                                                      | Delete                                                           |
| `EditionList.jsp`                       | yes                               | none               | —                                                      | Delete                                                           |
| `ItemBrowser.jsp`                       | yes                               | none               | —                                                      | Delete                                                           |
| `LocationSchemeEditor.jsp`              | yes                               | none               | —                                                      | Delete                                                           |
| `LocationSchemeLegacyEditor.jsp`        | yes                               | none               | —                                                      | Delete                                                           |
| `LocationSchemeParamEditor.jsp`         | yes                               | none               | —                                                      | Delete                                                           |
| `NoSchemeParameterSelectionWarning.jsp` | yes                               | none               | —                                                      | Delete                                                           |
| `NoSelectionWarning.jsp`                | yes                               | none               | —                                                      | Delete                                                           |
| `RemoveConfirmation.jsp`                | yes                               | none               | —                                                      | Delete                                                           |
| `RemoveLocationScheme.jsp`              | yes                               | none               | —                                                      | Delete                                                           |
| `SaveChildSchemeChangesWarning.jsp`     | yes                               | none               | —                                                      | Delete                                                           |
| `SelectEditionFromOtherSite.jsp`        | yes                               | none               | —                                                      | Delete                                                           |
| `SiteEditor.jsp`                        | yes                               | none               | —                                                      | Delete                                                           |
| `SiteList.jsp`                          | yes                               | none               | —                                                      | Delete                                                           |
| `SiteRootBrowser.jsp`                   | yes                               | none               | —                                                      | Delete                                                           |

**Sign-off**: Design deep JSPs deleted in #1819 (RET-06 slice B); `index.jsp` redirect retained; publish.jsp consumer rewired #1843.  
**Tracking issue**: [#1372](https://github.com/intersoftdatalabs-in/percussioncms/issues/1372) · audit [#1817](https://github.com/intersoftdatalabs-in/percussioncms/issues/1817)

## Surface B — JSF Publishing Runtime

|                Path / asset                |              Action               |      Status       |                      Notes                      |
|--------------------------------------------|-----------------------------------|-------------------|-------------------------------------------------|
| `ui/pubruntime/index.jsp`                  | Redirect → modern Runtime section | Done              | 301 → `/cm/app/?view=publish&section=runtime`   |
| `dce_header.jsp` Runtime link              | Points to modern Runtime          | Done              | No deep faces URL                               |
| Remaining `ui/pubruntime/*.jsp` deep pages | **Deleted** (#1818) — `index.jsp` redirect retained | Done (RET-06) | Product **nav** callers: **zero** (audit #1817); consumers rewired #1842/#1844 |
| `PSDemandPublishServlet` post-queue UI     | Rewired off `DemandPublish.jsp`   | Done (#1842)      | Redirect → modern shell status (see note below) |
| `PSRunEdition` `$sys.editionLogUrl`        | Rewired off `JobPubLog.faces`     | Done (#1844)      | Modern shell logs deep link (see note below)    |

**Tracked files (2026-08-04)** — `WebUI/src/main/webapp/ui/pubruntime/` (13 JSPs):

|              File               |      Faces historical?      | Product-nav caller |          Non-nav residual          |       #1818 disposition       |
|---------------------------------|-----------------------------|--------------------|------------------------------------|-------------------------------|
| `index.jsp`                     | entry                       | n/a (redirect)     | —                                  | **KEEP** redirect             |
| `PubRuntimeAuthentication.jsp`  | include                     | none               | included by Runtime pages          | Delete with Runtime faces set |
| `DemandPublish.jsp`             | **no** (plain JSP progress) | none               | **Servlet rewired (#1842)**        | **Delete** (no live consumer) |
| `ActiveJobStatus.jsp`           | yes                         | none               | —                                  | Delete                        |
| `AllPubLogs.jsp`                | yes                         | none               | —                                  | Delete                        |
| `DeleteSiteItemLogsWarning.jsp` | yes                         | none               | —                                  | Delete                        |
| `ErrorMessage.jsp`              | yes                         | none               | —                                  | Delete                        |
| `ItemPubLog.jsp`                | yes                         | none               | —                                  | Delete                        |
| `JobPubLog.jsp`                 | yes                         | none               | **`PSRunEdition` rewired (#1844)** | **Delete** (no live consumer) |
| `NoSelectionWarning.jsp`        | yes                         | none               | —                                  | Delete                        |
| `RuntimeEdition.jsp`            | yes                         | none               | —                                  | Delete                        |
| `RuntimeEditionList.jsp`        | yes                         | none               | —                                  | Delete                        |
| `SitePubLogs.jsp`               | yes                         | none               | —                                  | Delete                        |

**RET-06 residual — DemandPublish servlet rewire (issue #1842, 2026-08-04):**
`system/.../PSDemandPublishServlet` no longer forwards to
`/ui/pubruntime/DemandPublish.jsp` after `queueDemandWork` succeeds.
It now `sendRedirect`s to the modern Publishing shell peer
`publishingShellHref({ section: 'status' })` →
`{contextPath}/cm/app/?view=publish&section=status` (URL path separators only).
Demand `requestid` is **not** appended: modern shell deep-link contract has no
request-id filter. **`web.xml` mapping `/publisher/demandpublishing` is kept.**
**JSP file delete landed** under #1818 — this rewire had removed the live
server-side consumer first so DemandPublish.jsp could be deleted safely.

**RET-06 residual — JobPubLog consumer rewire (issue #1844, 2026-08-04):**
`system/.../PSRunEdition.getPublishingLogURL()` no longer builds
`/ui/pubruntime/JobPubLog.faces?sys_publishingJobId=…` (faces stack already gone).
It now targets the modern Publishing shell peer
`publishingShellHref({ section: 'logs' })` →
`{protocol}://{host}:{port}{requestRoot}/cm/app/?view=publish&section=logs`.
Job-id query is **not** appended: modern shell deep-link contract has no job-id
filter yet (only `section` / optional `siteId` / `serverId`). **JSP file delete
landed** under #1818 — this rewire had removed the live server-side
consumer first so JobPubLog.jsp could be deleted safely.

**Sign-off**: Runtime deep JSPs deleted in #1818 (RET-06 slice C); `index.jsp` redirect retained; non-nav consumers rewired (#1842, #1844).  
**Tracking issue**: [#1372](https://github.com/intersoftdatalabs-in/percussioncms/issues/1372) · audit [#1817](https://github.com/intersoftdatalabs-in/percussioncms/issues/1817)

## Shared retained

|                            Asset                            |                     Reason                      |
|-------------------------------------------------------------|-------------------------------------------------|
| `PercItemPublisherService.js`                               | Finder/editor item actions                      |
| `PercPublisherService.js`                                   | Shared path helpers / possible residual callers |
| sitemanage `/publish`, `/pubstatus`, `/servers` REST        | Engine APIs                                     |
| Modern Publishing shell (`WebUI/src/main/ts/publishing/**`) | Product Design/Runtime/Ops UI                   |

---

## #1817 audit results (2026-08-04)

### Product navigation — zero required deep-page callers

|       Caller surface       |                       Path / symbol                       |                                            Target today                                            | Deep faces? |
|----------------------------|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------|-------------|
| DCE header                 | `WebUI/src/main/webapp/dce_header.jsp`                    | `/cm/app/?view=publish&section=design\|runtime`                                                    | No          |
| Main nav                   | `VIEW_PUBLISH` / `PercNavigationManager`                  | SPA `publish` via `cm/app/index.jsp` `spaViews`                                                    | No          |
| Classic Minuet publish JSP | `cm/app/publish.jsp`, `cm/pages/app/publish.jsp`          | 301 → modern shell                                                                                 | No          |
| Design entry               | `ui/publishing/index.jsp`                                 | 301 → modern Design                                                                                | No          |
| Runtime entry              | `ui/pubruntime/index.jsp`                                 | 301 → modern Runtime                                                                               | No          |
| TS deep-link map           | `WebUI/src/main/ts/publishing/deepLinkMap.ts`             | Maps `/ui/publishing` → `design`, `/ui/pubruntime` → `runtime` (allowlist only; does not open JSF) | No          |
| Contract                   | `specs/990-unified-publishing-ui/contracts/deep-links.md` | Classic paths → modern sections                                                                    | No          |

**Conclusion**: Product navigation does **not** require residual Design/Runtime deep faces pages. Entry redirects + modern shell are the supported path (US8 / #1370).

### Faces-config catalogue

|                 Artifact                  |                                                                                                                                          Location today                                                                                                                                           |                                                                               Notes                                                                               |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Historical source                         | `WebUI/src/main/webapp/WEB-INF/publishing-faces-config.xml`                                                                                                                                                                                                                                       | **Removed** from tree in PR #1337 (`aa46aa5f86`, Home/Widget Builder Track B)                                                                                     |
| `cm` web.xml `jakarta.faces.CONFIG_FILES` | `WebUI/src/main/webapp/cm/WEB-INF/web.xml`                                                                                                                                                                                                                                                        | Entire MyFaces/JSF bootstrap block is **HTML-commented** (includes `publishing-faces-config.xml`, `admin-faces-config.xml`, `config/user/faces/faces-config.xml`) |
| Root WebUI web.xml                        | `WebUI/src/main/webapp/WEB-INF/web.xml`                                                                                                                                                                                                                                                           | Demand publishing servlet still mapped; no active faces CONFIG_FILES                                                                                              |
| Managed beans (historical)                | `sys_design_navigation` → `com.percussion.rx.publisher.jsf.beans.PSDesignNavigation` (session); `sys_runtime_navigation` → `PSRuntimeNavigation`; plus `sys_sitelist`, `sys_design_name_value_provider`, `sys_design_unique_name_validator`, `sys_path_validator`; converter `sys_normalize_path` | **Bean sources not present** on `main` (JSF publisher beans deleted with faces stack)                                                                             |
| Installer upgrade cleanup                 | `modules/perc-distribution-tree/.../install.xml`                                                                                                                                                                                                                                                  | Deletes `…/WEB-INF/publishing-faces-config.xml` (and admin/faces/trinidad peers) on upgrade                                                                       |
| Packaging test peer                       | `ObsoleteWebInfArtifactsCleanupTest`                                                                                                                                                                                                                                                              | Asserts `publishing-faces-config.xml` is listed for cleanup                                                                                                       |

**Implication for #1819 / #1818**: There is **no live faces-config file to prune** on `main`. Child B/C work is JSP (+ optional dead-comment cleanup in web.xml) and non-nav rewires — not a faces-config surgery PR. Installer already cleans residual upgrade copies.

### Historical faces outcomes → JSP map (from last checked-in faces-config)

Recoverable at: `git show aa46aa5f86^:WebUI/src/main/webapp/WEB-INF/publishing-faces-config.xml`

|           Outcome (from-outcome)           |                       to-view-id                       |
|--------------------------------------------|--------------------------------------------------------|
| `pub-design-site-views`                    | `/ui/publishing/SiteList.jsp`                          |
| `pub-design-site-editor`                   | `/ui/publishing/SiteEditor.jsp`                        |
| `pub-design-site-root-browser`             | `/ui/publishing/SiteRootBrowser.jsp`                   |
| `pub-design-item-browser`                  | `/ui/publishing/ItemBrowser.jsp`                       |
| `pub-design-edition-views`                 | `/ui/publishing/EditionList.jsp`                       |
| `pub-design-editions-from-other-sites`     | `/ui/publishing/SelectEditionFromOtherSite.jsp`        |
| `pub-design-edition-editor`                | `/ui/publishing/EditionEditor.jsp`                     |
| `pub-design-content-list-views`            | `/ui/publishing/ContentlistView.jsp`                   |
| `pub-design-content-list-editor`           | `/ui/publishing/ContentlistEditor.jsp`                 |
| `pub-design-context-views`                 | `/ui/publishing/ContextList.jsp`                       |
| `pub-design-context-editor`                | `/ui/publishing/ContextEditor.jsp`                     |
| `pub-design-deliverytypes-views`           | `/ui/publishing/DeliveryTypeList.jsp`                  |
| `pub-design-delivery-type-editor`          | `/ui/publishing/DeliveryTypeEditor.jsp`                |
| `pub-design-location-scheme-editor`        | `/ui/publishing/LocationSchemeEditor.jsp`              |
| `pub-design-location-scheme-legacy-editor` | `/ui/publishing/LocationSchemeLegacyEditor.jsp`        |
| `pub-design-location-scheme-param-editor`  | `/ui/publishing/LocationSchemeParamEditor.jsp`         |
| `no-selection-warning`                     | `/ui/publishing/NoSelectionWarning.jsp`                |
| `no-scheme-parameter-selection-warning`    | `/ui/publishing/NoSchemeParameterSelectionWarning.jsp` |
| `save-child-scheme-changes-warning`        | `/ui/publishing/SaveChildSchemeChangesWarning.jsp`     |
| `add-context-variable`                     | `/ui/publishing/AddContextVariable.jsp`                |
| `associate-content-list`                   | `/ui/publishing/AssociateContentlist.jsp`              |
| `pub-runtime-status-view`                  | `/ui/pubruntime/ActiveJobStatus.jsp`                   |
| `pub-runtime-edition`                      | `/ui/pubruntime/RuntimeEdition.jsp`                    |
| `pub-runtime-editionlist`                  | `/ui/pubruntime/RuntimeEditionList.jsp`                |
| `pub-runtime-all-logs`                     | `/ui/pubruntime/AllPubLogs.jsp`                        |
| `pub-runtime-site-logs`                    | `/ui/pubruntime/SitePubLogs.jsp`                       |
| `pub-runtime-job-log`                      | `/ui/pubruntime/JobPubLog.jsp`                         |
| `pub-runtime-log-item`                     | `/ui/pubruntime/ItemPubLog.jsp`                        |
| `pub-runtime-no-selection-warning`         | `/ui/pubruntime/NoSelectionWarning.jsp`                |
| `pub-runtime-error-message`                | `/ui/pubruntime/ErrorMessage.jsp`                      |
| `pub-runtime-delete-site-item-warn`        | `/ui/pubruntime/DeleteSiteItemLogsWarning.jsp`         |

(Additional per-view rules: delete/edit/save/cancel transitions among Design editors; next/previous on ItemPubLog.)

### Non-nav residual consumers (must rewire before full delete)

These are **not** product-nav, but are **product/engine** callers. Deleting the target pages without rewiring breaks demand publish / scheduled-edition log links / seed menus.

|           Owner           |                                                   Code / data                                                   |                                 Target                                  |                                                                                                               Recommended rewire                                                                                                                |
|---------------------------|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Demand publish servlet    | `system/business/.../PSDemandPublishServlet.java`                                                               | ~~Forward `/ui/pubruntime/DemandPublish.jsp`~~ → redirect modern status | **Done (#1842):** `sendRedirect` → `{contextPath}/cm/app/?view=publish&section=status`                                                                                                                                                          |
| Demand servlet mapping    | `web.xml` url-pattern `/publisher/demandpublishing` (WebUI + cm + war)                                          | Same servlet                                                            | **Kept** (#1842); only post-queue UI target changed                                                                                                                                                                                             |
| Publish Now action seed   | `modules/perc-distribution-tree/.../cmsTableData.xml` ACTIONID 217 `Publish_Now`                                | ~~`../ui/publishing/publish.jsp`~~ → `../publisher/demandpublishing`    | **Done (#1843 + #1884):** URL → `PSDemandPublishServlet`; `action="r"` / `onTableCreateOnly="no"` so upgrades replace existing ACTION 217 rows (peer of FF `EI_Publish_Now`). Params unchanged (`sys_contentid`, `sys_folderid`, `sys_siteid`). |
| Publish Now FF seed       | `.../RxffTableData.xml`, `system/FastForward/Core/Config/Data/RxffTableData.xml` ACTIONID 1012 `EI_Publish_Now` | Same servlet path                                                       | **Done (#1843):** same URL rewire; already `action="r"` / `onTableCreateOnly="no"`; FF params keep `sys_editionid` + content/folder ids.                                                                                                        |
| Scheduled edition log URL | `system/services/.../PSRunEdition.getPublishingLogURL()`                                                        | `/ui/pubruntime/JobPubLog.faces?…`                                      | **Done (#1844):** modern `view=publish&section=logs`                                                                                                                                                                                            |

### Installer / distribution peers (no delete in #1817)

|          Peer          |                                              Path                                               |                          Role for RET-06                          |
|------------------------|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Upgrade cleanup target | `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml` | Already deletes residual `publishing-faces-config.xml` on upgrade |
| Cleanup unit test      | `.../ObsoleteWebInfArtifactsCleanupTest.java`                                                   | Guards the install.xml contract                                   |
| Seed menu URL          | `cmsTableData.xml` / `RxffTableData.xml`                                                        | **Rewired (#1843)** + **upgrade replace (#1884)** for ACTION 217  |
| WAR packaging          | WebUI war packages `ui/publishing/**` and `ui/pubruntime/**` as static webapp files             | #1820 freeze + redirect tests live; absence assertions after B+C  |
| Packaging unit test    | `PublishingDeepPagePackagingTest` (#1820)                                                       | Redirect stubs + inventory freeze + disabled post-delete checks   |

### Docs / bookmark / external refs (preserve or redirect)

|                             Ref class                              |                                                                   Finding                                                                   |
|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Bookmarks to `ui/publishing/index.jsp` / `ui/pubruntime/index.jsp` | Already 301 to modern shell                                                                                                                 |
| Bookmarks to deep `.faces` / deep `.jsp`                           | No product nav; may 404 after delete — acceptable after UAT if modern Design/Runtime cover capability (capability matrix)                   |
| Spec contracts                                                     | `contracts/deep-links.md` already maps classic → modern                                                                                     |
| Historical research docs                                           | `docs/ai-generated/...` still mention `system/ear/WEB-INF/publishing-faces-config.xml` — historical only; source of truth is this checklist |

---

## Deletion checklist — Child B Design (#1819)

**Status: Done** — 27 exclusive Design deep JSPs deleted in PR #5617 (2026-11-05); `index.jsp` redirect retained; Surface A row → Done.

**Prereqs**: #1817 (Done) · #1371 UAT or human ack · **Publish_Now seed rewired (#1843)** — `publish.jsp` may be deleted in #1819 or kept only if another consumer appears.

### Delete (Design-exclusive deep faces / chrome)

- [ ] `AddContextVariable.jsp`
- [ ] `AssociateContentlist.jsp`
- [ ] `ContentlistEditor.jsp`
- [ ] `ContentlistView.jsp`
- [ ] `ContextEditor.jsp`
- [ ] `ContextList.jsp`
- [ ] `DeliveryTypeEditor.jsp`
- [ ] `DeliveryTypeList.jsp`
- [ ] `EditionEditor.jsp`
- [ ] `EditionList.jsp`
- [ ] `ItemBrowser.jsp`
- [ ] `LocationSchemeEditor.jsp`
- [ ] `LocationSchemeLegacyEditor.jsp`
- [ ] `LocationSchemeParamEditor.jsp`
- [ ] `NoSchemeParameterSelectionWarning.jsp`
- [ ] `NoSelectionWarning.jsp`
- [ ] `RemoveConfirmation.jsp`
- [ ] `RemoveLocationScheme.jsp`
- [ ] `SaveChildSchemeChangesWarning.jsp`
- [ ] `SelectEditionFromOtherSite.jsp`
- [ ] `SiteEditor.jsp`
- [ ] `SiteList.jsp`
- [ ] `SiteRootBrowser.jsp`
- [ ] `menu.jsp`
- [ ] `error.jsp` (if unused by retained publish.jsp)
- [ ] `PubDesignAuthentication.jsp` (only after all includes are gone)

### Keep

- [ ] `index.jsp` (modern Design redirect)
- [ ] `publish.jsp` — seed rewired (#1843); optional KEEP for #1819 delete pass (no seed consumer); do not delete in packaging-only PRs

### Faces-config / packaging

- [ ] Confirm no `publishing-faces-config.xml` in source tree (already true)
- [ ] Optional: remove dead commented `jakarta.faces.CONFIG_FILES` lines from `cm/WEB-INF/web.xml` only if Runtime admin faces comment cleanup is coordinated (prefer #1820 or shared comment cleanup)
- [ ] Do **not** remove installer delete of `publishing-faces-config.xml` (upgrade safety)
- [ ] Update this inventory Surface A deep-page row → Done when files deleted

### Smoke after delete

- [ ] Modern Design section loads
- [ ] `ui/publishing/index.jsp` still redirects
- [ ] Item publish-now / Publish_Now still works via `/publisher/demandpublishing` (seed #1843; upgrade replace #1884 rewrites existing ACTION 217 rows on tabledata load)

---

## Deletion checklist — Child C Runtime (#1818)

**Status: Done** — 12 exclusive Runtime deep JSPs deleted in PR #5613 (2026-11-05); `index.jsp` redirect retained; Surface B row → Done.

**Prereqs**: #1817 (Done) · prefer after or with #1819 · #1371 UAT or human ack · **DemandPublish servlet rewire Done (#1842)** · `PSRunEdition` JobPubLog URL rewire (see #1844).

### Delete (Runtime-exclusive deep faces / chrome)

- [ ] `ActiveJobStatus.jsp`
- [ ] `AllPubLogs.jsp`
- [ ] `DeleteSiteItemLogsWarning.jsp`
- [ ] `ErrorMessage.jsp`
- [ ] `ItemPubLog.jsp`
- [ ] `JobPubLog.jsp` (after `PSRunEdition` rewire)
- [ ] `DemandPublish.jsp` (**servlet rewired #1842** — safe to delete with Runtime set)
- [ ] `NoSelectionWarning.jsp`
- [ ] `RuntimeEdition.jsp`
- [ ] `RuntimeEditionList.jsp`
- [ ] `SitePubLogs.jsp`
- [ ] `PubRuntimeAuthentication.jsp` (only after all includes are gone)

### Keep

- [ ] `index.jsp` (modern Runtime redirect)
- [x] `PSDemandPublishServlet` post-queue UI rewired (#1842) — keep `/publisher/demandpublishing` mapping

### Faces-config / packaging

- [ ] No Runtime faces entries remain to purge (source file already gone)
- [ ] Leave installer cleanup of `publishing-faces-config.xml` in place
- [ ] Update this inventory Surface B deep-page row → Done when files deleted

### Smoke after delete

- [ ] Modern Runtime / Status / Logs sections load
- [ ] `ui/pubruntime/index.jsp` still redirects
- [ ] `/publisher/demandpublishing` still queues demand work and redirects to modern Status (#1842)
- [ ] Scheduled edition completion links open modern logs (after rewire)

---

## Packaging verification notes — Child D (#1820)

**Verification status (2026-11-05)** — deep-page deletes landed via #1819 (Design, PR #5617) and #1818 (Runtime, PR #5613). This slice enforces the retirement: absence assertions enabled, inventory closed, operator note in product-docs. UAT sign-off remains tracked on #1371.  
**Prep PR**: [#2095](https://github.com/intersoftdatalabs-in/percussioncms/pull/2095) · issue [#1820](https://github.com/intersoftdatalabs-in/percussioncms/issues/1820)

### Test classes (`modules/perc-distribution-tree`)

| Class | Role |
|-------|------|
| `com.percussion.distribution.install.PublishingDeepPagePackagingTest` | **#1820 primary (prep PR #2095; enforced post-#1819/#1818)**: assert `ui/publishing/index.jsp` + `ui/pubruntime/index.jsp` 301 redirect stubs still target modern shell (`/cm/app/?view=publish&section=design\|runtime`); freeze packaged inventory to the redirect stubs only; absence assertions fail on any re-introduced deep page; confirm `install.xml` still deletes `publishing-faces-config.xml` on upgrade |
| `com.percussion.distribution.install.ObsoleteWebInfArtifactsCleanupTest` | Peer: upgrade cleanup target still covers `publishing-faces-config.xml` (+ JSF lib/TLD families) |
| `com.percussion.distribution.install.PublishNowActionSeedUrlTest` | Peer: Publish_Now / EI_Publish_Now seeds stay on `/publisher/demandpublishing` (not legacy `publish.jsp`) |

### Live contract on `main` today

- Redirect stubs **must remain** and keep modern shell targets (enabled assertions).
- Packaged JSP inventory is **frozen to the redirect stubs only** — any re-introduced deep page fails CI until the freeze or the product change is intentional.
- Exclusive deep-page **absence** assertions are **enabled** (B+C merged) and Surface A/B rows are Done.
- Installer faces-config cleanup expectations **unchanged** (upgrade safety).

### After #1819 + #1818 (landed 2026-11-05)

- [x] Enable `afterDesignRetirementOnlyKeepFilesRemain` / `afterRuntimeRetirementOnlyKeepFilesRemain` in `PublishingDeepPagePackagingTest` (freeze replaced with KEEP-only redirect-stub sets).
- [x] Assert WAR / WebUI package contains only `ui/publishing/index.jsp` and `ui/pubruntime/index.jsp` (optional `publish.jsp` KEEP removed — deleted in #1819).
- [x] Keep `ObsoleteWebInfArtifactsCleanupTest` expectations for faces-config upgrade deletes.
- [x] Mark RET-06 / T124 Done in this file with PR/issue links; parent #1372 acceptance checkoff (UAT #1371 still open — see note below).
- Cross-platform: packaging path assertions use `Path.of` / `Path.resolve` only (no Unix-only separators).

**RET-06 closure (Child D, #1820):** exclusive deep pages are deleted, redirect stubs ship, `publishing-faces-config.xml` ships nowhere (installer still cleans pre-retirement upgrade copies), and the operator note lives in `product-docs/8.2/admin/publishing.md`. The commented-out `jakarta.faces.CONFIG_FILES` block in `cm/WEB-INF/web.xml` is intentionally retained — it also covers `admin-faces-config.xml` and documents the disabled JSF bootstrap; shared cleanup is out of scope. Full product-delete sign-off (UAT #1371 or human ack on #1372) is recorded on the parent, not in this file.

