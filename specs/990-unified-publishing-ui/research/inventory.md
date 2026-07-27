# Inventory: Percussion CMS Publishing Surfaces

**Feature**: `990-unified-publishing-ui`  
**Date**: 2026-07-18  
**Purpose**: Evidence base for the unified Publishing UI specification. Agents implementing the feature MUST treat this as the capability source of truth for feature parity until the capability matrix in the plan supersedes it.

## 1. Three primary UI surfaces today

|           Surface            |         User-facing name          |                            Technology                            |                         Entry / tree                         |                                                           Role                                                           |
|------------------------------|-----------------------------------|------------------------------------------------------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| **A. Publishing Design**     | Rhythmyx Publishing Design        | JSF (MyFaces/Trinidad legacy stack; pages under `ui/publishing`) | `WebUI/.../ui/publishing/` → `SiteList.faces`                | Design publishing infrastructure: sites, editions, content lists, contexts/location schemes, delivery types              |
| **B. Publishing Runtime**    | Rhythmyx Publishing Runtime       | JSF pages under `ui/pubruntime`                                  | `WebUI/.../ui/pubruntime/`                                   | Run editions, demand publish, monitor jobs, view/purge logs, clear site records                                          |
| **C. Modern CMS Publishing** | Publish (Web Management / Minuet) | jQuery + Handlebars Minuet templates                             | `WebUI/.../cm/app` publish views + `minuetPublishTemplates/` | Site/server oriented publish ops for CM1: select site, configure publish servers, full/incremental publish, status, logs |

WebUI module guidelines classify **Rhythmyx Publishing** as ~28 JSF screens, **Legacy / retiring**, with Track B (React) as the strategic replacement.

Help corpora (product documentation of intended capabilities):

- Design: `system/Docs/Rhythmyx_Publishing_Design_Help/` (TOC: About Publishing Design; Maintaining Sites; Maintaining Editions; Content Lists; Contexts and Link Generation Schemes; Browse Site Root Path; Maintaining Delivery Type Registrations)
- Runtime: `system/Docs/Rhythmyx_Publishing_Runtime_Help/` (TOC: About Publishing Runtime; Runtime Dialogs; Running an Edition; Reviewing Publishing Logs; Reviewing Publishing Status; Monitoring Localized Content; Clearing a Site Record)

---

## 2. Surface A — Publishing Design (JSF)

### 2.1 Screens (`WebUI/src/main/webapp/ui/publishing/`)

|                   Page                    |                                     Capability                                      |
|-------------------------------------------|-------------------------------------------------------------------------------------|
| `index.jsp`                               | Redirects to Site List                                                              |
| `SiteList.jsp`                            | List sites; create / edit / copy / delete site                                      |
| `SiteEditor.jsp`                          | Edit site; context variables (add/delete)                                           |
| `EditionList.jsp`                         | List editions for site; create / edit / copy / delete; copy edition from other site |
| `EditionEditor.jsp`                       | Edition properties; associate/edit content lists                                    |
| `SelectEditionFromOtherSite.jsp`          | Pick edition from another site; optional copy of content lists                      |
| `AssociateContentlist.jsp`                | Associate content list with edition                                                 |
| `ContentlistView.jsp`                     | List content lists; create modern / legacy content list; edit / copy / delete       |
| `ContentlistEditor.jsp`                   | Edit content list definition                                                        |
| `ContextList.jsp`                         | List contexts; create / edit / copy / delete                                        |
| `ContextEditor.jsp`                       | Context properties; create modern / legacy location schemes; edit scheme            |
| `LocationSchemeEditor.jsp`                | Modern location scheme editor                                                       |
| `LocationSchemeLegacyEditor.jsp`          | Legacy location scheme + parameters (create/move/remove params)                     |
| `LocationSchemeParamEditor.jsp`           | Single scheme parameter editor                                                      |
| `RemoveLocationScheme.jsp`                | Confirm delete location scheme                                                      |
| `AddContextVariable.jsp`                  | Add context variable                                                                |
| `ItemBrowser.jsp` / `SiteRootBrowser.jsp` | Browse site root path for schemes                                                   |
| `DeliveryTypeList.jsp`                    | List delivery types; create / edit / delete                                         |
| `DeliveryTypeEditor.jsp`                  | Delivery type registration editor                                                   |
| Warnings / auth                           | No selection, remove confirmation, save-child-scheme warning, design authentication |

