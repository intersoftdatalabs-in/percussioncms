# Component Package Manifest (schema v1.0)

| Field | Value |
|-------|--------|
| **Status** | Accepted for Phase 3 slice 1 (#2750) |
| **Schema version** | `1.0` |
| **Ship file name** | `component-package.json` |
| **Java model** | `com.percussion.packages.manifest.PSComponentPackageManifest` (`modules/perc-packages`) |
| **ADR** | [ADR-004](./adr/004-no-definition-xml-packaging.md) |
| **Plan** | [plan.md § Phase 3](./plan.md) |

## Purpose

Product **source of truth** for a packaged component (former Widget / Page-definition / Gadget XML role):

```text
Content types + Templates (assembler + JEXL bindings + source)
  + Slots (incl. layout/styles) + Catalog metadata + Resources
```

This document describes the **ship format** (what product packages author and install) versus **upgrade-input XML** (legacy Widget / Page meta / Gadget definition files consumed only by compilers and a time-boxed runtime shim).

## Ship format vs upgrade-input XML

| Concern | Ship format (modern) | Upgrade input (legacy) |
|---------|----------------------|------------------------|
| Identity / deps | `component-package.json` fields `id`, `version`, `dependencies`, `publisher`, `cmsVersion` | `psx_archiveInfo.xml` / `PSXDescriptor` |
| Palette metadata | `catalog` object | Widget `WidgetPrefs` (title, category, thumbnail, …) or gadget registry |
| Content types | `contentTypes[]` + package-relative CT artifacts | `*.contentType` trees inside `.ppkg` |
| Templates + JEXL | `templates[]` (`assembler`, `sourceRef`, `bindings[]`) | Widget `Code` (jexl) + `Content` (velocity/html) and/or `*.templateDef` |
| Slots + layout/styles | `slots[]` with `layout` / `styles` maps (ADR-003) | CM1 region tree + widget instance css/user prefs |
| Static assets | `resources[]` (`path` → install `target`) | `SupportFile-*` / `sys__UserDependency--*` trees |
| Instance prefs | `userPreferences[]` / `cssPreferences[]` (transitional) | Widget `UserPref` / `CssPref` |
| Definition XML | **Not present** in product source | `rxconfig/Widgets/*.xml`, page meta XML, gadget definition XML |

**Rules (ADR-004):**

1. Product packages **ship** without Page / Widget / Gadget definition XML as the authoring format.
2. Upgrade / compiler tools **read** legacy XML and **emit** this manifest + artifacts (sibling slice #2751).
3. Runtime may keep a **time-boxed shim** when modern package is absent (sibling slice #2752).

## File placement

```text
<package-source>/
  component-package.json          ← this manifest (schemaVersion 1.0)
  contentTypes/<typeName>/…       ← optional; refs from contentTypes[].ref
  templates/<templateName>.vm     ← optional; refs from templates[].sourceRef
  resources/…                     ← optional; refs from resources[].path
  … legacy .ppkg staging files may coexist during migration …
```

Paths inside the manifest are **package-relative**, always with `/` separators (URL / zip entry style). Absolute OS paths (`C:\…`, `/tmp/…`, UNC) and `..` segments are invalid.

## Root object

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `schemaVersion` | string | yes | Must be `"1.0"` for this model |
| `id` | string | yes | Stable package id (e.g. `perc.widget.title`) |
| `name` | string | yes | Display name |
| `version` | string | yes | Package semver / product version string |
| `description` | string | no | Human summary |
| `publisher` | object | no | `{ "name", "url" }` |
| `cmsVersion` | object | no | `{ "min", "max" }` compatible CMS range |
| `dependencies` | array | no | `{ "name", "version", "implied" }` |
| `catalog` | object | no | Palette / UI metadata (see below) |
| `contentTypes` | array | * | At least one of `contentTypes` or `templates` required |
| `templates` | array | * | At least one of `contentTypes` or `templates` required |
| `slots` | array | no | Composition holes with optional layout/styles |
| `resources` | array | no | CSS/JS/images |
| `userPreferences` | array | no | Transitional from Widget `UserPref` |
| `cssPreferences` | array | no | Transitional from Widget `CssPref`; prefer slot styles long-term |

## `catalog`

| Field | Type | Notes |
|-------|------|-------|
| `kind` | string | Default `component`; also `page`, `gadget`, … |
| `title` | string | Palette title |
| `category` | string | e.g. `content` |
| `description` | string | |
| `thumbnail` / `icon` | string | Package-relative resource path |
| `author` | string | |
| `preferredEditorWidth` / `preferredEditorHeight` | number | Editor chrome |
| `createSharedAsset` | boolean | |
| `editableOnTemplate` | boolean | |
| `responsive` | boolean | |
| `paletteVisible` | boolean | Default `true` |

## `contentTypes[]`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | string | yes | Content type name |
| `ref` | string | no | Package-relative path to CT artifact tree |

## `templates[]`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | string | yes | Template name |
| `type` | string | no | `snippet` (default), `page`, `global`, `binary`, `resource` |
| `assembler` | string | no | e.g. `velocityAssembler`, `htmlAssembler`, `markdownAssembler` |
| `sourceRef` | string | no | Package-relative path to source |
| `contentType` | string | no | Associated content type name |
| `bindings` | array | no | Ordered JEXL `{ "variable", "expression" }` (ADR-001 — JEXL only) |

## `slots[]`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `name` | string | yes | Slot name |
| `allowedContentTypes` | string[] | no | |
| `layout` | object | no | Free-form map → Phase 2 `slot_layout` |
| `styles` | object | no | Free-form map → Phase 2 `slot_styles` |

## `resources[]`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `path` | string | yes | Package-relative source path |
| `target` | string | no | Install-relative target (URL-style `/`) |
| `type` | string | no | `css`, `js`, `image`, … |

## `userPreferences[]` / `cssPreferences[]`

Transitional mirrors of Widget `UserPref` / `CssPref` so the XML compiler can land without inventing a second preference dialect. New design tools should prefer **slot layout/styles** for presentational concerns (ADR-003).

### User preference

| Field | Type | Notes |
|-------|------|-------|
| `name` | string | required |
| `displayName` | string | |
| `datatype` | string | e.g. `enum`, `string` |
| `required` | boolean | |
| `defaultValue` | string | |
| `enumValues` | array | `{ "value", "displayValue" }` |

### CSS preference

| Field | Type | Notes |
|-------|------|-------|
| `name` | string | required |
| `displayName` | string | |
| `datatype` | string | |
| `defaultValue` | string | |

## Minimal example

See test fixture:

`modules/perc-packages/src/test/resources/manifests/minimal-component-package.json`

(Title widget-shaped minimal package: one content type, one snippet template, one slot, one image resource, one user pref, one css pref.)

## Java API

| Type | Role |
|------|------|
| `PSComponentPackageManifest` | Typed model + nested types |
| `PSComponentPackageManifestIo` | JSON parse / write / Path I/O |
| `PSComponentPackageManifestValidator` | Structural validation |
| `PSComponentPackageManifestException` | Parse / validation failure |

```java
PSComponentPackageManifest m = PSComponentPackageManifestIo.read(path);
PSComponentPackageManifestValidator.validate(m);
String json = PSComponentPackageManifestIo.toJson(m);
```

## Relationship to existing package system

- **Today:** `PSPackageBuilder` zips legacy package source trees into `.ppkg` for the deployer (`psx_archiveInfo.xml` / dependency trees). That path remains for install compatibility.
- **This manifest:** extends the packaging story with an explicit, tool-friendly **component** model that does not require Widget/Page/Gadget XML. Future slices wire compiler output and install recognition; this slice lands model + schema + tests only.

## Widget XML compiler (slices #2751 / #2772)

Upgrade-input path for **baseWidgets** and the **high-traffic residual batch**:

| Type | Role |
|------|------|
| `PSWidgetXmlParser` | Secure DOM parse of `<Widget>` definition XML (prefs, code, content, **Resource**) |
| `PSWidgetXmlCompiler` | XML model → `PSComponentPackageManifest` + template text artifacts |
| `PSWidgetXmlPackageCompiler` | Scan `sys__UserDependency--rxconfig/Widgets/*.xml` under a package root; `compileHighTrafficPackages` |
| Golden fixtures | `modules/perc-packages/src/test/resources/widgetxml/golden/` (percSimpleText, percTitle, simplePageAutoList, percNavBreadcrumb) |

Inventory + residual packages: [widget-xml-inventory.md](./widget-xml-inventory.md).

## Out of scope (sibling / residual)

| Slice | Issue | Role |
|-------|-------|------|
| Runtime legacy shim | #2752 | Load customer XML when modern package absent |
| Remaining high-traffic / long-tail widgets | residual under #2630 | blog, calendar, directory, social, forms, auto-lists, … (see inventory) |
| Delete product Widget XML from source | later | After install path consumes modern artifacts |

## Validation summary

- `schemaVersion` must be `1.0`
- `id`, `name`, `version` required non-blank
- At least one `contentTypes[]` or `templates[]` entry
- Nested names required when entries present
- Package-relative paths: `/` only, no `..`, no absolute OS forms
