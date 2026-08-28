---
id: developer-page-packages
title: Product page packages
description: Native-only install of product page layout packages in Percussion CMS 8.2
version: "8.2"
order: 54
tags: [developer, packages, templates, install]
---

# Product page packages

Percussion CMS 8.2 **product** page layout packages install **natively**. Dual-ship of
root `*.templateDef` files is **retired as the product ship path**.

Operators and integrators who install or rebuild product packages do **not** author or
maintain dual-ship root `*.templateDef` files for those packages.

## What you get on 8.2

Product packages such as `perc.baseTemplates`, `perc.responsiveTemplates`, and
`perc.Baseline` are authored as modern `pages/<id>/component-package.json` plus template
sources (Velocity / assembler). At **package-build** time the CMS stages assembly-template
XML into the `.ppkg` archive (`TemplateDef-N/`) so deployer install is unchanged.

| Package | Native install |
|---------|----------------|
| `perc.baseTemplates` | `package-install.properties` → `page.installMode=native` |
| `perc.responsiveTemplates` | `page.installMode=native` |
| `perc.Baseline` (system templates) | `page.installMode=native` (stable GUIDs on a **fresh** 8.2 host — see [Design templates](id:admin-design-templates)) |
| File / image leftover binary TemplateDefs | Converted to modern `pages/` with native install |

The `.ppkg` that deployer installs still contains assembly-template XML under
`TemplateDef-N/`. Native install means **package build** is the consumer of modern
`pages/`; it does **not** require deployer to parse `component-package.json` at runtime.

## `page.installMode=native`

Each converted product package sets this at the package root:

```properties
page.installMode=native
```

Native mode:

1. Skips generating dual-ship root `*.templateDef` next to modern `pages/`.
2. Writes archive `TemplateDef-N/<stem>.templateDef` from the modern package (same
   GUID, name, assembler, and body as the former dual-ship emit).

That is the **product** setting for 8.2 page layout packages.

### Optional build overrides

Use these only for one-off or CI builds. They are not required for a normal product
install.

| Knob | Values | Notes |
|------|--------|-------|
| Package-local `page.installMode` | `native` or `dual-ship` | Product packages use `native` |
| JVM `perc.packages.page.installMode` | `native` or `dual-ship` | Overrides the package-local value |
| JVM `perc.packages.dualShip.pageTemplateDefs` | `false`, `0`, or `off` | Forces native when modern pages exist |

Packages that have **not** set `page.installMode` resolve to **native** (policy default
#3949). Dual-ship is explicit opt-in only (`page.installMode=dual-ship` or JVM
`perc.packages.page.installMode`). Package-build **fails closed** if dual-ship is
selected — it does **not** materialize root `*.templateDef`. **Shipped product** page
packages on 8.2 are native-only.

## Dual-ship vs dual-run (do not mix them up)

| Concept | Layer | Product 8.2 status |
|---------|-------|--------------------|
| **Dual-ship** page `*.templateDef` | Package **build** | Retired as the product ship path. Product packages use native install. |
| **Dual-run** definition XML shim | **Runtime** selection | Still required so customer Widget / Page / Gadget XML loads when no modern package is present. |

Native page package install is **not** the same as removing the runtime legacy-definition
XML shim. Customer XML fallback stays until Phase 5 removal criteria are met.

## Related

- [Extensions & packages](id:developer-extensions)
- [Design templates](id:admin-design-templates)
- [Installation Overview](id:install-overview)
- [Upgrade Overview](id:upgrade-overview)
