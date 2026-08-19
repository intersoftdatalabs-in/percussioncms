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
for Pages/Gadgets) except the explicit waiver **`perc.Test`**.

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

