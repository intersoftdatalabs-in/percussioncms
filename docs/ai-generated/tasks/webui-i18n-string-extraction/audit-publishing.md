# publishing audit

Source sweep: `tmp/webui-i18n-by-area/candidates-publishing.tsv` (68 raw regex hits,
2026-08-01). Candidate area under audit:
`WebUI/src/main/ts/publishing/**` (39 .tsx/.ts files, 68 hits — the largest area).

Goal: identify every hardcoded English string that must route through
`message(MSG.PUBLISH.*)` and pick the correct tuid, reusing
`perc.ui.publish.title@*` / `perc.ui.publish.modern@*` / `perc.ui.publish.view@*`
where they already exist, and proposing new `perc.ui.publish.<screen>@*` tuids
otherwise.

This is documentation only. No code or TMX edits happen here.

---

## Scope

Per-file hit counts grouped by sub-area.

|       Sub-area        |                   File                    |   Hits |
|-----------------------|-------------------------------------------|-------:|
| `components/`         | `components/LogDetailsPanel.tsx`          |     13 |
| `components/`         | `components/ServerEditor.tsx`             |      4 |
| `components/drivers/` | `components/drivers/FileDriverFields.tsx` |      1 |
| `design/`             | `design/ContentListEditor.tsx`            |      5 |
| `design/`             | `design/ContextsPanel.tsx`                |     12 |
| `design/`             | `design/DeliveryTypesPanel.tsx`           |      2 |
| `design/`             | `design/EditionEditor.tsx`                |      8 |
| `design/`             | `design/SiteDesignPanel.tsx`              |      5 |
| `design/`             | `design/SiteRootBrowser.tsx`              |      1 |
| `sections/`           | `sections/DesignSection.tsx`              |      3 |
| `sections/`           | `sections/LogsSection.tsx`                |      5 |
| `sections/`           | `sections/RuntimeSection.tsx`             |      7 |
| `sections/`           | `sections/SiteWorkspace.tsx`              |      2 |
| **Total**             |                                           | **68** |

Of those 68 hits:

- **53** are real candidates needing a new or existing key (see §III / §IV / §V).
- **8** are option-label / option-value pairs where the **value** is a machine
  identifier (`PRODUCTION`, `STAGING`, `File`, `Database`, `String`,
  `BackendColumn`, empty string). Only the label is localizable; the value
  stays. See §VI.
- **7** are short placeholder / aria-label strings (`name`, `value`, `Item`,
  `select all related items`, `e.g. 101, 102`) that need new keys but live in
  very short forms — folded into §V with notes.

---

## Reusable keys (MSG already exposes a matching constant)

`WebUI/src/main/ts/i18n/message.ts` already publishes these `PUBLISH_*`
constants whose `@Human Text` matches a candidate exactly:

|            Candidate (file:line, English)             |         MSG constant         |                  Tuid                  |
|-------------------------------------------------------|------------------------------|----------------------------------------|
| `LogDetailsPanel.tsx:130`, `Status` (table header)    | `MSG.PUBLISH_SECTION_STATUS` | `perc.ui.publish.title@Status`         |
| `LogDetailsPanel.tsx:206`, `Back` (close button)      | `MSG.PUBLISH_BACK`           | `perc.ui.publish.modern@Back`          |
| `LogDetailsPanel.tsx:113`, `No Logs` (empty state)    | `MSG.PUBLISH_EMPTY_LOGS`     | `perc.ui.publish.modern@No Logs`       |
| `ServerEditor.tsx:178`, `Server Type` (field label)   | `MSG.PUBLISH_SERVER_TYPE`¹   | `perc.ui.publish.view@Production`      |
| `ServerEditor.tsx:190`, `Delivery Type` (field label) | `MSG.PUBLISH_DELIVERY_TYPE`  | `perc.ui.publish.modern@Delivery Type` |
| `ServerEditor.tsx:202`, `Driver` (field label)        | `MSG.PUBLISH_DRIVER`         | `perc.ui.publish.modern@Driver`        |

