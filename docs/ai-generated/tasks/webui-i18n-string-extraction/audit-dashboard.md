# dashboard audit

Per-area audit for `WebUI/src/main/ts/dashboard/` under the WebUI i18n string-extraction plan (phase 0). Source sweep: `tmp/webui-i18n-by-area/candidates-dashboard.tsv` (36 raw regex hits across 17 files). Census re-checked against `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` on 2026-08-01: `perc.ui.dashboard.title@` has the 1 legacy entry, and `perc.ui.dashboard.modern@`, `perc.ui.dashboard.welcome@`, `perc.ui.dashboard.activity@` are still 0 in TMX — i.e. the three missing prefixes that own almost all new keys for this area.

## Scope

Files under `WebUI/src/main/ts/dashboard/` with at least one regex hit:

|             File              |   Hits |   Real |      False positive | New keys | TMX reuse |
|-------------------------------|-------:|-------:|--------------------:|---------:|----------:|
| `AssetsStatusWidget.tsx`      |      2 |      2 |                   0 |        2 |         0 |
| `BlogsWidget.tsx`             |      3 |      2 |                   1 |        2 |         0 |
| `BulkUploadWidget.tsx`        |      2 |      2 |                   0 |        2 |         0 |
| `CommentsWidget.tsx`          |      2 |      2 |                   0 |        2 |         0 |
| `CookieConsentWidget.tsx`     |      1 |      1 |                   0 |        1 |         0 |
| `DashboardLayout.tsx`         |      1 |      0 |           1 (JSDoc) |        0 |         0 |
| `EffectivenessWidget.tsx`     |      3 |      3 |                   0 |        3 |         0 |
| `FormsTrackerWidget.tsx`      |      2 |      2 |                   0 |        2 |         0 |
| `IframeWidget.tsx`            |      1 |      0 | 1 (URL placeholder) |        0 |         0 |
| `MembershipWidget.tsx`        |      3 |      3 |                   0 |        2 |         0 |
| `ProcessMonitorWidget.tsx`    |      2 |      2 |                   0 |        2 |         0 |
| `ReportsWidget.tsx`           |      1 |      1 |                   0 |        1 |         0 |
| `SEOAuditWidget.tsx`          |      1 |      1 |                   0 |        1 |         0 |
| `SiteimproveWidget.tsx`       |      5 |      5 |                   0 |        5 |         0 |
| `SitewideFrameworkWidget.tsx` |      1 |      1 |                   0 |        1 |         0 |
| `TrafficWidget.tsx`           |      3 |      3 |                   0 |        2 |         1 |
| `UnavailableGadgetShell.tsx`  |      1 |      1 |                   0 |        1 |         0 |
| `WorkflowStatusWidget.tsx`    |      2 |      2 |                   0 |        2 |         0 |
| **Total**                     | **36** | **33** |               **3** |   **31** |     **1** |

> Note: `MembershipWidget.tsx` has 3 raw hits but two of them (`"No sites available."` in `MembershipWidget.tsx:97` and `CommentsWidget.tsx:95`) collapse to the **same** tuid; that is why "New keys" total is 31 not 32. Details under `## New keys`.

## Reusable keys (already in MSG, just need code-side swap)

None. None of the candidate hardcoded strings in this area matches the English segment of an `MSG.*` constant exactly. The closest constants are:

- `MSG.LOADING = "perc.ui.home.modern@Loading"` (English = `"Loading"`) — every candidate adds a widget-specific subject after the word (`"Loading asset status..."`, `"Loading comments..."`, etc.), so they don't share a key.
- `MSG.DASHBOARD_LOADING = "perc.ui.dashboard.modern@Loading gadgets"` — applies to the shell-level Dashboard view, not to per-widget loading messages.
- `MSG.GADGET_DESC_TRAFFIC = "perc.ui.dashboard.modern@Content traffic series"` — description, not the chart legend label `"Visits"` from `TrafficWidget.tsx:178`.
- `MSG.ACTIVITY_LOADING = "perc.ui.dashboard.activity@Loading activity"` — exists in MSG (catalogue already wired) but no current implementation site uses it as a literal; the only candidate that would semantically match it (`"Loading activity"`) was **not** in this sweep (Activity widget chrome was out of the regex pattern). Recorded here only so a Phase 1 swap on `ActivityWidget.tsx` doesn't re-add a duplicate constant.

