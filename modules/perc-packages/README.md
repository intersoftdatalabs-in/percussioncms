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

## G4 Widget definition XML inventory gate (#3026)

Product packages must not reintroduce committed install Widget definition XML under
`sys__UserDependency--rxconfig/Widgets/` except the explicit waiver **`perc.Test`**.

| Piece | Class |
|-------|--------|
| Inventory API + CLI | `com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory` |
| Surefire assertion | `PSWidgetDefinitionXmlInventoryTest` |

See root `scripts/README.md` (Widget definition XML inventory gate) and
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

