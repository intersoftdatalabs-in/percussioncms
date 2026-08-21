# Dual-ship page templateDef retirement checklist

| Field | Value |
|-------|--------|
| **Status** | Active (#2806 native install path landed; dual-ship optional) |
| **Parent** | [#2630](https://github.com/intersoftdatalabs-in/percussioncms/issues/2630) · Grandparent [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Related** | #2786 dual-ship modern authoring · #2806 native package install · Phase 5 shim #2632 |
| **Code** | `PSPageXmlInstallPolicy`, `PSPageXmlNativeInstall`, `PSPageXmlDualShip`, `PSPackageBuilder` |

## Purpose

Product page layout packages author modern `pages/<id>/component-package.json` + template sources (ADR-004). Until #2806, package build **dual-shipped** root `*.templateDef` so deployer `TemplateDef` install was unchanged. Native install stages the same assembly-template XML **directly into** archive `TemplateDef-N/` folders without dual-ship root materialization.

This document is the **operator / engineering checklist** for turning dual-ship off package-by-package and eventually removing the dual-ship code path.

## Install modes

| Mode | When | Package-build behavior |
|------|------|------------------------|
| **dual-ship** (default) | No package-local opt-in; system prop unset | Materialize root `*.templateDef` → reorganize maps to `TemplateDef-N/` |
| **native** | `package-install.properties` `page.installMode=native`, or sys props below | Skip root dual-ship; after reorganize, `PSPageXmlNativeInstall.stageArchiveTemplateDefs` writes `TemplateDef-N/<stem>.templateDef` |

### Configuration knobs

| Knob | Values | Notes |
|------|--------|-------|
| Package-local `package-install.properties` → `page.installMode` | `native` \| `dual-ship` | Preferred for converted product packages |
| System property `perc.packages.page.installMode` | `native` \| `dual-ship` | Overrides package-local (CI / one-off builds) |
| System property `perc.packages.dualShip.pageTemplateDefs` | `false` / `0` / `off` | Forces native (dual-ship generation off) |

Policy code: `com.percussion.packages.pagexml.PSPageXmlInstallPolicy` (aligns with modern-preferred selection in `PSLegacyDefinitionXmlShim`).

## Packages already on native install

| Package | Mode file | Notes |
|---------|-----------|-------|
| `perc.baseTemplates` | `package-install.properties` → `native` | 20 page layouts |
| `perc.responsiveTemplates` | `package-install.properties` → `native` | Banded / Basic / plain |
| `perc.Baseline` | `package-install.properties` → `native` | 7 system templates (`perc.page`, `perc.pageDatabase`, `perc.pageDispatcher`, `perc.pageXml`, `perc.sys.resource`, `perc.widget`, `perc.widgetDispatcher`) — #3673 |

## Retirement checklist (per package)

1. **Author modern** — `pages/<id>/component-package.json` + `templates/*.vm` present; no product-authored root `*.templateDef`.
2. **Mapping intact** — `*.mapping.properties` still lists `stem.templateDef=TemplateDef-N` (and ACL side-cars as needed).
3. **Opt into native** — add package-root `package-install.properties` with `page.installMode=native`.
4. **Parity test** — unit test or golden: native archive XML GUID / name / assembler / body match dual-ship emit (`PSPageXmlNativeInstallTest` pattern).
5. **Package build** — `PSPackageBuilder` log line `native-install page TemplateDefs for <pkg>: N written`.
6. **Install smoke** — deployer installs package; templates load with stable GUIDs (human QA when host install available).
7. **Remove dual-ship dependency** — package no longer needs root materialization; keep mapping + ACLs.

## Global dual-ship exit criteria

Dual-ship **code path** can be deleted when **all** hold:

- [x] Native install API + policy exist (`PSPageXmlNativeInstall` / `PSPageXmlInstallPolicy`) — #2806
- [x] `perc.baseTemplates` / `perc.responsiveTemplates` use native mode — #2806
- [x] Remaining page layout packages on native (`perc.Baseline` system templates — #3673)
- [ ] Widget packages that still dual-ship any page-like templateDefs inventoried and converted or explicitly dual-ship-retained
- [ ] CI / product package build has zero `dual-ship page templateDefs` log lines (or only waived packages)
- [ ] Docs (this file + page inventory + ADR-004) mark dual-ship retired
- [ ] Follow-up removes `PSPageXmlDualShip.materializeInstallTemplateDefs` call sites when unused

## Deployer note

Runtime install still uses `PSTemplateDefDependencyHandler` and assembly-template XML **inside** the `.ppkg`. Native install does **not** require deployer to parse `component-package.json` at runtime yet; it makes package **build** the consumer of modern packages (shim policy: modern preferred). A future residual may teach deployer to load modern manifests directly from archive user-deps; until then, archive `TemplateDef-N/` XML remains the wire format.

## Dual-run / dual-ship relationship

| Concept | Layer | Status |
|---------|-------|--------|
| Dual-run **definition XML shim** | Runtime selection modern vs Widget/Page/Gadget XML | Time-boxed; Phase 5 #2632 — criteria: [definition-xml-shim-removal-criteria.md](./definition-xml-shim-removal-criteria.md) |
| Dual-ship **page templateDef** | Package-build install bridge for page layouts | Optional; native preferred for converted packages |
| Native **page install** | Package-build stages TemplateDef archive from modern pages | Landed #2806 for base/responsive; #3673 for Baseline |

## See also

- [page-definition-inventory.md](./page-definition-inventory.md)
- [dual-run-legacy-definition-xml-shim.md](./dual-run-legacy-definition-xml-shim.md)
- [adr/004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md)
