# Data Model: Home + Widget Builder (UI migration)

**Feature**: 989-react-cui-widget-builder  
**Note**: No new persistent schema. Entities below are **client/DTO views** of existing server models and REST payloads.

## Home / contributor workspace

Logical UI entity; not a DB table.

|   Attribute   |                    Description                     |
|---------------|----------------------------------------------------|
| activeSection | One of: `recent`, `library`, `search`, `create`    |
| siteContext   | Selected site name/id when multi-site              |
| permissions   | Derived from session/roles (empty-state messaging) |

### Content item (list/search/open)

| Field (conceptual) |                    Notes                     |
|--------------------|----------------------------------------------|
| id                 | Content id (server format as returned today) |
| name / title       | Display name                                 |
| path               | Folder/site path                             |
| type               | Page, asset, blog, etc.                      |
| status / workflow  | When provided by list APIs                   |
| thumbnailPath      | Optional                                     |
| lastModified*      | Optional metadata for Recent                 |

**Relationships**: Content items live under sites/folders; Library navigates folder tree; Open hands off to existing editor routes (out of scope to rewrite editor).

### Site / folder node

|    Field     |         Notes          |
|--------------|------------------------|
| title / name | Display                |
| path         | Absolute CMS path      |
| isFolder     | Browse vs open-as-item |

### Search criteria

|   Field    |                         Notes                         |
|------------|-------------------------------------------------------|
| query text | User input                                            |
| filters    | Workflow/state/type if exposed by existing search API |
| results    | List of content items                                 |

### Create request (page / asset / blog)

Full UX and MUST/SHOULD rows: [home-capability-matrix.md](./contracts/home-capability-matrix.md) §6.

| Flow  |                 Key inputs (existing capability)                  |                         Picker requirements (parity)                          |
|-------|-------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Page  | title, file name, templateId, folderPath (+ site when multi-site) | Site (if multi), template list, folder list/tree—not free-text-only IDs/paths |
| Asset | folderPath, widget/asset type id                                  | Asset type list, asset folder list/tree                                       |
| Blog  | site, blog target, title, file name                               | Site, blogs-for-site list                                                     |

Validation: server-side rules remain authoritative; client shows errors. After create: open or locate item (same as classic).

## Widget definition

Maps to `PSWidgetBuilderDefinitionData` / summary types.

### Summary (`PSWidgetBuilderSummaryData`)

|             Field              |    Notes    |
|--------------------------------|-------------|
| widgetId                       | Long id     |
| label, prefix, version, author | Metadata    |
| description, publisherUrl      | Metadata    |
| responsive                     | Flag        |
| toolTipMessage, tray icon path | UX metadata |

### Full definition (`PSWidgetBuilderDefinitionData`)

Extends summary with:

|          Field           |                    Notes                     |
|--------------------------|----------------------------------------------|
| fieldsList               | Ordered field definitions (type, name, etc.) |
| widgetHtml               | Display template HTML                        |
| jsFileList / cssFileList | Resource lists                               |
| (deployed state)         | Via `isWidgetDefinitionDeployed`             |

### Validation results (`PSWidgetBuilderValidationResults`)

|       Field       |              Notes              |
|-------------------|---------------------------------|
| errors / messages | Block save/package when present |
| saved id          | On successful save              |

### Widget package

Server-generated deployable artifact from `deployWidget(definitionId)`; UI triggers only—no client-side package algorithm.

## Navigation entry

|   Field   |               Notes                |
|-----------|------------------------------------|
| view key  | `home`, `widgetbuilder`, `dash`, … |
| shell JSP | Modern thin shell after cutover    |
| roles     | adminViews / designerViews for WB  |

## Feature enablement

|                       Source                       |                 Notes                  |
|----------------------------------------------------|----------------------------------------|
| `WidgetBuilderActive` / `isWidgetBuilderEnabled()` | Hides or denies WB when false          |
| index.jsp attribute `IS_WIDGET_BUILDER_ACTIVE`     | Nav visibility today—preserve behavior |

## State transitions

### Widget definition lifecycle (UI)

```text
[Empty list] --create--> [Edit draft in client]
[Edit] --validate--> [Invalid | Valid]
[Valid] --save--> [Persisted]   // last write wins
[Persisted] --deploy--> [Deployed package]
[Persisted] --delete--> [Removed]
```

No client merge/version precondition required unless server already returns conflicts.

### Home deep-link → section

```text
initialScreen=library  → Library
initialScreen=list     → Recent (list)
initialScreen=search   → Search
initialScreen=newitem  → Create
(missing/default)      → product default (typically Recent)
```

## Validation rules (product)

- Widget name uniqueness for new definitions (server validator).
- Required metadata/fields per existing `PSWidgetBuilder*Validator` classes.
- Home create required fields per existing page/asset/blog services.
- CSRF required on mutating REST calls.