Conclusion: every per-widget literal below currently bypasses `MSG`; Phase 1 will swap implementations to MSG constants as part of the new-key rollout. No reuse-without-new-key path.

## Reusable keys (already in TMX, but no MSG constant yet)

One. There is exactly one candidate whose English text already exists in `CmsUi.tmx` but for which `message.ts` does not yet expose an `MSG.*` constant:

- **TrafficWidget.tsx:178** — `"Visits"` (chart-legend label inside `<div style={{ fontSize: "0.75em", color: "#666" }}>Visits</div>`).
  - Existing TU: `<tu tuid="perc.ui.traffic.gadget@Visits">` at `CmsUi.tmx:75856` (verified with `Select-String -LiteralPath .../CmsUi.tmx -Pattern 'tuid="perc\.ui\.traffic\.gadget@Visits"'`; the `<seg>Visits</seg>` English row is on line 75857).
  - Other `"Visits"`/`"None"`/`"Present"` matches inside CmsUi.tmx are unrelated (Spanish-fragment text, `perc.ui.widget.tinymce@None`, `perc.ui.control.imageSlider@None`) — they do not cover this gadget.
  - **Proposed MSG constant** (for ergonomic reuse, keeping the existing legacy gadget-prefix key untouched):

    ```ts
    TRAFFIC_LEGEND_VISITS: "perc.ui.traffic.gadget@Visits",
    ```
  - Phase-1 follow-up: add this entry to `MSG` in `WebUI/src/main/ts/i18n/message.ts` so `TrafficWidget.tsx:178` reads `{message(MSG.TRAFFIC_LEGEND_VISITS)}` instead of a literal.

No other candidates have a `perc.ui.<prefix>@<English>` TU that exactly matches their English text under the searched prefixes (`perc.ui.dashboard.*`, `perc.ui.gadgets.*`, `perc.ui.home.modern@*`, `perc.ui.navMenu.*`). For example: `"No sites available."`, `"Not available in React Home"`, `"Token on server:"`, `"Present"`, `"None"`, `"Not configured"` were all searched and returned zero hits.

## New keys (need new TMX entry + MSG constant)

All new tuids go under the three missing prefixes (per the plan census): `perc.ui.dashboard.modern@…` is the default for shell and most gadget chrome; `perc.ui.dashboard.welcome@…` for Welcome-widget strings (already declared in `MSG`); `perc.ui.dashboard.activity@…` for Activity-widget strings (already declared in `MSG`). Every row was verified absent in CmsUi.tmx before recommendation.

Grouped by file:

### `AssetsStatusWidget.tsx`

|          file:line           |                    english                    |                             proposed tuid                              |  proposed MSG constant  |                                   notes                                    |
|------------------------------|-----------------------------------------------|------------------------------------------------------------------------|-------------------------|----------------------------------------------------------------------------|
| `AssetsStatusWidget.tsx:88`  | `Loading asset status...`                     | `perc.ui.dashboard.modern@Loading asset status`                        | `WIDGET_ASSETS_LOADING` | Drop trailing `...` in TU segment; renderer can re-add ellipsis if needed. |
| `AssetsStatusWidget.tsx:102` | `No assets found for this path and workflow.` | `perc.ui.dashboard.modern@No assets found for this path and workflow.` | `WIDGET_ASSETS_EMPTY`   | Mirror copy of `WIDGET_WORKFLOW_EMPTY` and `WIDGET_TRAFFIC_EMPTY`.         |

### `BlogsWidget.tsx`

The two-line JSX paragraph at `:415` and `:416` is one logical message (split by JSX whitespace) and should be one key:

|         file:line         |                                                                                              english                                                                                               |                                                                                                    proposed tuid                                                                                                    |    proposed MSG constant    |                                  notes                                   |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|--------------------------------------------------------------------------|
| `BlogsWidget.tsx:415–417` | `A blog needs two existing templates: one with a **Blog List** widget and one with a **Blog Post** widget. Create those in Design / Templates first (or copy base blog templates onto this site).` | `perc.ui.dashboard.modern@A blog needs two existing templates: one with a Blog List widget and one with a Blog Post widget. Create those in Design / Templates first (or copy base blog templates onto this site).` | `WIDGET_BLOGS_NO_TEMPLATES` | Bold markers belong to JSX, not the catalog. Encode the plain text only. |