### 2.2 Design concepts (domain)

Users maintain the **publishing infrastructure**:

1. **Site** — publishing site definition and context variables
2. **Edition** — unit of work that runs one or more content lists (full, incremental, publish-now patterns, etc.)
3. **Content list** — generator of items to publish (query, selected items, incremental/changed, etc.)
4. **Context** — link-generation / location context
5. **Location scheme** (modern JEXL and legacy parameter-based) — URL/path generation rules
6. **Delivery type** — how assembled results are delivered (filesystem, FTP family, database, etc.)

### 2.3 Backend (design / infrastructure services)

- `system/services/.../publisher/` — `IPSPublisherService`, editions, content lists, delivery types, pub status entities
- `system/business/.../rx/publisher/` — runtime job execution (`PSRxPublisherService`, edition runner, job status)
- `system/webservices/.../publishing/` — `IPSPublishingWs` / `PSPublishingWs`
- Design UI is JSF-bound navigation (`sys_design_navigation` in pages); faces config referenced from `cm/WEB-INF/web.xml` (`publishing-faces-config.xml`)

---

## 3. Surface B — Publishing Runtime (JSF)

### 3.1 Screens (`WebUI/src/main/webapp/ui/pubruntime/`)

|              Page               |                               Capability                               |
|---------------------------------|------------------------------------------------------------------------|
| `index.jsp`                     | Runtime entry                                                          |
| `RuntimeEditionList.jsp`        | List editions available to run                                         |
| `RuntimeEdition.jsp`            | Edition runtime view; start/stop edition                               |
| `DemandPublish.jsp`             | Demand / on-demand publish                                             |
| `ActiveJobStatus.jsp`           | Active job status                                                      |
| `JobPubLog.jsp`                 | Job-level publishing log                                               |
| `ItemPubLog.jsp`                | Item-level publish log detail                                          |
| `SitePubLogs.jsp`               | Logs for a site                                                        |
| `AllPubLogs.jsp`                | Cross-site / all logs                                                  |
| `DeleteSiteItemLogsWarning.jsp` | Confirm purge of site item logs / clear site record                    |
| Auth / errors                   | `PubRuntimeAuthentication.jsp`, `ErrorMessage.jsp`, selection warnings |

### 3.2 Runtime capabilities (from help + screens)

- Start / stop / cancel edition runs
- View runtime status of editions and jobs
- Review publishing logs (edition, job, item detail)
- Export log data (documented in help)
- Archive / delete / prune / purge logs
- Demand publish
- Monitor publication of localized content
- Clear a site record (site item log cleanup)

### 3.3 Backend (runtime)

- `PSRxPublisherService` / `IPSRxPublisherService` — start jobs, cancel, status callbacks
- Servlets: `PSJobStatusServlet`, `PSDemandPublishServlet`
- `IPSPublisherJobStatus`, `PSPubItemStatus`, edition task hooks (`IPSEditionTask`)
- WebUI mapping: `/publisher/demandpublishing` in `web.xml`

---

## 4. Surface C — Modern CMS Publishing (Minuet / jQuery)

### 4.1 Client assets

|      Area      |                                                  Paths (source under `WebUI/src/main/webapp/cm/`)                                                  |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Views          | `app/js/legacy/views/PercPublishMinuetView.js`, `PercPublishStatusMinuetView.js`, `PercPublishLogsMinuetView.js`, `PublishView.js`                 |
| Services       | `PercPublisherService.js`, `PercItemPublisherService.js`                                                                                           |
| Templates      | `app/includes/minuetPublishTemplates/` (`publishTemplates`, `publishStatusTemplates`, `publishLogTemplates`, `publishIncrementalPreviewTemplates`) |
| Entry          | `app/publish.jsp` (and pages mirror)                                                                                                               |
| Dialog         | `PercPublishingHistoryDialog.js`                                                                                                                   |
| Path constants | `perc_path_constants.js` (`SITE_PUBLISH`, `PUBLISH_*`, incremental paths, pub server paths)                                                        |

