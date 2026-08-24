# perc-packages

This module builds product component packages (`.ppkg`) from `src/main/resources/Packages`
and hosts Widget/Page/Gadget compilers, dual-ship helpers, and the legacy definition-XML
selection shim.

## Building

From this module directory (standalone, preferred):

```bat
..\..\mvnw.cmd clean install
```

```bash
../../mvnw clean install
```

## G4 definition XML inventory gates (#3026 Widget, #3581 Pages/Gadgets)

Product packages must not reintroduce committed install definition XML under
`sys__UserDependency--rxconfig/{Widgets,Pages,Gadgets}/` (or `rxconfig/{Pages,Gadgets}/`
for Pages/Gadgets). **Widget** waiver remains **`perc.Test` only** (widget ship-exit is
#3736). **Pages/Gadgets** waiver is **empty** after perc.Test page dual-ship exit (#3737).

| Piece | Class |
|-------|--------|
| Widget inventory API + CLI | `com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory` |
| Widget Surefire | `PSWidgetDefinitionXmlInventoryTest` |
| Shared Page/Gadget scanner | `com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory` |
| Page inventory API + CLI | `com.percussion.packages.pagexml.PSPageDefinitionXmlInventory` |
| Page Surefire | `PSPageDefinitionXmlInventoryTest` |
| Gadget inventory API + CLI | `com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory` |
| Gadget Surefire | `PSGadgetDefinitionXmlInventoryTest` |

Modern authoring (`pages/`, `widgets/`, `gadget-catalog.json`) is not definition XML.
Do not delete `PSLegacyDefinitionXmlShim` (#2852 blocked).

See root `scripts/README.md` (definition XML inventory gates) and
`docs/ai-generated/tasks/template-assembler-normalization/definition-xml-shim-removal-criteria.md`.

## Dual-ship page templateDef inventory gate (#3675)

Product packages with modern `pages/` must not re-introduce dual-ship root `*.templateDef`
materialization. Waiver is **empty** after perc.Test page dual-ship exit (#3737)
(`perc.Test` never authored `pages/` / page `*.templateDef`). Native packages
(`package-install.properties` `page.installMode=native`) are not dual-ship emitters.
#3674 leftover widget binaries are **not** dual-ship-retained (empty retain list).

| Piece | Class |
|-------|--------|
| Inventory + log parser + CLI | `com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory` |
| Surefire | `PSDualShipPageTemplateDefInventoryTest` |
| Package-build fail-closed | `PSPackageBuilder` |

Committed policy ignores JVM `perc.packages.page.installMode` so CI reflects the source tree.

## Archive-manifest Widget XML paths (#3582)

Non-waived product `psx_archiveInfo.xml` / `psx_archiveManifest.xml` must not author
`rxconfig/Widgets/*.xml` (or encoded `rxconfig_Widgets_`) when modern `widgets/` roots exist.
Waiver is **`perc.Test` only**. Package build re-injects those user-dependencies on the staging
copy via `PSWidgetXmlInstallEmitter` / `PSWidgetArchiveManifestInventory` so the built `.ppkg`
still installs Widget XML.

| Piece | Class |
|-------|--------|
| Inventory + strip + install inject | `com.percussion.packages.widgetxml.PSWidgetArchiveManifestInventory` |
| Surefire assertion | `PSWidgetArchiveManifestInventoryTest` |

## M2 product/H2 zero-legacy-selection evidence (#3583 / #3738)

Non-waived product / H2 widget package roots must select modern-first
(`wouldUseLegacyShim == false` / `MODERN_COMPONENT_PACKAGE`). Unexpected
`LEGACY_*` on a non-waived product widget fails Surefire. Waiver: **empty or
`perc.Test` only** (`assertWidgetWaiverPolicy`). While `perc.Test` remains on
the list it may still select `LEGACY_WIDGET_XML`; after #3736 it must be
modern-first. The runtime shim stays (#2852); this is **not** M2 PASS overall
(M3 still FAIL).

| Piece | Class |
|-------|--------|
| Evidence API + CLI | `com.percussion.packages.shim.PSProductPackageRootSelectionEvidence` |
| Surefire assertion | `PSProductPackageRootSelectionEvidenceTest` |
| DAO H2 harness | `projects/sitemanage` `PSWidgetDaoProductH2ZeroLegacySelectionTest` |

