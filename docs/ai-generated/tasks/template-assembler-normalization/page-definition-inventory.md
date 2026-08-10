# Page definition packaging inventory (Phase 3 / #2770 + #2786)

| Field | Value |
|-------|--------|
| **Status** | Compiler (#2770) + modern authoring dual-ship (#2786) for base/responsive templates |
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

## Product packages with Page layout templates

| Package | Modern `pages/` count | Assembler (typical) | Output format | Notes |
|---------|----------------------:|---------------------|---------------|-------|
| `perc.baseTemplates` | 20 | `pageAssembler` | Page | **Modern authoring (#2786)**; dual-ship generates install `*.templateDef` at package build |
| `perc.responsiveTemplates` | 3 | `pageAssembler` | Page | **Modern authoring (#2786)**; Banded / Basic / plain |
| `perc.Baseline` | (still `*.templateDef`) | mix (`pageAssembler`, `velocityAssembler`, …) | Page / Global | System templates residual — convert carefully |

Snippet-style `*.templateDef` files also appear inside **widget** packages (e.g. file/image binary templates). Those are **not** page layout packages; leave them to widget conversion residuals unless inventory shows Page `output-format`.

### `perc.baseTemplates` (layout catalog) — modern ship layout

```text
Packages/perc.baseTemplates/
  pages/<templateId>/component-package.json
  pages/<templateId>/templates/<templateId>.vm
  *.templateDef.aclDef          ← ACL side-cars (unchanged)
  perc.baseTemplates.mapping.properties
  psx_archiveInfo.xml
  sys__UserDependency--rx_resources/…
```

Install dual-ship (package build): root `*.templateDef` regenerated into staging from `pages/` with GUIDs from mapping (`TemplateDef-N` → `0-4-N`). Product source trees **do not** author root `*.templateDef`.

| Template name | Label (sample) | Regions (`#region`) |
|---------------|----------------|---------------------|
| `perc.base.plain` | Plain | `perc-content` |
| `perc.base.header` | Header | header-shaped |
| `perc.base.footer` | Footer | footer-shaped |
| `perc.base.headerFooter` | Header Footer | `header`, `content`, `footer` |
| `perc.base.Box` | Box | `header`, `leftsidebar`, `content`, `rightsidebar`, `footer` |
| `perc.base.leftSidebar` / `rightSidebar` / `rightLeftSidebar` | sidebars | multi-region |
| `perc.base.lLeft*` / `lRight*` / `invertedL*` / `cClamp*` | L / clamp layouts | multi-region |

### `perc.responsiveTemplates` — modern ship layout

| Template name | Label |
|---------------|-------|
| `perc.resp.plain` | Plain |
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

**Dual-ship (install parity, #2786):** product **authors** modern `pages/` only for `perc.baseTemplates` and `perc.responsiveTemplates`. `PSPackageBuilder` calls `PSPageXmlDualShip.materializeInstallTemplateDefs` so `.ppkg` still contains root `*.templateDef` for deployer `TemplateDef` handlers. Semantic parity (name, assembler, body, GUID, slots) is unit-tested in `PSPageXmlDualShipTest`. Native deployer install of `component-package.json` remains a follow-on.

## Golden fixtures

| Fixture | Path |
|---------|------|
| Upgrade-input | `modules/perc-packages/src/test/resources/pagexml/perc.base.plain.templateDef` |
| Manifest golden | `…/pagexml/golden/perc.base.plain.component-package.json` |
| Template golden | `…/pagexml/golden/perc.base.plain.vm` |
| Compiler tests | `com.percussion.packages.pagexml.PSPageXmlCompilerTest` |
| Dual-ship tests | `com.percussion.packages.pagexml.PSPageXmlDualShipTest` |
| Product modern sources | `…/Packages/perc.baseTemplates/pages/`, `…/Packages/perc.responsiveTemplates/pages/` |

## Residuals (not this PR)

1. **Native deployer install** of modern page `component-package.json` (then dual-ship generator can retire).
2. **Thumbnails / resources** wiring for template images into `resources[]`.
3. **Baseline system templates** conversion matrix (Global / Xml / Database / Dispatcher).
4. **Page item composition** (site storage region trees) → slot composition IR — depends on Phase 2 storage / REST (#2690 family).

## Related

- Widget XML inventory: [widget-xml-inventory.md](./widget-xml-inventory.md)
- Region ↔ slot: [region-slot-mapping.md](./region-slot-mapping.md)
- Plan Phase 3: [plan.md](./plan.md)