Mirrored trees: `cm/app`, `cm/pages/app`, and packaged `war/` — treat source `app` + `pages` as both requiring rewire on cutover (same pattern as feature 989).

### 4.2 User capabilities (parity checklist for ops UI)

**Sites**

- List sites (card and list views)
- Filter sites by name
- Select site → site detail shell

**Publish servers (per site)**

- List servers for site
- Add / edit / delete server
- Refresh server list
- Mark / recognize default “Publish Now” server
- Server type: **Production** / **Staging**
- Publish target type: **File** / **Database**
- File drivers: **Local**, **FTP**, **FTPS**, **SFTP**, **Amazon S3**
- Database drivers: MSSQL, MySQL, Oracle (common + driver-specific fields)
- Properties: folder path, FTP host/port/user/password/private key, secure FTP, S3 region/credentials, DB connection fields, XML format flag where applicable
- EC2 / region / available publishing server helpers when hosted

**Publish actions**

- Full publish for site + server
- Incremental publish
- Incremental preview queue (items queued + related items)
- Incremental with approval (when workflow requires)
- Stop running job

**Status**

- Live / polled status table: site, status, start time, duration, progress %, stop action
- Sortable columns

**Logs / reports**

- Filter logs (day, server, count, site)
- List logs; open job details (items published)
- Delete/purge selected logs (with confirmation)

**Site publish properties** (legacy FTP-oriented properties API still present on service)

- Get/update site publish properties (FTP fields, delivery root, publish type, site security flag)

### 4.3 Item-level publishing (adjacent, same product domain)

Not only the Publish screen: content workflows call `PercItemPublisherService`:

- Publish page/resource
- Takedown page/resource (with linked-item awareness)
- Stage / remove from staging
- Get available publishing actions for an item
- Schedule publish dates
- Publishing history dialog

Backend: `IPSSitePublishService.PubType` — `FULL`, `FULL_NONBINARY`, `INCREMENTAL`, `STAGING_INCREMENTAL`, `PUBLISH_NOW`, `TAKEDOWN_NOW`, `STAGE_NOW`, `REMOVE_FROM_STAGING_NOW`.

### 4.4 Backend REST used by modern Publish UI

|      Concern       |                      Service                       |                                                                        Path root (under sitemanage services)                                                                         |
|--------------------|----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Site publish       | `PSSitePublishService` (+ web adapter)             | `/publish` (site, page, resource, takedown, incremental, publishingActions)                                                                                                          |
| Status / logs      | `PSSitePublishStatusService` `@Path("/pubstatus")` | `/current`, `/current/{siteId}`, `/logs`, `/purge`, `/details`                                                                                                                       |
| Publish servers    | `PSPubServerRestService` `@Path("/servers")`       | CRUD by site, stopPublishing, availableDrivers, availableRegions, availablePublishingServer, isEC2Instance, defaultFolderLocation, availableDeliveryServers, isDefaultServerModified |
| Public REST (thin) | `rest/.../publishingserver`                        | Publishing server DTO (adapter-oriented)                                                                                                                                             |

These services wrap core `rx.publisher` and `services.publisher` implementations — **reuse, do not reimplement** publishing engine logic in the UI.

---

## 5. Modern React Track B baseline (target stack)

Existing patterns for the unified Publishing UI (same as Dashboard / Home / Widget Builder):

|            Piece             |                        Location                         |
|------------------------------|---------------------------------------------------------|
| React 19 + TypeScript + Vite | `WebUI/src/main/ts/`, `WebUI/src/main/frontend/`        |
| Mount bridge                 | `bridge.ts` → `window.PercModernUI.mount(...)`          |
| Component registry           | `registry.ts`                                           |
| Typed API client / CSRF      | `api/`                                                  |
| i18n                         | TMX + `I18N.message` / TS wrapper (feature 989 pattern) |
| Build output                 | `/cm/modern/` under generated web resources             |

There is **no** production React Publishing module yet (`WebUI/src/main/ts` has `dashboard`, `home`, `widgetbuilder` only).

Legacy packed publish bundle still referenced: `vite.legacy.config.ts` → `perc_publish.packed.min` / `perc_publish.bundle.js`.

