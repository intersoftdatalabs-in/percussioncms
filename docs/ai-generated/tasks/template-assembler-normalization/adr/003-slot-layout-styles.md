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

## Rationale

- Layout chrome applies to any content in a hole, not only “widgets.”
- Unifies classic slots and CM1 regions.
- 34/48 product widgets already define `CssPref`s (often `rootclass`) — natural upgrade source.

## Consequences

- Schema/persistence changes for slots (`SLOT_LAYOUT` / `SLOT_STYLES` columns).
- Upgrade maps widget `CssPref` / layout-ish `UserPref` → slot defaults + instance overrides (residual after first slice).
- Visual Design editor edits these properties on slots (later phases).
- Do not invent an open-ended CSS-in-DB product in v1 — start with CM1-parity property set and version the schema.
