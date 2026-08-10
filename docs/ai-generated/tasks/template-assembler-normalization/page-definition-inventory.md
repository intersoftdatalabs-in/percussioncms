# Page definition packaging inventory (Phase 3 / #2770)

| Field | Value |
|-------|--------|
| **Status** | Inventory + compiler landed (#2770) |
| **Parent** | #2630 · Grandparent #2626 |
| **Code** | `modules/perc-packages/.../pagexml/PSPageXml*.java` |
| **ADR** | [ADR-004](./adr/004-no-definition-xml-packaging.md) |
| **Manifest** | [component-package-manifest.md](./component-package-manifest.md) |

## What “Page definition XML” is in product packages

Unlike widgets (`rxconfig/Widgets/*.xml`), product **page layout** packaging is primarily **assembly template definitions**:

| Artifact | Extension / root | Role |
|----------|------------------|------|
| Assembly template def | `*.templateDef` / `<assembly-template>` | Page (or Global) layout Velocity body + `pageAssembler` + `#region` holes |
| ACL side-car | `*.templateDef.aclDef` | ACL for the template dependency (out of compiler scope) |
| Thumbnail resources | `sys__UserDependency--rx_resources/images/TemplateImages/…` | Palette thumbs (not compiled in #2770) |
| Package identity | `psx_archiveInfo.xml` | Version / publisher / CMS range for compiler context |

There is **no** product tree under `rxconfig/Pages/*.xml` in `modules/perc-packages` today. The dual-run shim still recognizes `rxconfig/Pages` for customer installs (see [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)).

CM1 **page item** region trees / widget instances live in site storage (sitemanage domain), not as package `*.templateDef` files. This slice covers **packaged page templates** only; page-item composition upgrade remains a residual under #2630 when storage write-path is ready.

## Product packages with Page layout templateDefs

| Package | `*.templateDef` count (approx) | Assembler (typical) | Output format | Notes |
|---------|--------------------------------:|---------------------|---------------|-------|
| `perc.baseTemplates` | 20 | `pageAssembler` | Page | Primary golden target (`perc.base.plain`) |
| `perc.responsiveTemplates` | 3 | `pageAssembler` | Page | Banded / Basic / plain |
| `perc.Baseline` | 4+ page-related | mix (`pageAssembler`, `velocityAssembler`, …) | Page / Global | System templates (`perc.page`, `perc.pageXml`, …) — convert carefully |

Snippet-style `*.templateDef` files also appear inside **widget** packages (e.g. file/image binary templates). Those are **not** page layout packages; leave them to widget conversion residuals unless inventory shows Page `output-format`.

### `perc.baseTemplates` (layout catalog)

| Template name | Label (sample) | Regions (`#region`) |
|---------------|----------------|---------------------|
| `perc.base.plain` | Plain | `perc-content` |
| `perc.base.header` | Header | header-shaped |
| `perc.base.footer` | Footer | footer-shaped |
| `perc.base.headerFooter` | Header Footer | `header`, `content`, `footer` |
| `perc.base.Box` | Box | `header`, `leftsidebar`, `content`, `rightsidebar`, `footer` |
| `perc.base.leftSidebar` / `rightSidebar` / `rightLeftSidebar` | sidebars | multi-region |
| `perc.base.lLeft*` / `lRight*` / `invertedL*` / `cClamp*` | L / clamp layouts | multi-region |

Full file list: `modules/perc-packages/src/main/resources/Packages/perc.baseTemplates/*.templateDef`.

### `perc.responsiveTemplates`

| Template name | Label |
|---------------|-------|
| `perc.resp.plain` | (plain responsive) |
| `perc.resp.Basic` | Basic |
| `perc.resp.Banded` | Banded |

## Compiler mapping (upgrade-input → modern package)

| Source (`*.templateDef`) | Target (Component Package Manifest) |
|--------------------------|-------------------------------------|
| `<name>` | `id`, `templates[0].name` |
| `<label>` | `name`, `catalog.title` |
| `<description>` | `description` / `catalog.description` |
| Package `psx_archiveInfo` | `version`, `publisher`, `cmsVersion`, `dependencies` |
| `<assembler>` extension path | short assembler id (`pageAssembler`, `velocityAssembler`, …) |
| `<output-format>` Page/Global/… | `templates[].type` (`page` / `global` / …) |
| `<template>` body (entity-decoded) | `templates/<name>.vm` text artifact + `sourceRef` |
| `#region("id" …)` | `slots[]` named by region id |
| Matching `id="…" class="perc-region perc-vertical …"` | `slots[].layout.orientation`, `slots[].styles.rootclass` (+ span hints) |
| `catalog.kind` | always `page` for this compiler |

**Dual-run (documented):** product packages **still ship** `*.templateDef` as install input until a residual removes them. The compiler emits modern `component-package.json` + template sources for upgrade tooling and future authoring; install recognition of the modern format is a follow-on.

## Golden fixtures

| Fixture | Path |
|---------|------|
| Input | `modules/perc-packages/src/test/resources/pagexml/perc.base.plain.templateDef` |
| Manifest golden | `…/pagexml/golden/perc.base.plain.component-package.json` |
| Template golden | `…/pagexml/golden/perc.base.plain.vm` |
| Tests | `com.percussion.packages.pagexml.PSPageXmlCompilerTest` |

## Residuals (not this PR)

1. **Remove product `*.templateDef` authoring** after install path consumes modern packages (and dual-run exit criteria).
2. **Thumbnails / resources** wiring for template images into `resources[]`.
3. **Baseline system templates** conversion matrix (Global / Xml / Database / Dispatcher).
4. **Page item composition** (site storage region trees) → slot composition IR — depends on Phase 2 storage / REST (#2690 family).
5. **Gadget registry** conversion (sibling slice).

## Related

- Widget XML inventory: [widget-xml-inventory.md](./widget-xml-inventory.md)
- Region ↔ slot: [region-slot-mapping.md](./region-slot-mapping.md)
- Plan Phase 3: [plan.md](./plan.md)