### `BulkUploadWidget.tsx`

|         file:line         |             english              |                      proposed tuid                      |      proposed MSG constant      |                                                             notes                                                              |
|---------------------------|----------------------------------|---------------------------------------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `BulkUploadWidget.tsx:88` | `File` (label, `value="file"`)   | `perc.ui.dashboard.modern@Bulk upload asset type File`  | `WIDGET_BULK_UPLOAD_TYPE_FILE`  | The `value="file"` is an enum and stays English; only the visible label moves to TMX.                                          |
| `BulkUploadWidget.tsx:89` | `Image` (label, `value="image"`) | `perc.ui.dashboard.modern@Bulk upload asset type Image` | `WIDGET_BULK_UPLOAD_TYPE_IMAGE` | Same rationale; consider also a shared `perc.ui.common.label@Image` if `Image` appears in other contexts, otherwise keep here. |

### `CommentsWidget.tsx`

|        file:line        |        english        |                 proposed tuid                  |   proposed MSG constant    |                               notes                               |
|-------------------------|-----------------------|------------------------------------------------|----------------------------|-------------------------------------------------------------------|
| `CommentsWidget.tsx:81` | `Loading comments...` | `perc.ui.dashboard.modern@Loading comments`    | `WIDGET_COMMENTS_LOADING`  |                                                                   |
| `CommentsWidget.tsx:95` | `No sites available.` | `perc.ui.dashboard.modern@No sites available.` | `WIDGET_COMMENTS_NO_SITES` | **Same key shared with `MembershipWidget.tsx:97`** — keep one TU. |

### `CookieConsentWidget.tsx`

|          file:line           |           english           |                   proposed tuid                   |      proposed MSG constant      | notes |
|------------------------------|-----------------------------|---------------------------------------------------|---------------------------------|-------|
| `CookieConsentWidget.tsx:76` | `Loading cookie consent...` | `perc.ui.dashboard.modern@Loading cookie consent` | `WIDGET_COOKIE_CONSENT_LOADING` |       |

### `EffectivenessWidget.tsx`

|             file:line             |                                                                              english                                                                               |                                                                    proposed tuid                                                                     |           proposed MSG constant           |                                                 notes                                                 |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `EffectivenessWidget.tsx:100`     | `Loading effectiveness...`                                                                                                                                         | `perc.ui.dashboard.modern@Loading effectiveness`                                                                                                     | `WIDGET_EFFECTIVENESS_LOADING`            |                                                                                                       |
| `EffectivenessWidget.tsx:111–117` | `Google Analytics is not configured` + `What's Working needs a Google Analytics provider and site profile. Use the Google Setup gadget, then refresh this widget.` | (two TUs) — see below                                                                                                                                | (two constants)                           | Split into title + body.                                                                              |
| `EffectivenessWidget.tsx:111`     | `Google Analytics is not configured`                                                                                                                               | `perc.ui.dashboard.modern@Google Analytics is not configured`                                                                                        | `WIDGET_EFFECTIVENESS_NO_ANALYTICS_TITLE` | Bold title.                                                                                           |
| `EffectivenessWidget.tsx:115–117` | `What's Working needs a Google Analytics provider and site profile. Use the Google Setup gadget, then refresh this widget.`                                        | `perc.ui.dashboard.modern@What's Working needs a Google Analytics provider and site profile. Use the Google Setup gadget, then refresh this widget.` | `WIDGET_EFFECTIVENESS_NO_ANALYTICS_BODY`  | Apostrophe in `What's` → use `&apos;` entity in XML or escape in TU; verify TMX/JS string round-trip. |
| `EffectivenessWidget.tsx:140`     | `No effectiveness data for this path and duration.`                                                                                                                | `perc.ui.dashboard.modern@No effectiveness data for this path and duration.`                                                                         | `WIDGET_EFFECTIVENESS_EMPTY`              |                                                                                                       |

### `FormsTrackerWidget.tsx`

