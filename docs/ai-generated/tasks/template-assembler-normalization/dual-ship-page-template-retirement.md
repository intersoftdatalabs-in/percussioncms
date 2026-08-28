# Dual-ship page templateDef retirement checklist

| Field | Value |
|-------|--------|
| **Status** | **Retired as product ship path** (native-only product page packages). `PSPageXmlDualShip` **code still present** — do not claim DualShip deleted. |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Related** | #2786 dual-ship modern authoring · #2806 native package install · #3673 Baseline native · #3674 leftover binary conversion · #3675 CI dual-ship gate · #3737 perc.Test page dual-ship exit · #3949 native default · #3950 DualShip removed from package-build · product-docs #3951 · Phase 5 shim #2632 / #2852 |
| **Code** | `PSPageXmlInstallPolicy`, `PSPageXmlNativeInstall`, `PSPageXmlDualShip`, `PSPackageBuilder` |
| **Product help** | [Product page packages](../../../../product-docs/8.2/developer/page-packages.md) (`id: developer-page-packages`) |

## Purpose

Product page layout packages author modern `pages/<id>/component-package.json` + template sources (ADR-004). Until #2806, package build **dual-shipped** root `*.templateDef` so deployer `TemplateDef` install was unchanged. Native install stages the same assembly-template XML **directly into** archive `TemplateDef-N/` folders without dual-ship root materialization.

This document is the **operator / engineering checklist** for dual-ship vs native page install.

