# Item publish entry points (US6 / T087)

**Date**: 2026-07-19  
**Feature**: `990-unified-publishing-ui`

## Residual jQuery paths (intentional — remain after shell cutover)

|              Caller              |          Service           |                          Paths                           |
|----------------------------------|----------------------------|----------------------------------------------------------|
| `PercPageView.js`                | `PercItemPublisherService` | publish now, takedown, stage, unstage, schedule, actions |
| `PercFinderView.js`              | same                       | same actions from finder context menu                    |
| `PercScheduleDialog.js`          | schedule get/set           | schedule dates API                                       |
| `PercPublishingHistoryDialog.js` | history table              | item publish history (dialog stays jQuery)               |

## REST (unchanged)

From `perc_path_constants.js` / `itemPublishPaths.ts`:

- `/sitemanage/publish/page/{id}`
- `/sitemanage/publish/resource/{id}`
- `/sitemanage/publish/takedown/page|resource/{id}`
- `/sitemanage/publish/publishingActions/...`

## Deep link glue

History / status links into the site shell should use:

`/cm/app/?view=publish&section=status|logs`

Helper: `publishingShellHref()` in `WebUI/src/main/ts/publishing/itemPublishPaths.ts`.

Legacy dialog may call `window.location` to that href when opening job context (minimal bridge).

## Non-goals for US6

- Full React rewrite of finder/editor publish-now menus
- Changing server publish-now rules