¹ `MSG.PUBLISH_SERVER_TYPE` currently points at `perc.ui.publish.view@Production`
which is wrong as a *Server Type* label (the field label should read "Server
Type", not "Production"). The existing `perc.ui.publish.title@` prefix has no
"Server Type" tuid. Treat this as a **catalog bug to fix in Phase 1** — propose
`MSG.PUBLISH_SERVER_TYPE` → `perc.ui.publish.title@Server Type` (new tuid, see
§V) and remove the broken `perc.ui.publish.view@Production` reference. The
option **label** `Production` itself still needs its own key (see §V).

`ServerEditor.tsx` lines 184/185/196/197 (option text "Production" / "Staging"
/ "File" / "Database") and `ContextsPanel.tsx` lines 344/345 (option text
"String" / "BackendColumn") are **not** matched by any existing `MSG.PUBLISH_*`
constant. They go to §V.

---

## Reusable keys (TMX has the tuid; needs MSG constant)

`CmsUi.tmx` (`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`) already has
the matching `perc.ui.publish.*@*` tuid, but no `MSG.PUBLISH_*` constant
exists yet. Phase 1 adds the constant (and Phase 3 wires the JSX).

|         file:line         |         English         |            Tuid (exists in TMX)             |                                Proposed MSG constant                                 |     Sub-area      |
|---------------------------|-------------------------|---------------------------------------------|--------------------------------------------------------------------------------------|-------------------|
| `LogDetailsPanel.tsx:77`  | `Job ID`                | `perc.ui.publish.title@Job ID`              | `MSG.PUBLISH.LOGS_DETAILS.JOB_ID`                                                    | `logs.details`    |
| `LogDetailsPanel.tsx:117` | `Filter items`          | `perc.ui.publish.title@Filter Items`        | `MSG.PUBLISH.LOGS_DETAILS.FILTER_ITEMS`                                              | `logs.details`    |
| `LogDetailsPanel.tsx:131` | `Operation`             | `perc.ui.publish.title@Operation`           | `MSG.PUBLISH.LOGS_DETAILS.OPERATION`                                                 | `logs.details`    |
| `LogDetailsPanel.tsx:132` | `Location`              | `perc.ui.publish.title@Location`            | `MSG.PUBLISH.LOGS_DETAILS.LOCATION`                                                  | `logs.details`    |
| `LogDetailsPanel.tsx:181` | `Content ID:`           | `perc.ui.publish.title@Content ID`          | `MSG.PUBLISH.LOGS_DETAILS.CONTENT_ID`                                                | `logs.details`    |
| `LogDetailsPanel.tsx:190` | `Filename:`             | `perc.ui.publish.title@Filename`            | `MSG.PUBLISH.LOGS_DETAILS.FILENAME`                                                  | `logs.details`    |
| `LogDetailsPanel.tsx:193` | `Location:`             | `perc.ui.publish.title@Location`            | (reuse `MSG.PUBLISH.LOGS_DETAILS.LOCATION`)                                          | `logs.details`    |
| `LogDetailsPanel.tsx:196` | `Operation:`            | `perc.ui.publish.title@Operation`           | (reuse `MSG.PUBLISH.LOGS_DETAILS.OPERATION`)                                         | `logs.details`    |
| `LogDetailsPanel.tsx:199` | `Status:`               | `perc.ui.publish.title@Status`              | `MSG.PUBLISH.LOGS_DETAILS.STATUS` (or reuse `PUBLISH_SECTION_STATUS` if intentional) | `logs.details`    |
| `LogsSection.tsx:200`     | `Show` (label)          | `perc.ui.publish.title@Show`                | `MSG.PUBLISH.SECTIONS.LOGS.SHOW`                                                     | `sections.logs`   |
| `LogsSection.tsx:180`     | `All` (placeholder)     | `perc.ui.publish.title@All`                 | `MSG.PUBLISH.SECTIONS.LOGS.FILTER_ALL`                                               | `sections.logs`   |
| `ContextsPanel.tsx:344`   | `String` (option label) | `perc.ui.publish.title@Type` (reuse `Type`) | `MSG.PUBLISH.DESIGN.CONTEXTS.PARAM_TYPE_STRING`                                      | `design.contexts` |

The `Location` / `Operation` / `Status` text appears in two roles: the table
header (lines 131/132/130) and the detail-block label (lines 193/196/199).
They share the same human text, so they share the same tuid — only the MSG
constant names need to differ if we want explicit semantics.

---

## New keys (need new TMX entry + MSG constant)

Both the tuid and the `MSG.PUBLISH.*` constant are net-new. Phase 1 registers
the constant + the new `<tu>`; Phase 3 wires the JSX.

Sub-prefix policy used below:

|          Sub-area          |                Sub-prefix                |
|----------------------------|------------------------------------------|
| Logs detail panel          | `perc.ui.publish.logs.details@…`         |
| Logs section filters       | `perc.ui.publish.sections.logs@…`        |
| Runtime section            | `perc.ui.publish.sections.runtime@…`     |
| Design section             | `perc.ui.publish.sections.design@…`      |
| Site workspace             | `perc.ui.publish.sections.site@…`        |
| Server editor field labels | `perc.ui.publish.server.editor@…`        |
| File driver fields         | `perc.ui.publish.drivers.file@…`         |
| Design — content lists     | `perc.ui.publish.design.contentLists@…`  |
| Design — contexts          | `perc.ui.publish.design.contexts@…`      |
| Design — delivery types    | `perc.ui.publish.design.deliveryTypes@…` |
| Design — editions          | `perc.ui.publish.design.editions@…`      |
| Design — site design       | `perc.ui.publish.design.site@…`          |
| Design — site root browser | `perc.ui.publish.design.siteRoot@…`      |

### components/ (server editor + log details + drivers)

|             file:line              |           English           |                Proposed tuid                |               Proposed MSG constant                |    Sub-area     |                                                     Notes                                                      |
|------------------------------------|-----------------------------|---------------------------------------------|----------------------------------------------------|-----------------|----------------------------------------------------------------------------------------------------------------|
| `LogDetailsPanel.tsx:133`          | `Elapsed`                   | `perc.ui.publish.logs.details@Elapsed`      | `MSG.PUBLISH.LOGS_DETAILS.ELAPSED`                 | `logs.details`  | No exact match in TMX (only `@Elapsed Time`). Use shorter form for header column.                              |
| `LogDetailsPanel.tsx:184`          | `Revision:`                 | `perc.ui.publish.logs.details@Revision`     | `MSG.PUBLISH.LOGS_DETAILS.REVISION`                | `logs.details`  | Distinct from `perc.ui.publish.title@Revision ID` — header label, not the field id.                            |
| `LogDetailsPanel.tsx:187`          | `Template:`                 | `perc.ui.publish.logs.details@Template`     | `MSG.PUBLISH.LOGS_DETAILS.TEMPLATE`                | `logs.details`  | Distinct from `perc.ui.publish.title@Template ID`.                                                             |
| `ServerEditor.tsx:178`             | `Server Type`               | `perc.ui.publish.server.editor@Server Type` | `MSG.PUBLISH.SERVER.EDITOR.SERVER_TYPE`            | `server.editor` | Replaces the broken `MSG.PUBLISH_SERVER_TYPE = "perc.ui.publish.view@Production"` mapping (see §III footnote). |
| `ServerEditor.tsx:184`             | `Production` (option label) | `perc.ui.publish.server.editor@Production`  | `MSG.PUBLISH.SERVER.EDITOR.SERVER_TYPE_PRODUCTION` | `server.editor` | value=`PRODUCTION` stays machine-readable.                                                                     |
| `ServerEditor.tsx:185`             | `Staging` (option label)    | `perc.ui.publish.server.editor@Staging`     | `MSG.PUBLISH.SERVER.EDITOR.SERVER_TYPE_STAGING`    | `server.editor` | value=`STAGING` stays.                                                                                         |
| `ServerEditor.tsx:196`             | `File` (option label)       | `perc.ui.publish.server.editor@File`        | `MSG.PUBLISH.SERVER.EDITOR.DELIVERY_TYPE_FILE`     | `server.editor` | value=`File` stays (driver type id).                                                                           |
| `ServerEditor.tsx:197`             | `Database` (option label)   | `perc.ui.publish.server.editor@Database`    | `MSG.PUBLISH.SERVER.EDITOR.DELIVERY_TYPE_DATABASE` | `server.editor` | value=`Database` stays.                                                                                        |
| `drivers/FileDriverFields.tsx:168` | `Select` (option label)     | `perc.ui.publish.drivers.file@Select`       | `MSG.PUBLISH.DRIVERS.FILE.SELECT_REGION`           | `drivers.file`  | value=`""` empty-string placeholder; "Select" is the human label.                                              |

### design/ panels

|          file:line           |                 English                  |                          Proposed tuid                           |                  Proposed MSG constant                  |        Sub-area        |                                                                     Notes                                                                      |
|------------------------------|------------------------------------------|------------------------------------------------------------------|---------------------------------------------------------|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `ContentListEditor.tsx:125`  | `Description`                            | `perc.ui.publish.design.contentLists@Description`                | `MSG.PUBLISH.DESIGN.CONTENT_LISTS.DESCRIPTION`          | `design.contentLists`  | New publishing-specific key — do not reuse `perc.ui.editSiteSectionDialog.label@Description` / `perc.ui.roles@Description` (different scopes). |
| `ContentListEditor.tsx:133`  | `Type`                                   | `perc.ui.publish.design.contentLists@Type`                       | `MSG.PUBLISH.DESIGN.CONTENT_LISTS.TYPE`                 | `design.contentLists`  | Same reasoning — `perc.ui.publish.title@Type` exists but the plan asks for `<screen>.@<text>` per area. New key.                               |
| `ContentListEditor.tsx:140`  | `Modern` (option label)                  | `perc.ui.publish.design.contentLists@Modern`                     | `MSG.PUBLISH.DESIGN.CONTENT_LISTS.TYPE_MODERN`          | `design.contentLists`  | value=`modern` stays.                                                                                                                          |
| `ContentListEditor.tsx:141`  | `Legacy` (option label)                  | `perc.ui.publish.design.contentLists@Legacy`                     | `MSG.PUBLISH.DESIGN.CONTENT_LISTS.TYPE_LEGACY`          | `design.contentLists`  | value=`legacy` stays.                                                                                                                          |
| `ContentListEditor.tsx:155`  | `Generator`                              | `perc.ui.publish.design.contentLists@Generator`                  | `MSG.PUBLISH.DESIGN.CONTENT_LISTS.GENERATOR`            | `design.contentLists`  |                                                                                                                                                |
| `ContextsPanel.tsx:252`      | `Description`                            | `perc.ui.publish.design.contexts@Description`                    | `MSG.PUBLISH.DESIGN.CONTEXTS.DESCRIPTION`               | `design.contexts`      | Same reasoning as ContentListEditor — new key per screen.                                                                                      |
| `ContextsPanel.tsx:297`      | `Description`                            | `perc.ui.publish.design.contexts@Scheme Description`             | `MSG.PUBLISH.DESIGN.CONTEXTS.SCHEME_DESCRIPTION`        | `design.contexts`      | Scheme-row label is distinct from the context-level Description.                                                                               |
| `ContextsPanel.tsx:305`      | `Content type id`                        | `perc.ui.publish.design.contexts@Content type id`                | `MSG.PUBLISH.DESIGN.CONTEXTS.SCHEME_CONTENT_TYPE`       | `design.contexts`      | Distinct from `perc.ui.publish.title@Template ID` — text differs ("Content type id" vs "Template ID").                                         |
| `ContextsPanel.tsx:313`      | `Template id`                            | `perc.ui.publish.design.contexts@Template id`                    | `MSG.PUBLISH.DESIGN.CONTEXTS.SCHEME_TEMPLATE`           | `design.contexts`      | Distinct from `Template ID`.                                                                                                                   |
| `ContextsPanel.tsx:339`      | `name` (placeholder)                     | `perc.ui.publish.design.contexts@Parameter Name`                 | `MSG.PUBLISH.DESIGN.CONTEXTS.PARAM_NAME_PLACEHOLDER`    | `design.contexts`      | Lowercase placeholder; localized as `Parameter Name` so the meaning survives in non-en locales.                                                |
| `ContextsPanel.tsx:345`      | `BackendColumn` (option label)           | `perc.ui.publish.design.contexts@BackendColumn`                  | `MSG.PUBLISH.DESIGN.CONTEXTS.PARAM_TYPE_BACKEND_COLUMN` | `design.contexts`      | value=`BackendColumn` stays.                                                                                                                   |
| `ContextsPanel.tsx:348`      | `value` (placeholder)                    | `perc.ui.publish.design.contexts@Parameter Value`                | `MSG.PUBLISH.DESIGN.CONTEXTS.PARAM_VALUE_PLACEHOLDER`   | `design.contexts`      |                                                                                                                                                |
| `ContextsPanel.tsx:402`      | `Publishing context` (aria-label)        | `perc.ui.publish.design.contexts@Publishing context`             | `MSG.PUBLISH.DESIGN.CONTEXTS.LIST_ARIA`                 | `design.contexts`      | aria-label for the `<ul>` list.                                                                                                                |
| `ContextsPanel.tsx:444`      | `No publishing contexts.`                | `perc.ui.publish.design.contexts@No publishing contexts`         | `MSG.PUBLISH.DESIGN.CONTEXTS.EMPTY`                     | `design.contexts`      | Empty state for the contexts list.                                                                                                             |
| `ContextsPanel.tsx:446`      | `Location schemes` (h4)                  | `perc.ui.publish.design.contexts@Location schemes`               | `MSG.PUBLISH.DESIGN.CONTEXTS.SCHEMES_HEADING`           | `design.contexts`      |                                                                                                                                                |
| `ContextsPanel.tsx:448`      | `No schemes for this context.`           | `perc.ui.publish.design.contexts@No schemes for this context`    | `MSG.PUBLISH.DESIGN.CONTEXTS.SCHEMES_EMPTY`             | `design.contexts`      |                                                                                                                                                |
| `DeliveryTypesPanel.tsx:130` | `Description`                            | `perc.ui.publish.design.deliveryTypes@Description`               | `MSG.PUBLISH.DESIGN.DELIVERY_TYPES.DESCRIPTION`         | `design.deliveryTypes` | Same reasoning as contexts — publishing-specific key.                                                                                          |
| `DeliveryTypesPanel.tsx:178` | `No delivery types.`                     | `perc.ui.publish.design.deliveryTypes@No delivery types`         | `MSG.PUBLISH.DESIGN.DELIVERY_TYPES.EMPTY`               | `design.deliveryTypes` |                                                                                                                                                |
| `EditionEditor.tsx:223`      | `Comment`                                | `perc.ui.publish.design.editions@Comment`                        | `MSG.PUBLISH.DESIGN.EDITIONS.COMMENT`                   | `design.editions`      |                                                                                                                                                |
| `EditionEditor.tsx:244`      | `Associated content lists` (h4)          | `perc.ui.publish.design.editions@Associated content lists`       | `MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATED_LISTS`          | `design.editions`      |                                                                                                                                                |
| `EditionEditor.tsx:246`      | `None`                                   | `perc.ui.publish.design.editions@None`                           | `MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATED_LISTS_NONE`     | `design.editions`      | `perc.ui.widget.tinymce@None` exists but is widget-scoped — separate key.                                                                      |
| `EditionEditor.tsx:272`      | `Content list to associate` (aria-label) | `perc.ui.publish.design.editions@Content list to associate`      | `MSG.PUBLISH.DESIGN.EDITIONS.ASSOCIATE_LIST_ARIA`       | `design.editions`      |                                                                                                                                                |
| `EditionEditor.tsx:274`      | `Select content list` (option label)     | `perc.ui.publish.design.editions@Select content list`            | `MSG.PUBLISH.DESIGN.EDITIONS.SELECT_LIST`               | `design.editions`      | value=`""` empty-string marker.                                                                                                                |
| `EditionEditor.tsx:284`      | `Delivery context` (aria-label)          | `perc.ui.publish.design.editions@Delivery context`               | `MSG.PUBLISH.DESIGN.EDITIONS.DELIVERY_CONTEXT_ARIA`     | `design.editions`      |                                                                                                                                                |
| `EditionEditor.tsx:301`      | `Copy to site` (h4)                      | `perc.ui.publish.design.editions@Copy to site`                   | `MSG.PUBLISH.DESIGN.EDITIONS.COPY_TO_SITE_HEADING`      | `design.editions`      |                                                                                                                                                |
| `EditionEditor.tsx:303`      | `Target site`                            | `perc.ui.publish.design.editions@Target site`                    | `MSG.PUBLISH.DESIGN.EDITIONS.TARGET_SITE`               | `design.editions`      |                                                                                                                                                |
| `SiteDesignPanel.tsx:128`    | `Design site` (aria-label)               | `perc.ui.publish.design.site@Design site`                        | `MSG.PUBLISH.DESIGN.SITE.PICKER_ARIA`                   | `design.site`          |                                                                                                                                                |
| `SiteDesignPanel.tsx:142`    | `Property context` (aria-label)          | `perc.ui.publish.design.site@Property context`                   | `MSG.PUBLISH.DESIGN.SITE.PROPERTY_CONTEXT_ARIA`         | `design.site`          |                                                                                                                                                |
| `SiteDesignPanel.tsx:159`    | `Context variables` (h4)                 | `perc.ui.publish.design.site@Context variables`                  | `MSG.PUBLISH.DESIGN.SITE.CONTEXT_VARIABLES_HEADING`     | `design.site`          |                                                                                                                                                |
| `SiteDesignPanel.tsx:179`    | `Name`                                   | `perc.ui.publish.design.site@Property Name`                      | `MSG.PUBLISH.DESIGN.SITE.PROPERTY_NAME`                 | `design.site`          | Distinct from `perc.ui.folderPropsDialog.label@Name` / `perc.ui.workflow@Name` (different scopes).                                             |
| `SiteDesignPanel.tsx:187`    | `Value`                                  | `perc.ui.publish.design.site@Property Value`                     | `MSG.PUBLISH.DESIGN.SITE.PROPERTY_VALUE`                | `design.site`          |                                                                                                                                                |
| `SiteRootBrowser.tsx:110`    | `Empty folder or path not found.`        | `perc.ui.publish.design.siteRoot@Empty folder or path not found` | `MSG.PUBLISH.DESIGN.SITE_ROOT.EMPTY`                    | `design.siteRoot`      |                                                                                                                                                |

### sections/

|        file:line         |                 English                 |                        Proposed tuid                         |                  Proposed MSG constant                  |      Sub-area      |                                                                                  Notes                                                                                   |
|--------------------------|-----------------------------------------|--------------------------------------------------------------|---------------------------------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DesignSection.tsx:183`  | `Design site` (aria-label)              | `perc.ui.publish.sections.design@Design site`                | `MSG.PUBLISH.SECTIONS.DESIGN.SITE_PICKER_ARIA`          | `sections.design`  | Distinct from the SiteDesignPanel aria-label (`sections.design` vs `design.site`); both read "Design site" so the human text is identical — only the sub-prefix differs. |
| `DesignSection.tsx:207`  | `No editions for this site.`            | `perc.ui.publish.sections.design@No editions for this site`  | `MSG.PUBLISH.SECTIONS.DESIGN.EDITIONS_EMPTY`            | `sections.design`  |                                                                                                                                                                          |
| `DesignSection.tsx:244`  | `No content lists.`                     | `perc.ui.publish.sections.design@No content lists`           | `MSG.PUBLISH.SECTIONS.DESIGN.CONTENT_LISTS_EMPTY`       | `sections.design`  |                                                                                                                                                                          |
| `LogsSection.tsx:156`    | `Site id`                               | `perc.ui.publish.sections.logs@Site id`                      | `MSG.PUBLISH.SECTIONS.LOGS.SITE_ID`                     | `sections.logs`    |                                                                                                                                                                          |
| `LogsSection.tsx:175`    | `Server id`                             | `perc.ui.publish.sections.logs@Server id`                    | `MSG.PUBLISH.SECTIONS.LOGS.SERVER_ID`                   | `sections.logs`    |                                                                                                                                                                          |
| `LogsSection.tsx:185`    | `Days`                                  | `perc.ui.publish.sections.logs@Days`                         | `MSG.PUBLISH.SECTIONS.LOGS.DAYS`                        | `sections.logs`    |                                                                                                                                                                          |
| `RuntimeSection.tsx:204` | `Runtime site` (aria-label)             | `perc.ui.publish.sections.runtime@Runtime site`              | `MSG.PUBLISH.SECTIONS.RUNTIME.SITE_PICKER_ARIA`         | `sections.runtime` |                                                                                                                                                                          |
| `RuntimeSection.tsx:227` | `No editions for this site.`            | `perc.ui.publish.sections.runtime@No editions for this site` | `MSG.PUBLISH.SECTIONS.RUNTIME.EDITIONS_EMPTY`           | `sections.runtime` | Same human text as the Design section empty state but different scope — different tuid.                                                                                  |
| `RuntimeSection.tsx:275` | `Demand publish` (h3)                   | `perc.ui.publish.sections.runtime@Demand publish`            | `MSG.PUBLISH.SECTIONS.RUNTIME.DEMAND_HEADING`           | `sections.runtime` |                                                                                                                                                                          |
| `RuntimeSection.tsx:282` | `Content ids`                           | `perc.ui.publish.sections.runtime@Content ids`               | `MSG.PUBLISH.SECTIONS.RUNTIME.CONTENT_IDS`              | `sections.runtime` |                                                                                                                                                                          |
| `RuntimeSection.tsx:287` | `e.g. 101, 102` (placeholder)           | `perc.ui.publish.sections.runtime@e.g. 101, 102`             | `MSG.PUBLISH.SECTIONS.RUNTIME.CONTENT_IDS_PLACEHOLDER`  | `sections.runtime` | Could be a positional arg `{0}` of a generic "e.g. {0}" key — Phase 3 decides; this audit uses the literal.                                                              |
| `RuntimeSection.tsx:301` | `Advanced cleanup` (h3)                 | `perc.ui.publish.sections.runtime@Advanced cleanup`          | `MSG.PUBLISH.SECTIONS.RUNTIME.ADVANCED_CLEANUP_HEADING` | `sections.runtime` |                                                                                                                                                                          |
| `RuntimeSection.tsx:313` | `Purge job log by id`                   | `perc.ui.publish.sections.runtime@Purge job log by id`       | `MSG.PUBLISH.SECTIONS.RUNTIME.PURGE_JOB_LOG`            | `sections.runtime` |                                                                                                                                                                          |
| `SiteWorkspace.tsx:527`  | `select all related items` (aria-label) | `perc.ui.publish.sections.site@Select all related items`     | `MSG.PUBLISH.SECTIONS.SITE.SELECT_ALL_ARIA`             | `sections.site`    | Lowercase `s` in source — capitalize in tuid to match the existing convention (per phase 0 audit rule).                                                                  |
| `SiteWorkspace.tsx:531`  | `Item` (th)                             | `perc.ui.publish.sections.site@Item`                         | `MSG.PUBLISH.SECTIONS.SITE.ITEM_HEADING`                | `sections.site`    |                                                                                                                                                                          |

---

## False positives

Items in the candidate TSV that are **not** in scope for localization:

|             file:line              |         Snippet         |                              Reason (do **not** localize)                              |
|------------------------------------|-------------------------|----------------------------------------------------------------------------------------|
| `ServerEditor.tsx:184`             | `value="PRODUCTION"`    | Machine-readable enum; the visible label `Production` is localized separately (§V).    |
| `ServerEditor.tsx:185`             | `value="STAGING"`       | Machine-readable enum; label `Staging` is localized separately.                        |
| `ServerEditor.tsx:196`             | `value="File"`          | Driver type id; label `File` is localized separately.                                  |
| `ServerEditor.tsx:197`             | `value="Database"`      | Driver type id; label `Database` is localized separately.                              |
| `ContextsPanel.tsx:344`            | `value="String"`        | Parameter type id; label `String` is localized separately.                             |
| `ContextsPanel.tsx:345`            | `value="BackendColumn"` | Parameter type id; label `BackendColumn` is localized separately.                      |
| `drivers/FileDriverFields.tsx:168` | `value=""`              | Empty-string option marker (placeholder option); the label `Select` is localized (§V). |

No JSDoc, console.error, data-testid, regex, class names, or HTTP error codes
appear in the candidate TSV — the upstream regex sweep already filtered them.

`MSG.PUBLISH_SERVER_TYPE = "perc.ui.publish.view@Production"` is a **catalog
bug** (not a false positive): the constant is wired into the `<label>` of the
*Server Type* select, but its tuid resolves to the *Production* option text.
Phase 1 should point this constant at the new `perc.ui.publish.server.editor@Server Type`
tuid (§V). The `perc.ui.publish.view@Production` reference can then be deleted
(the `perc.ui.publish.view@` prefix has only `@Add Server` in TMX today).

---

## Sub-PR split recommendation

The publishing area is **68 candidates** across 13 files. A single PR-B will
land ~500–700 lines of diff (TU additions + JSX wiring + Vitest + Playwright),
which is at the upper limit of what the WebUI/AGENTS.md "screen-by-screen"
rule wants to review in one pass. **Recommend splitting PR-B into three
sub-PRs**, each scoped to a coherent review boundary:

1. **PR-B1 — Shell + sections** (`PublishingShell.tsx`, `sections/LogsSection.tsx`,
   `sections/RuntimeSection.tsx`, `sections/DesignSection.tsx`,
   `sections/SiteWorkspace.tsx`, `components/LogDetailsPanel.tsx`). ~25 new
   tuids. Touches the read-mostly screens: filters, browse, detail. Playwright
   spec `tests/publishing-logs.spec.js` + extend `tests/publishing.spec.js`.
2. **PR-B2 — Design panels** (`design/ContentListEditor.tsx`,
   `design/ContextsPanel.tsx`, `design/DeliveryTypesPanel.tsx`,
   `design/EditionEditor.tsx`, `design/SiteDesignPanel.tsx`,
   `design/SiteRootBrowser.tsx`). ~25 new tuids. Touches authoring flow.
   Playwright spec `tests/publishing-design.spec.js`.
3. **PR-B3 — Server editor + drivers** (`components/ServerEditor.tsx`,
   `components/drivers/FileDriverFields.tsx`, future
   `components/drivers/DatabaseDriverFields.tsx`). ~10 new tuids + the
   `MSG.PUBLISH_SERVER_TYPE` catalog-bug fix. Touches the write path (create
   / edit server). Playwright spec `tests/publishing-servers.spec.js`.

Each sub-PR's body must list the audit rows it implements (file:line) and the
Playwright spec(s) that cover the new behavior, per the Phase 3 hard gate.

---

## Audit row totals

|                    Sub-area                     | Real (needs key) | Reuse MSG | Reuse TMX (new MSG) |    New | False positive |
|-------------------------------------------------|-----------------:|----------:|--------------------:|-------:|---------------:|
| `components/` (server + logs details + drivers) |               18 |         3 |                   8 |      7 |              4 |
| `design/`                                       |               33 |         0 |                   1 |     32 |              2 |
| `sections/`                                     |               17 |         0 |                   2 |     15 |              0 |
| **Total**                                       |           **68** |     **3** |              **11** | **54** |          **6** |

(See header for the per-file hit-count breakdown.)
