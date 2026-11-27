---
id: developer-page-package-pilot
title: Pilot product package walkthrough
description: Convert a product package to manifest-native authoring using perc.FileAssetWidget as the worked example
version: "8.2"
order: 56
tags: [developer, packages, templates, install]
---

# Pilot product package walkthrough

This is the worked example operators copy when converting the next product
package to manifest-native authoring. The pilot is `perc.FileAssetWidget`:
one binary page template (`perc.fileBinary`, `TemplateDef-607`) plus one
widget (`percFile`), with zero Page/Widget definition XML on its path.

The executable version of this checklist is
`Java code: PSPilotProductPackageRoundTripTest` (`modules/perc-packages`):
every step below has a test proving it against the real pilot package
directory. If a step fails on the next package, fix the package — do not
weaken the step.

## Prerequisites

- The package keeps its deployer identity: mapping entries
  (`<stem>.templateDef=TemplateDef-N`) stay stable so existing customer rows
  are never remapped. The pilot mapping is
  `perc.FileAssetWidget.mapping.properties`
  (`perc.fileBinary.templateDef=TemplateDef-607`).
- Widget content types (`*.contentType.*`, `*.aclDef`) and web resources stay
  where they are. This walkthrough converts **definition** XML only.

## 1. Author the manifest + native sources

Each page lives under `pages/<id>/`:

- `pages/<id>/component-package.json` — one entry in `templates[]`
  (the render path consumes the first entry only), ordered JEXL `bindings[]`,
  and `slots[]` when the template has region holes. The pilot page declares
  six bindings in evaluation order and no slots (binary template).
- `pages/<id>/templates/<id>.vm` — the Velocity source.

Each widget lives under `widgets/<id>/` with the same manifest shape
(`templates[]` snippet, `bindings[]`, `slots[]`, `resources[]`).

Set native install at the package root (`package-install.properties`):

```properties
page.installMode=native
```

Native mode skips dual-ship root `*.templateDef` generation and stages
archive `TemplateDef-N/<stem>.templateDef` from the modern sources at
package-build time. Packages without the key resolve to native by policy;
dual-ship is explicit opt-in only.

## 2. Prove native install

Stage the archive folders from the modern sources
(`Java code: PSPageXmlNativeInstall`):

1. `listInstallArtifacts(packageDir)` returns one artifact per page —
   the pilot returns exactly one (`perc.fileBinary` → `TemplateDef-607`).
2. `stageArchiveTemplateDefs(packageDir, archiveRoot)` writes
   `TemplateDef-607/perc.fileBinary.templateDef` and nothing beside the
   modern sources (no dual-ship root file).
3. The staged XML parses with the stable GUID (`0-4-607`), the registered
   assembler path, and a body identical to the authored `.vm` source.

## 3. Prove readback

Read the normalized assembler-model definition back through the manifest
path (`Java code: PSPageXmlManifestReadback`):

- Assembler resolves through the registered catalog
  (`Java/global/percussion/assembly/binaryAssembler` for the pilot —
  unregistered ids fail fast instead of silently defaulting).
- Ordered JEXL bindings survive in document order
  (`$sys.binary` first for the pilot, six total).
- Binary templates omit mime type and charset, matching install emit.

## 4. Prove dry-run READY

Feed the staged install XML to the migration dry-run
(`Java code: PSPageXmlMigrationDryRun`), which simulates
parse → compile → manifest readback → install-XML parity **without writing
anything**:

- One file in, one `READY` entry out, zero `BLOCKED`, clean report.
- The entry names the would-be manifest id (`perc.fileBinary`), the
  manifest template type (`binary`), the short assembler id
  (`binaryAssembler`), and the binding count (6).

A `BLOCKED` entry names the blocker (malformed XML, unregistered
assembler, parity mismatch) — fix it in the authored sources, then re-run.

## 5. Recount M1 on the pilot path

Zero Page/Widget definition XML must remain under the package:

- No committed install Widget XML beside `widgets/`
  (`Java code: PSWidgetXmlInstallEmitter.hasCommittedWidgetXml`).
- No root `*.templateDef` files (mapping-only `*.aclDef` files are fine).
- The ship-path inventories scan clean:
  `Java code: PSWidgetDefinitionXmlInventory`,
  `Java code: PSPageDefinitionXmlInventory`,
  `Java code: PSGadgetDefinitionXmlInventory`,
  `Java code: PSDefinitionXmlShipPathInventory`.

## 6. Prove the widget half

Compile the modern widget and materialize the install wire format on a
**staging copy** (never the source tree):

1. `Java code: PSWidgetXmlPackageCompiler.compilePackage` returns one
   result (`percFile`) whose manifest validates
   (`Java code: PSComponentPackageManifestValidator`).
2. `Java code: PSWidgetXmlInstallEmitter.materializeInstallWidgetXml`
   writes exactly one `Widgets/percFile.xml` on the staging copy; the
   emitted XML parses back to the same stem.

## 7. Run the conformance gates

- **Binding order (next-a):** the pilot page bindings are conformant —
  ordered, unique, no blanks
  (`Java code: PSPageXmlBindingConformance`). Widget snippets that reassign
  JEXL variables across conditional branches (`$target`, `$rootclass`,
  `$link` in the pilot) intentionally shadow: the gate surfaces the
  duplicates in document order instead of sorting them away.
- **Slot `slot_layout` / `slot_styles` (next-b):** the pilot slot
  (`percFileContent`) is conformant — empty `layout` is informational,
  every `styles` value is scalar
  (`Java code: PSSlotLayoutStylesConformance`).

## Related

- [Product page packages](id:developer-page-packages)
- [Convert definition XML (dual-run)](id:admin-definition-xml-dual-run)
- [Design templates](id:admin-design-templates)
- [Extensions & packages](id:developer-extensions)