|          file:line           |               english               |                        proposed tuid                         |  proposed MSG constant  |                                notes                                |
|------------------------------|-------------------------------------|--------------------------------------------------------------|-------------------------|---------------------------------------------------------------------|
| `FormsTrackerWidget.tsx:88`  | `Loading forms...`                  | `perc.ui.dashboard.modern@Loading forms`                     | `WIDGET_FORMS_LOADING`  |                                                                     |
| `FormsTrackerWidget.tsx:102` | `No sites available to load forms.` | `perc.ui.dashboard.modern@No sites available to load forms.` | `WIDGET_FORMS_NO_SITES` | Distinct from generic `No sites available.` — site-context differs. |

### `MembershipWidget.tsx`

|         file:line          |           english           |                    proposed tuid                     |    proposed MSG constant     |                                      notes                                      |
|----------------------------|-----------------------------|------------------------------------------------------|------------------------------|---------------------------------------------------------------------------------|
| `MembershipWidget.tsx:79`  | `Loading membership...`     | `perc.ui.dashboard.modern@Loading membership`        | `WIDGET_MEMBERSHIP_LOADING`  |                                                                                 |
| `MembershipWidget.tsx:97`  | `No sites available.`       | `perc.ui.dashboard.modern@No sites available.`       | `WIDGET_MEMBERSHIP_NO_SITES` | **Same TU as `WIDGET_COMMENTS_NO_SITES`** — one TU, two constants are optional. |
| `MembershipWidget.tsx:110` | `No members for this site.` | `perc.ui.dashboard.modern@No members for this site.` | `WIDGET_MEMBERSHIP_EMPTY`    | `data-testid="membership-empty"` is a selector, not chrome.                     |

### `ProcessMonitorWidget.tsx`

|           file:line            |           english            |                   proposed tuid                    |      proposed MSG constant       | notes |
|--------------------------------|------------------------------|----------------------------------------------------|----------------------------------|-------|
| `ProcessMonitorWidget.tsx:89`  | `Loading process monitor...` | `perc.ui.dashboard.modern@Loading process monitor` | `WIDGET_PROCESS_MONITOR_LOADING` |       |
| `ProcessMonitorWidget.tsx:103` | `No monitors available`      | `perc.ui.dashboard.modern@No monitors available`   | `WIDGET_PROCESS_MONITOR_EMPTY`   |       |

### `ReportsWidget.tsx`

|       file:line        |      english      |               proposed tuid                | proposed MSG constant  | notes |
|------------------------|-------------------|--------------------------------------------|------------------------|-------|
| `ReportsWidget.tsx:72` | `No report data.` | `perc.ui.dashboard.modern@No report data.` | `WIDGET_REPORTS_EMPTY` |       |

### `SEOAuditWidget.tsx`

|        file:line        |        english         |                proposed tuid                 | proposed MSG constant | notes |
|-------------------------|------------------------|----------------------------------------------|-----------------------|-------|
| `SEOAuditWidget.tsx:71` | `Loading SEO audit...` | `perc.ui.dashboard.modern@Loading SEO audit` | `WIDGET_SEO_LOADING`  |       |

### `SiteimproveWidget.tsx`

|          file:line          |         english          |                 proposed tuid                  |        proposed MSG constant        |                                                                     notes                                                                      |
|-----------------------------|--------------------------|------------------------------------------------|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `SiteimproveWidget.tsx:93`  | `Loading Siteimprove...` | `perc.ui.dashboard.modern@Loading Siteimprove` | `WIDGET_SITEIMPROVE_LOADING`        |                                                                                                                                                |
| `SiteimproveWidget.tsx:120` | `Token on server:`       | `perc.ui.dashboard.modern@Token on server`     | `WIDGET_SITEIMPROVE_TOKEN_LABEL`    | Trailing colon is JSX punctuation; encode without.                                                                                             |
| `SiteimproveWidget.tsx:127` | `Not configured`         | `perc.ui.dashboard.modern@Not configured`      | `WIDGET_SITEIMPROVE_NOT_CONFIGURED` |                                                                                                                                                |
| `SiteimproveWidget.tsx:133` | `Present`                | `perc.ui.dashboard.modern@Present`             | `WIDGET_SITEIMPROVE_PRESENT`        | Watch out for collisions with future non-dashboard strings — keep under `dashboard.modern` for now.                                            |
| `SiteimproveWidget.tsx:135` | `None`                   | `perc.ui.dashboard.modern@None`                | `WIDGET_SITEIMPROVE_NONE`           | Same caveat as `Present`; the two existing `None` TUs in TMX are widget-specific (tinymce, image slider) so a dashboard-local copy is correct. |

