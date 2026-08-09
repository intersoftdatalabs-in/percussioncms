# Architecture decision records

| ADR | Title | Status |
|-----|-------|--------|
| [001](./001-jexl-bindings-stay.md) | Keep JEXL for template bindings | Accepted |
| [002](./002-assembler-set.md) | Assembler set (Velocity, HTML-first, Markdown, …) | Accepted; `${path}` placeholder syntax locked |
| [003](./003-slot-layout-styles.md) | Slot layout and slot styles | Accepted (direction) |
| [004](./004-no-definition-xml-packaging.md) | No Page/Widget/Gadget XML for product packaging | Accepted |

Open follow-ups before Phase 1 coding freezes:

- HTML-first placeholder syntax choice (ADR-002)
- Exact assembly context keys for slot layout/styles (ADR-003)
- Component package manifest format (new ADR when drafted)
