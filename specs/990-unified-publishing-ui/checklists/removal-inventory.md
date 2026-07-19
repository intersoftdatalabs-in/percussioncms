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
| Remaining `ui/publishing/*.jsp` | Left in place; entry redirects | Acceptable residual | Faces-config may still map deep faces URLs; product path uses modern shell. Full face page deletion optional follow-up packaging PR. |

**Sign-off**: Entry-path retirement 2026-07-19

## Surface B — JSF Publishing Runtime

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `ui/pubruntime/index.jsp` | Redirect → modern Runtime section | Done | |
| `dce_header.jsp` Runtime link | Points to modern Runtime | Done | |
| Remaining `ui/pubruntime/*.jsp` | Entry redirects | Acceptable residual | Same packaging note as Design |

**Sign-off**: Entry-path retirement 2026-07-19

## Shared retained

| Asset | Reason |
|-------|--------|
| `PercItemPublisherService.js` | Finder/editor item actions |
| `PercPublisherService.js` | Shared path helpers / possible residual callers |
| sitemanage `/publish`, `/pubstatus`, `/servers` REST | Engine APIs |
