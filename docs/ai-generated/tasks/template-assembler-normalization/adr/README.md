# Architecture decision records

| ADR | Title | Status |
|-----|-------|--------|
| [001](./001-jexl-bindings-stay.md) | Keep JEXL for template bindings | Accepted |
| [002](./002-assembler-set.md) | Assembler set (Velocity, HTML-first, Markdown, …) | Accepted; `${path}` placeholder syntax locked |
| [003](./003-slot-layout-styles.md) | Slot layout and slot styles | Accepted (direction) |
| [004](./004-no-definition-xml-packaging.md) | No Page/Widget/Gadget XML for product packaging | Accepted |

Related schema (not a separate ADR):

- [Component Package Manifest v1.0](../component-package-manifest.md) — ship format + Java model (Phase 3 / #2750); grounded in ADR-004

Open follow-ups:

- Exact assembly context keys for slot layout/styles (ADR-003)
- Compiler (#2751) and runtime shim (#2752) consumption of the manifest