---

## 6. Navigation and product entry points (to rewire)

Implementers must inventory exact nav keys at plan/implement time; known anchors:

- Web Management **Publish** view (Minuet publish.jsp / PublishView)
- JSF Publishing Design URL space `/ui/publishing/*`
- JSF Publishing Runtime URL space `/ui/pubruntime/*`
- Demand publishing servlet `/publisher/demandpublishing`
- Desktop Content Explorer may expose publish actions (out of primary web UI scope unless product requires parity)

---

## 7. Capability groups for unified UI (parity map)

Agents should implement and test against these **capability groups** (see also `spec.md` user stories):

|         ID         |                                  Group                                  |  Source surface   |                 Priority bias                 |
|--------------------|-------------------------------------------------------------------------|-------------------|-----------------------------------------------|
| CG-OPS-SITE        | Site pick, filter, card/list                                            | C                 | P1                                            |
| CG-OPS-SERVER      | Publish server CRUD + drivers + production/staging                      | C                 | P1                                            |
| CG-OPS-RUN         | Full / incremental / incremental preview / stop                         | C (+ B)           | P1                                            |
| CG-OPS-STATUS      | Live job status, progress, stop                                         | C + B             | P1                                            |
| CG-OPS-LOGS        | Logs list, filters, details, purge                                      | C + B             | P1                                            |
| CG-ITEM            | Item publish now / takedown / stage / schedule / history                | C (item services) | P2 (keep content-context entry points)        |
| CG-DESIGN-SITE     | Site design create/edit/copy/delete + context variables                 | A                 | P2                                            |
| CG-DESIGN-EDITION  | Editions + content list associations + copy from site                   | A                 | P2                                            |
| CG-DESIGN-CLIST    | Content lists (modern + legacy)                                         | A                 | P2                                            |
| CG-DESIGN-CTX      | Contexts + location schemes (modern + legacy) + site root browser       | A                 | P2                                            |
| CG-DESIGN-DTYPE    | Delivery type registrations                                             | A                 | P2                                            |
| CG-RUNTIME-EDITION | Runtime edition list, start/stop, demand publish                        | B                 | P2                                            |
| CG-RUNTIME-LOGADV  | Advanced log archive/export/prune, clear site record, locale monitoring | B                 | P3 if not already in CG-OPS-LOGS              |
| CG-RETIRE          | Remove JSF design/runtime + Minuet publish clients after parity         | All               | Same release as corresponding modern sections |

---

## 8. Explicit non-goals for inventory (scope of this feature’s *engine*)

- Rewriting the publishing engine (`rx.publisher`, assembly expanders, edition tasks)
- Changing package/install formats or TableFactory schema unless a UI contract gap forces a documented API extension
- Desktop Content Explorer full rewrite
- DTS (delivery tier) microservice internals

---

## 9. Risks and gaps for planning

1. **Two mental models**: Design (editions/content lists) vs CM1 server-centric Publish UI — unified UX must reconcile without stripping power users of design depth.
2. **API completeness for design**: Design is largely JSF-bound; plan phase must confirm which design operations already have REST/ws vs need thin adapters (no invented APIs).
3. **Dual webapp trees** (`cm/app` vs `cm/pages/app` vs `war/`): incomplete cleanup will leave classic UI in some installs (989 lesson).
4. **Secrets in server forms**: passwords/keys must not appear in logs or client persistence beyond existing secure patterns.
5. **Long-running jobs**: status polling/UX must remain robust (stop, progress, multi-site jobs).
6. **Feature flags / license**: modern service already gates some publish paths on license/config — preserve.

---

## 10. Suggested next artifacts (after specify)

|             Artifact             |                            Owner command / phase                             |
|----------------------------------|------------------------------------------------------------------------------|
| `spec.md`                        | `/speckit-specify` (this feature)                                            |
| Capability matrix contract       | `/speckit-plan` or research                                                  |
| REST/API gap analysis for Design | plan research                                                                |
| `plan.md` + tasks                | `/speckit-plan`, `/speckit-tasks`                                            |
| Implementation                   | Track B React under `WebUI/src/main/ts/publishing/` (name finalized in plan) |