### `SitewideFrameworkWidget.tsx`

|            file:line             |      english       |                proposed tuid                |       proposed MSG constant       | notes |
|----------------------------------|--------------------|---------------------------------------------|-----------------------------------|-------|
| `SitewideFrameworkWidget.tsx:77` | `No themes found.` | `perc.ui.dashboard.modern@No themes found.` | `WIDGET_SITEWIDE_FRAMEWORK_EMPTY` |       |

### `TrafficWidget.tsx`

|        file:line        |                     english                     |                              proposed tuid                               |  proposed MSG constant   |                               notes                               |
|-------------------------|-------------------------------------------------|--------------------------------------------------------------------------|--------------------------|-------------------------------------------------------------------|
| `TrafficWidget.tsx:101` | `Loading traffic data...`                       | `perc.ui.dashboard.modern@Loading traffic data`                          | `WIDGET_TRAFFIC_LOADING` |                                                                   |
| `TrafficWidget.tsx:121` | `No traffic data for this path and date range.` | `perc.ui.dashboard.modern@No traffic data for this path and date range.` | `WIDGET_TRAFFIC_EMPTY`   |                                                                   |
| `TrafficWidget.tsx:178` | `Visits`                                        | **reuse** `perc.ui.traffic.gadget@Visits` (already in TMX)               | `TRAFFIC_LEGEND_VISITS`  | See `## Reusable keys (already in TMX, but no MSG constant yet)`. |

### `UnavailableGadgetShell.tsx`

|            file:line            |            english            |                     proposed tuid                      |    proposed MSG constant    |                                                    notes                                                    |
|---------------------------------|-------------------------------|--------------------------------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------|
| `UnavailableGadgetShell.tsx:40` | `Not available in React Home` | `perc.ui.dashboard.modern@Not available in React Home` | `UNAVAILABLE_GADGET_HEADER` | The component's `reason` prop is already data-driven and out of scope; only this title literal needs a key. |

### `WorkflowStatusWidget.tsx`

|           file:line            |                   english                    |                             proposed tuid                             |   proposed MSG constant   | notes |
|--------------------------------|----------------------------------------------|-----------------------------------------------------------------------|---------------------------|-------|
| `WorkflowStatusWidget.tsx:87`  | `Loading workflow status...`                 | `perc.ui.dashboard.modern@Loading workflow status`                    | `WIDGET_WORKFLOW_LOADING` |       |
| `WorkflowStatusWidget.tsx:103` | `No pages found for this path and workflow.` | `perc.ui.dashboard.modern@No pages found for this path and workflow.` | `WIDGET_WORKFLOW_EMPTY`   |       |

### Aggregate

After Phase 1 this audit will add **31 net-new tuids** under `perc.ui.dashboard.modern@…`, plus the corresponding MSG constants (each TU gets exactly one constant unless two widgets intentionally share a message — only `"No sites available."` does). The `perc.ui.dashboard.welcome@…` and `perc.ui.dashboard.activity@…` prefixes remain 0 in this audit because Welcome/Activity widgets were not in the regex sweep; those will get entries during their own per-area audits.

## False positives

|        file:line         |                                         snippet                                         |                                                                                                           reason                                                                                                            |
|--------------------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DashboardLayout.tsx:24` | `<p>Note: Drag-and-drop can be added later via react-grid-layout or similar.</p>`       | JSDoc comment paragraph (continuation of the component-level doc block on lines 18–25). Not rendered to the DOM.                                                                                                            |
| `BlogsWidget.tsx:48`     | `* the <strong>blog section itself</strong> (classic Blogs gadget responsibility).</p>` | JSDoc comment block describing what the widget does (`/**` opens at line 44). The `<p>` / `<strong>` are inside the comment, not JSX.                                                                                       |
| `IframeWidget.tsx:106`   | `placeholder="https://…"`                                                               | Example URL placeholder showing a valid `https://…` value; not English chrome and not a localization target per Phase 0 rules (attribute-localizable strings must start with an uppercase English word or contain a space). |

