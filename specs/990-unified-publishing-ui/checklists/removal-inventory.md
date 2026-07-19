# Removal Inventory: Legacy Publishing UIs

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Durable proof for US8 / FR-015.  
**Status**: Updated 2026-07-19 (US8 implementation).

## Surface C — Minuet / CMS Publish

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `WebUI/.../cm/app/index.jsp` views.put publish | Rewired to `publishModern.jsp` | Done | Dual tree pages/app |
| `WebUI/.../cm/pages/app/index.jsp` | Same | Done | |
| `WebUI/.../cm/app/publish.jsp` | Redirect 301 → modern shell | Done | Bookmarks preserved |
| `WebUI/.../cm/pages/app/publish.jsp` | Mirror redirect | Done | |
| `PercPublishMinuetView.js` | Deleted | Done | Exclusive Minuet UI |
| `PercPublishStatusMinuetView.js` | Deleted | Done | |
| `PercPublishLogsMinuetView.js` | Deleted | Done | |
| `minuetPublishTemplates/*` (app + pages) | Deleted | Done | |
| `vite.legacy.config.ts` perc_publish entry | Removed | Done | No remaining consumers |
| `PercPublisherService.js` | **Retained** | Keep | Item + shared publish APIs |
| `PercItemPublisherService.js` | **Retained** | Keep | Item publish-now (US6) |
| `PercPublishingHistoryDialog.js` | Retained + modern link | Done | Link to section=logs |

**Sign-off**: US8 implementation 2026-07-19 / feature branch `990-unified-publishing-ui`

## Surface A — JSF Publishing Design

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `ui/publishing/index.jsp` | Redirect → modern Design section | Done | Primary entry |
| `dce_header.jsp` Design link | Points to modern Design | Done | |
| Remaining `ui/publishing/*.jsp` (28 files) | **Keep packaged** for now | Deferred (RET-06) | Inventory 2026-07-19: deep faces pages still shipped; product nav uses modern shell only. Delete requires consumer/bookmark audit + faces-config purge in a follow-up packaging PR. |

**Inventory (T124, 2026-07-19)** — `WebUI/src/main/webapp/ui/publishing/`:
`AddContextVariable`, `AssociateContentlist`, `ContentlistEditor/View`, `ContextEditor/List`, `DeliveryTypeEditor/List`, `EditionEditor/List`, `error`, `index` (redirect), `ItemBrowser`, `LocationScheme*`, `menu`, warnings, `PubDesignAuthentication`, `publish`, `Remove*`, `SaveChildSchemeChangesWarning`, `SelectEditionFromOtherSite`, `SiteEditor/List/RootBrowser`.

**Sign-off**: Entry-path retirement Done; deep-page file deletion **explicitly deferred** (owner: packaging follow-up after UAT; do not block #1370).

## Surface B — JSF Publishing Runtime

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `ui/pubruntime/index.jsp` | Redirect → modern Runtime section | Done | |
| `dce_header.jsp` Runtime link | Points to modern Runtime | Done | |
| Remaining `ui/pubruntime/*.jsp` (13 files) | **Keep packaged** for now | Deferred (RET-06) | Same as Design: entry Done; file delete deferred. |

**Inventory (T124, 2026-07-19)** — `WebUI/src/main/webapp/ui/pubruntime/`:
`ActiveJobStatus`, `AllPubLogs`, `DeleteSiteItemLogsWarning`, `DemandPublish`, `ErrorMessage`, `index` (redirect), `ItemPubLog`, `JobPubLog`, `NoSelectionWarning`, `PubRuntimeAuthentication`, `RuntimeEdition/List`, `SitePubLogs`.

**Sign-off**: Entry-path retirement Done; deep-page file deletion **explicitly deferred** (same packaging follow-up as Design).

## Shared retained

| Asset | Reason |
|-------|--------|
| `PercItemPublisherService.js` | Finder/editor item actions |
| `PercPublisherService.js` | Shared path helpers / possible residual callers |
| sitemanage `/publish`, `/pubstatus`, `/servers` REST | Engine APIs |
