# Architecture decision records

| ADR | Title | Status |
|-----|-------|--------|
| [001](./001-jexl-bindings-stay.md) | Keep JEXL for template bindings | Accepted |
| [002](./002-assembler-set.md) | Assembler set (Velocity, HTML-first, Markdown, …) | Accepted; `${path}` placeholder syntax locked |
| [003](./003-slot-layout-styles.md) | Slot layout and slot styles | Accepted (direction) |
| [004](./004-no-definition-xml-packaging.md) | No Page/Widget/Gadget XML for product packaging | Accepted |

Related schema (not a separate ADR):

- [Component Package Manifest v1.0](../component-package-manifest.md) — ship format + Java model (Phase 3 / #2750); grounded in ADR-004

**Implementer entry point (Phase 5):** [Assemblers and Templates implementer guide](../implementer-guide.md) — consolidates ADRs 001–004, bindings, package format, dual-run/dual-ship status.

Open follow-ups:

- Remaining product packages off definition XML / dual-run exit metrics (Phase 5 #2632)
- XSL migration cookbook (#2834); Design SPA help (#2835)
