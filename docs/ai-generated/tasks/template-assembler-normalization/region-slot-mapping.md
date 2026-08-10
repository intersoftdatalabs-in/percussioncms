# CM1 region ↔ unified slot composition mapping

| Field | Value |
|-------|--------|
| **Status** | Phase 2 residual (#2690) |
| **Parent** | #2629 · Grandparent epic #2626 |
| **Code** | `projects/sitemanage/.../mapper/PSRegionToSlotCompositionMapper` |
| **CssPref upgrade** | `.../mapper/PSWidgetPrefToSlotMapper` |
| **Schema** | ADR-003 · `PSSlotLayoutStyles` (system) |

## Goals of this residual

1. Document and unit-test **region tree → slot composition** mapping rules.
2. Sketch **CssPref / layout-ish UserPref → `slot_styles` / `slot_layout`** upgrade (definition defaults + instance overrides).
3. List **package upgrade residual steps** for Phase 3 (#2630) — inventory only here; no bulk package rewrite.

Out of scope: REST/Workbench fields (#2691), product Widget XML elimination (#2630), Design SPA (#2631).

## Region ↔ slot mapping rules

| CM1 concept | Unified slot composition |
|-------------|--------------------------|
| `PSRegion.regionId` | `PSSlotCompositionNode.slotName` (identity in v1 sketch) |
| Nested `PSRegion` children | Nested `PSSlotCompositionNode` children |
| `PSRegionCode` (markup) | Not a slot — stays template body between holes |
| `PSRegion.cssClass` | Seeds `slot_styles.rootclass` on the composition node |
| `PSRegionWidgets` / widget items | Ordered `PSSlotCompositionItem` list on that slot |
| Widget instance `cssProperties` | Instance `styleOverrides` |
| Layout-ish widget `properties` | Instance `layoutOverrides` |

### Runtime sketch

```text
PSRegionTree
  rootRegion ──► PSSlotCompositionNode (slotName = regionId)
       │              ├── slot_styles (region cssClass → rootclass)
       │              ├── items[]  (widgets on this region)
       │              └── children[] (child regions only)
```

Call entry points (offline / upgrade tools):

- `PSRegionToSlotCompositionMapper.map(PSRegionTree)`
- `PSRegionToSlotCompositionMapper.map(PSRegionTree, Map<String,PSWidgetDefinition>)`
- `PSRegionToSlotCompositionMapper.regionIdToSlotNameMap(root)` — flat id map for tooling

## CssPref / UserPref → slot_layout / slot_styles

| Source | Target map | Canonical keys |
|--------|------------|----------------|
| `CssPref` (any name) | `slot_styles` | `rootclass`, `itemclass`, plus custom names preserved |
| UserPref `rootclass` / `itemclass` (legacy) | `slot_styles` | same |
| UserPref `layout` / `orientation` | `slot_layout` | `orientation` (`ui-perc-list-horizontal` → `horizontal`) |
| UserPref `maxlength` / `max_results` / `maxItems` | `slot_layout` | `maxItems` |
| UserPref `columns`, `emptyState`, `wrapperClassPolicy` | `slot_layout` | matching ADR-003 keys |
| Other UserPrefs (e.g. `target`, content filters) | **not** mapped | remain content-type / snippet properties |

### Definition vs instance

| Layer | Method | Role |
|-------|--------|------|
| Slot **definition** defaults | `definitionStyleDefaults` / `definitionLayoutDefaults` | From widget XML prefs → future component slot catalog |
| Slot **instance** overrides | `instanceStyleOverrides` / `instanceLayoutOverrides` | From placed `PSWidgetItem` maps |
| Merge | `merge(base, overrides, layout?)` | Instance wins; schema version stamped |

Persistence of maps on classic slots is already in `RXSLOTTYPE.SLOT_LAYOUT` / `SLOT_STYLES` (#2692). Composition-level instance storage / REST is #2691.

## Residual package upgrade steps (Phase 3 inventory)

These steps are **not** automated in #2690. Product packages under `modules/perc-packages/.../Widgets/`:

1. **Inventory** — use `widget-xml-inventory.md` / `.csv`; filter widgets with `CssPref` or layout-ish `UserPref` (`layout`, `maxlength`, `rootclass`, …).
2. **Slot catalog defaults** — for each converted component, write definition defaults via `PSWidgetPrefToSlotMapper.definition*Defaults` into the new package slot metadata (or `PSTemplateSlot` when classic slots are generated).
3. **Page/template composition** — for each CM1 template/page region tree, run `PSRegionToSlotCompositionMapper.map` to produce slot composition + per-item overrides; persist when Phase 3 storage exists.
4. **Velocity/JEXL consumers** — migrate `$perc.widget.item.cssProperties.get('rootclass')` reads toward `$sys.slot.styles.rootclass` (binding names locked in ADR-003 / #2692).
5. **Leave content UserPrefs** on the snippet/content type until content model migration defines their home.
6. **Do not delete** Widget XML in this residual; elimination is #2630 after upgrade tooling exists.

### Example (product pattern)

`percRichText` / `percSimpleText`: single `CssPref name="rootclass"` → slot definition styles `{ schemaVersion: 1, rootclass: <default if any> }`; page instance values from Design CSS dialog → item `styleOverrides.rootclass`.

`percArchiveList`: `UserPref layout` + `CssPref rootclass` / `summaryclass` → layout `orientation` + styles map including custom `summaryclass`.

## Tests

Offline (no Spring / no DB):

- `PSWidgetPrefToSlotMapperTest`
- `PSRegionToSlotCompositionMapperTest`

Module: `cd projects/sitemanage && ../../mvnw clean install`

## Related

- [ADR-003](./adr/003-slot-layout-styles.md)
- [parity-notes.md](./parity-notes.md) § Region vs slot
- [plan.md](./plan.md) §3.3
- Residual REST/Workbench: #2691
