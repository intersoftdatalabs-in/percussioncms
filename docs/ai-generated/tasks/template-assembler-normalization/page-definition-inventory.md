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

There is **no** product tree under `rxconfig/Pages/*.xml` in `modules/perc-packages` today. The dual-run shim still recognizes `rxconfig/Pages` for customer installs (see [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)). **G4 ship-path inventory (#3581 / #3737):** `PSPageDefinitionXmlInventory` + Surefire fails CI if Page definition XML reappears under `sys__UserDependency--rxconfig/Pages` or `rxconfig/Pages` (waiver set empty after perc.Test page dual-ship exit).

CM1 **page item** region trees / widget instances live in site storage (sitemanage domain), not as package `*.templateDef` files. This slice covers **packaged page templates** only; page-item composition upgrade remains a residual under #2630 when storage write-path is ready.

## Product packages with Page layout templates

| Package | Modern `pages/` count | Assembler (typical) | Output format | Notes |
|---------|----------------------:|---------------------|---------------|-------|
| `perc.baseTemplates` | 20 | `pageAssembler` | Page | **Modern authoring (#2786)**; **native install** (#2806) — dual-ship roots off |
| `perc.responsiveTemplates` | 3 | `pageAssembler` | Page | **Modern authoring (#2786)**; **native install** (#2806); Banded / Basic / plain |
| `perc.Baseline` | 7 | mix (`velocityAssembler`, `pageVariantAssembler`, `dispatchAssembler`, `resourceAssembler`, `pageDatabaseAssembler`) | Page / Global / Snippet | **Modern authoring (#2805)**; **native install** (#3673) — dual-ship roots off |

Snippet-style `*.templateDef` files also appear inside **widget** packages (e.g. file/image binary templates). Those are **not** page layout packages; leave them to widget conversion residuals unless inventory shows Page `output-format`.

### `perc.baseTemplates` (layout catalog) — modern ship layout

```text
Packages/perc.baseTemplates/
  package-install.properties    ← page.installMode=native (#2806)
  pages/<templateId>/component-package.json
  pages/<templateId>/templates/<templateId>.vm
  *.templateDef.aclDef          ← ACL side-cars (unchanged)
  perc.baseTemplates.mapping.properties
  psx_archiveInfo.xml
  sys__UserDependency--rx_resources/…
```

Native install (package build, #2806): archive `TemplateDef-N/<stem>.templateDef` staged from `pages/` with GUIDs from mapping (`TemplateDef-N` → `0-4-N`). Dual-ship root materialization is off for this package. Product source trees **do not** author root `*.templateDef`.

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

**Install packaging (#2786 dual-ship + #2806 / #3673 native + #3949 native default):** product **authors** modern `pages/` for `perc.baseTemplates`, `perc.responsiveTemplates`, and `perc.Baseline`. Package-local `package-install.properties` may set `page.installMode=native` (also the policy **default** when unset); dual-ship root `*.templateDef` generation is **off** unless `page.installMode=dual-ship` or sysprop `perc.packages.page.installMode=dual-ship`. `PSPageXmlNativeInstall` stages archive `TemplateDef-N/<stem>.templateDef` from modern pages (same XML/GUID semantics as dual-ship). Policy: `PSPageXmlInstallPolicy`. Retirement checklist: [dual-ship-page-template-retirement.md](./dual-ship-page-template-retirement.md).

## Golden fixtures

| Fixture | Path |
|---------|------|
| Upgrade-input | `modules/perc-packages/src/test/resources/pagexml/perc.base.plain.templateDef` |
| Manifest golden | `…/pagexml/golden/perc.base.plain.component-package.json` |
| Template golden | `…/pagexml/golden/perc.base.plain.vm` |
| Compiler tests | `com.percussion.packages.pagexml.PSPageXmlCompilerTest` |
| Dual-ship tests | `com.percussion.packages.pagexml.PSPageXmlDualShipTest` |
| Native install tests | `com.percussion.packages.pagexml.PSPageXmlNativeInstallTest` |
| Product modern sources | `…/Packages/perc.baseTemplates/pages/`, `…/Packages/perc.responsiveTemplates/pages/`, `…/Packages/perc.Baseline/pages/` |
| Native opt-in | `…/Packages/perc.baseTemplates/package-install.properties` (and responsive + Baseline) |

## Residuals (not this PR)

1. **Runtime deployer** reading `component-package.json` from archive (today native path is package-build staging into TemplateDef wire format).
2. **Thumbnails / resources** wiring for template images into `resources[]`.
3. **Baseline system templates** conversion matrix landed (#2805) with native opt-in (#3673). CI dual-ship log/path gate landed (#3675). Remaining: widget leftover binary `*.templateDef` (#3674).
4. **Page item composition** (site storage region trees) → slot composition IR — depends on Phase 2 storage / REST (#2690 family).
5. **Delete dual-ship code path** when all page packages use native (see retirement checklist).

## Related

- Widget XML inventory: [widget-xml-inventory.md](./widget-xml-inventory.md)
- Region ↔ slot: [region-slot-mapping.md](./region-slot-mapping.md)
- Plan Phase 3: [plan.md](./plan.md)
