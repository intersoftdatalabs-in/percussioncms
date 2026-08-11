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

