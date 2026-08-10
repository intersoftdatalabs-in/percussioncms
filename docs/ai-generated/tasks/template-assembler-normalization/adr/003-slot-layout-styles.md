# ADR-003: Slot layout and slot styles

| Field | Value |
|-------|--------|
| **Status** | Accepted (direction) |
| **Date** | 2026-08-09 |

## Decision

Promote CM1 widget layout/style preferences to **first-class slot properties**:

- **`slot_layout`** — structural hints (orientation, columns, max items, wrapper policy, empty state, responsive breakpoints).
- **`slot_styles`** — presentational tokens/classes (root class, item class, theme hooks).

Stored as structured maps (JSON) on:

1. Slot **definition** (defaults)
2. Slot **instance** on a page/template composition (overrides)

Exposed to assembly context for JEXL/templates. **Binding names (locked for Phase 2):**

| Binding | Meaning |
|---------|---------|
| `$sys.slot.layout` | Structural layout map for the current slot |
| `$sys.slot.styles` | Presentational styles map for the current slot |
| `$sys.slot.schemaVersion` | Integer schema version |
| `$sys.currentslot.layout` / `.styles` | Mirrored on Velocity AA slot context |
| `$rx.asmhelper.slotAssemblyContext(slot)` | Builds the `$sys.slot` map from a definition |

Schema helper: `PSSlotLayoutStyles` (`SCHEMA_VERSION = 1`). Persistence: `RXSLOTTYPE.SLOT_LAYOUT` / `SLOT_STYLES` CLOB JSON on `PSTemplateSlot`.

**REST (Developer module / #2691):** slot definition maps are exposed as `slotLayout` / `slotStyles` on `SlotDetail` (`GET/PUT /slots/{idOrName}`). Non-null maps on PUT replace definition values (empty / schema-only clears to defaults).

**Instance overrides:** composition-level sparse maps merge over definition defaults via `PSSlotLayoutStyles.merge(base, overrides, layout)` (instance keys win). `clearOverride` removes a key from the override map so the definition value applies again. `toAssemblyContext(slot, layoutOverrides, stylesOverrides)` builds the effective `$sys.slot` binding. Sparse override JSON: `encodeOverrides` / `parseOverrides`.

## Rationale

- Layout chrome applies to any content in a hole, not only “widgets.”
- Unifies classic slots and CM1 regions.
- 34/48 product widgets already define `CssPref`s (often `rootclass`) — natural upgrade source.

## Consequences

- Schema/persistence changes for slots (`SLOT_LAYOUT` / `SLOT_STYLES` columns).
- Upgrade maps widget `CssPref` / layout-ish `UserPref` → slot defaults + instance overrides — implemented as offline mappers in #2690 (`PSWidgetPrefToSlotMapper`, `PSRegionToSlotCompositionMapper`; see [region-slot-mapping.md](../region-slot-mapping.md)).
- Visual Design editor edits these properties on slots (later phases).
- Do not invent an open-ended CSS-in-DB product in v1 — start with CM1-parity property set and version the schema.
