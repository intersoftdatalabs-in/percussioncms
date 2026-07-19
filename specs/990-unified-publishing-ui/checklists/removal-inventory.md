# Removal Inventory: Legacy Publishing UIs

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Durable proof for US8 / FR-015. Sign off per surface at cutover.  
**Status**: Seed — complete during implementation.

## Surface C — Minuet / CMS Publish (after US1–3 UAT)

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `WebUI/.../cm/app/publish.jsp` | Delete or stop shipping as primary | Pending | After rewire to publishModern.jsp |
| `WebUI/.../cm/pages/app/publish.jsp` | Mirror | Pending | |
| `PercPublishMinuetView.js` (+ pages/war copies) | Delete exclusive | Pending | |
| `PercPublishStatusMinuetView.js` | Delete exclusive | Pending | |
| `PercPublishLogsMinuetView.js` | Delete exclusive | Pending | |
| `PublishView.js` (if publish-only) | Delete or retain if shared | Pending | Inventory consumers |
| `minuetPublishTemplates/*` | Delete exclusive | Pending | |
| `perc_publish.packed.min` / bundle entry | Remove if unused | Pending | `vite.legacy.config.ts` |
| `PercPublisherService.js` | Delete only if no remaining callers | Pending | Item flows may still need paths |

**Sign-off**: _name / date / PR_  

## Surface A — JSF Publishing Design (after US4 UAT)

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `WebUI/.../ui/publishing/**` | Remove from product path | Pending | |
| `publishing-faces-config.xml` entries for design | Remove/adjust | Pending | |
| Design help packaging (optional) | Keep docs or link new help later | Pending | Non-blocking |

**Sign-off**: _name / date / PR_  

## Surface B — JSF Publishing Runtime (after US5 UAT)

| Path / asset | Action | Status | Notes |
|--------------|--------|--------|-------|
| `WebUI/.../ui/pubruntime/**` | Remove from product path | Pending | |
| Demand publish UI JSP | Replaced by Runtime section | Pending | Servlet may remain |

**Sign-off**: _name / date / PR_  

## Shared libraries

| Library | Keep? | Evidence |
|---------|-------|----------|
| Platform jQuery | Likely keep | Other Web Management screens |
| Handlebars publish-only templates | Remove with Minuet publish | |
| JSF stack | Broader product decision | Other admin JSF may remain |

## Deep links verified

| URL | Result | Verified |
|-----|--------|----------|
| `view=publish` | Modern shell | Pending |
| `/ui/publishing/` | Design section or message | Pending |
| `/ui/pubruntime/` | Runtime section or message | Pending |