**Product ship path (8.2):** converted product page packages use **native** only. Dual-ship is **not** how product packages ship. `PSPageXmlDualShip` **code still present** (CLI/tests). Package-build no longer calls `materializeInstallTemplateDefs` (#3950); dual-ship mode fails closed. Follow-up class deletion is a separate residual — do **not** invent that `PSPageXmlDualShip` was removed.

## Install modes

| Mode | When | Package-build behavior |
|------|------|------------------------|
| **native** (default, #3949; product ship path #3951) | No sysprop / no package-local `page.installMode`, or converted product packages set `page.installMode=native` | Skip root dual-ship; after reorganize, `PSPageXmlNativeInstall.stageArchiveTemplateDefs` writes `TemplateDef-N/<stem>.templateDef` |
| **dual-ship** (opt-in) | `package-install.properties` `page.installMode=dual-ship`, or sysprop `perc.packages.page.installMode=dual-ship` | Fail closed in `PSPackageBuilder` — does **not** materialize root `*.templateDef` (#3950). Native archive staging is the only production emit. **Not** used by product page packages on `main`. |

### Configuration knobs

| Knob | Values | Notes |
|------|--------|-------|
| Package-local `package-install.properties` → `page.installMode` | `native` \| `dual-ship` | Dual-ship is explicit opt-in; native is the default when unset (#3949) |
| System property `perc.packages.page.installMode` | `native` \| `dual-ship` | Overrides package-local (CI / one-off builds) |
| System property `perc.packages.dualShip.pageTemplateDefs` | `false` / `0` / `off` | Forces native (dual-ship generation off) |

Policy code: `com.percussion.packages.pagexml.PSPageXmlInstallPolicy` (aligns with modern-preferred selection in `PSLegacyDefinitionXmlShim`).

## Packages already on native install

| Package | Mode file | Notes |
|---------|-----------|-------|
| `perc.baseTemplates` | `package-install.properties` → `native` | 20 page layouts |
| `perc.responsiveTemplates` | `package-install.properties` → `native` | Banded / Basic / plain |
| `perc.Baseline` | `package-install.properties` → `native` | 7 system templates (`perc.page`, `perc.pageDatabase`, `perc.pageDispatcher`, `perc.pageXml`, `perc.sys.resource`, `perc.widget`, `perc.widgetDispatcher`) — #3673 |
| `perc.FileAssetWidget` / `perc.widgets.image` | `package-install.properties` → `native` | Leftover binary TemplateDefs (`perc.fileBinary`, `perc.imageMainBinary`, `perc.imageThumbBinary`) converted to modern `pages/` — #3674 / #3680 |
| `perc.Test` | n/a (no `pages/`) | Never authored page `*.templateDef` / `pages/`; dual-ship and Page G4 waive lists emptied — #3737 |

## Retirement checklist (per package)

1. **Author modern** — `pages/<id>/component-package.json` + `templates/*.vm` present; no product-authored root `*.templateDef`.
2. **Mapping intact** — `*.mapping.properties` still lists `stem.templateDef=TemplateDef-N` (and ACL side-cars as needed).
3. **Native is default** — unconfigured packages resolve native (#3949). Keep or add package-root `package-install.properties` with `page.installMode=native` for converted product packages; dual-ship requires an explicit `dual-ship` value.
4. **Parity test** — unit test or golden: native archive XML GUID / name / assembler / body match dual-ship emit (`PSPageXmlNativeInstallTest` pattern).
5. **Package build** — `PSPackageBuilder` log line `native-install page TemplateDefs for <pkg>: N written`.
6. **Install smoke** — deployer installs package; templates load with stable GUIDs (human QA when host install available).
7. **Remove dual-ship dependency** — package no longer needs root materialization; keep mapping + ACLs.

## Global dual-ship exit criteria

Dual-ship **code path** can be deleted when **all** hold:

- [x] Native install API + policy exist (`PSPageXmlNativeInstall` / `PSPageXmlInstallPolicy`) — #2806
- [x] `perc.baseTemplates` / `perc.responsiveTemplates` use native mode — #2806
- [x] Remaining page layout packages on native (`perc.Baseline` system templates — #3673)
- [x] Widget leftover binary TemplateDefs inventoried and converted (#3674 / #3680): `perc.fileBinary`, `perc.imageMainBinary`, `perc.imageThumbBinary` are `output-format=Binary` / `binaryAssembler` (not page layouts). Converted to `pages/<id>/component-package.json` + native install (Widget XML emitter cannot emit TemplateDef). Product tree has zero authored root `*.templateDef`.
- [x] CI / product package build has zero `dual-ship page templateDefs` log lines — Surefire `PSDualShipPageTemplateDefInventory` / #3675. Waiver is **empty** after perc.Test page dual-ship exit (#3737). Leftover binaries are converted on `main` via #3680 (not dual-ship-retained). Package-build also fails closed via `assertDualShipMaterializationAllowed`.
- [x] Docs (this file + [page inventory](./page-definition-inventory.md) + [ADR-004](./adr/004-no-definition-xml-packaging.md) + product-docs `id: developer-page-packages`) mark dual-ship **retired as the product ship path** (#3951)
- [x] Follow-up removes `PSPageXmlDualShip.materializeInstallTemplateDefs` call sites when unused — package-build no longer calls it (#3950). DualShip CLI/tests still exercise the helper. Policy default flip is a sibling slice.

### CI gate (#3675)

| Piece | Class |
|-------|--------|
| Inventory + log parser + CLI | `com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory` |
| Surefire | `PSDualShipPageTemplateDefInventoryTest` |
| Package-build fail-closed | `PSPackageBuilder.stageModernPageInstallArtifacts` (native only; dual-ship throws, #3950) |

Scan is **committed** `package-install.properties` only (JVM `perc.packages.page.installMode` must not hide an explicit dual-ship opt-in, and must not make unconfigured packages look dual-ship). Packages with modern `pages/` + native (default or explicit) are not dual-ship emitters. Authored root `*.templateDef` without modern `pages/` is out of this gate; leftover binaries were converted (#3674 / #3680). Waiver set is **empty** after perc.Test page dual-ship exit (#3737). Native default: #3949.

## Deployer note

Runtime install still uses `PSTemplateDefDependencyHandler` and assembly-template XML **inside** the `.ppkg`. Native install does **not** require deployer to parse `component-package.json` at runtime yet; it makes package **build** the consumer of modern packages (shim policy: modern preferred). A future residual may teach deployer to load modern manifests directly from archive user-deps; until then, archive `TemplateDef-N/` XML remains the wire format.

## Dual-run / dual-ship relationship

| Concept | Layer | Status |
|---------|-------|--------|
| Dual-run **definition XML shim** | Runtime selection modern vs Widget/Page/Gadget XML | Time-boxed; Phase 5 #2632 — criteria: [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md). **Shim kept** (#2852 blocked). |
| Dual-ship **page templateDef** | Package-build install bridge for page layouts | **Retired as product ship path**; DualShip code still present |
| Native **page install** | Package-build stages TemplateDef archive from modern pages | Product path for base/responsive (#2806), Baseline (#3673), leftover binaries (#3674 / #3680) |

## See also

- [page-definition-inventory.md](./page-definition-inventory.md)
- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)
- [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
