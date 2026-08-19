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

## M2 product/H2 zero-legacy-selection evidence (#3583)

Non-waived product / H2 widget package roots must select modern-first
(`wouldUseLegacyShim == false` / `MODERN_COMPONENT_PACKAGE`). Unexpected
`LEGACY_*` on a non-waived product widget fails Surefire. Waiver: **`perc.Test`**
only. The runtime shim stays (#2852); this is **not** M2 PASS overall (M3 still
FAIL).

| Piece | Class |
|-------|--------|
| Evidence API + CLI | `com.percussion.packages.shim.PSProductPackageRootSelectionEvidence` |
| Surefire assertion | `PSProductPackageRootSelectionEvidenceTest` |
| DAO H2 harness | `projects/sitemanage` `PSWidgetDaoProductH2ZeroLegacySelectionTest` |

