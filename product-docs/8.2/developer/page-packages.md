---
id: developer-page-packages
title: Product page packages
description: Native-only install of product page layout packages in Percussion CMS 8.2
version: "8.2"
order: 55
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

## Assembler selection from the manifest

A manifest-authored page template selects its assembler through the modern Component
Package Manifest path: the short `templates[].assembler` id resolves against the
registered product catalog (`Java code: PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS`),
so new page templates never need Widget / Page definition XML.

| Manifest `assembler` id | Resolves to | Use for |
|-------------------------|-------------|---------|
| `velocityAssembler` | `Java/global/percussion/assembly/velocityAssembler` | Power path: macros, loops, Active Assembly |
| `htmlAssembler` | `Java/global/percussion/assembly/htmlAssembler` | HTML-first snippets and pages (`${path}` placeholders) |
| `markdownAssembler` | `Java/global/percussion/assembly/markdownAssembler` | CommonMark → HTML after JEXL bindings |
| `pageAssembler` / `pageVariantAssembler` | `…/pageAssembler` / `…/pageVariantAssembler` | Page and page-variant layouts |
| `legacyAssembler` (alias `xslAssembler`) | `…/legacyAssembler` | XSL migration path (see [XSL and legacyAssembler support](id:admin-xsl-legacy-assembler)) |
| `dispatchAssembler` / `resourceAssembler` / `pageDatabaseAssembler` / `binaryAssembler` | `…/<id>` | Dispatch, resource, database, and binary templates |

Author the short id (for example `"assembler": "htmlAssembler"`); package build stages
the registered extension path into the archive `TemplateDef-N/<stem>.templateDef`
`<assembler>` element. Values that are already full extension paths pass through
unchanged for site-local custom assemblers.

## Assembler-model readback from the manifest

Tooling reads a normalized assembler-model definition back through the modern
manifest path (`Java code: PSPageXmlManifestReadback`) instead of legacy definition
XML: given `pages/<id>/component-package.json` plus its template sources, readback
rebuilds the same definition the legacy `<assembly-template>` parser would produce —
assembler extension path, output format, ordered JEXL bindings, region holes, and the
install defaults (`text/html`, `UTF-8`, `Default`, `Shared`, `Normal`). Binary asset
templates omit mime type and charset, matching install emit.

Readback never touches Widget / Page definition XML, and the customer legacy-XML
fallback stays in place (removal is tracked separately). Operators do not need to do
anything: this is the inspection path package build and upgrade tooling use to verify
a modern package before it is staged into `TemplateDef-N/`.

## Legacy definition-XML migration dry-run report

Upgrade tooling previews a legacy definition-XML migration with
`Java code: PSPageXmlMigrationDryRun` before anything is converted. The dry-run
simulates parse → compile → manifest readback → install-XML parity for every
`*.templateDef` file and renders a per-package status panel — what would change
(would-be manifest id, template type, assembler, slot / binding counts) and what
blocks (parse, compile, validation, or parity reason). **It writes nothing**: compile
results live only in memory and `writeArtifacts` is never called.

| Status | Meaning | Operator action |
|--------|---------|-----------------|
| `READY` | Parses, compiles, validates, and round-trips through the manifest path | Safe to convert with the normal package-build path |
| `BLOCKED` | Migration cannot proceed; the panel names the reason | Fix the named blocker (malformed XML, unknown assembler, parity mismatch), then re-run |

The panel renders as Markdown (`Report.toMarkdown()`, one row per package) for
issue evidence and operator tooling, or plain text (`Report.toText()`) for CLI use.
The legacy fallback is untouched by the dry-run; fallback-path removal is tracked
separately.

## Dual-ship vs dual-run (do not mix them up)

| Concept | Layer | Product 8.2 status |
|---------|-------|--------------------|
| **Dual-ship** page `*.templateDef` | Package **build** | Retired as the product ship path. Product packages use native install. |
| **Dual-run** definition XML shim | **Runtime** selection | Still required so customer Widget / Page / Gadget XML loads when no modern package is present. |

Native page package install is **not** the same as removing the runtime legacy-definition
XML shim. Customer XML fallback stays until remaining definitions have modern packages
and engineering removal criteria pass. Operators convert customer XML with
[Convert definition XML (dual-run)](id:admin-definition-xml-dual-run). Do **not** delete
or hard-disable the dual-run shim as an upgrade cleanup step.

## Related

- [Convert definition XML (dual-run)](id:admin-definition-xml-dual-run)
- [Extensions & packages](id:developer-extensions)
- [Design templates](id:admin-design-templates)
- [Installation Overview](id:install-overview)
- [Upgrade Overview](id:upgrade-overview)
