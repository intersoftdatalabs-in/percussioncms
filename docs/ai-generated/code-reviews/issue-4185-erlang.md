# Erlang review — #4185 SPA Action Menus usage/command/visibility

Independent of the implementer. Scope: WebUI Action Menu UI-03 chrome, Vitest,
Playwright surface spec, product-docs 8.2. REST stacked from #4183/#4184; not
re-implemented.

## Findings

- **Bugs:** none found. Create POST stays identity-only (JAXB #4171). PUT sends
  handler, parameters, properties, visibilityContexts, uiContexts. System 409
  and non-Admin 403 reuse existing save mapping. Empty visibility name rows are
  omitted; empty arrays clear on PUT (REST contract).
- **Tests:** Vitest covers 400/403/409 on UI-03 save plus PUT save/reload of
  usage/command/visibility. Playwright extends
  `developer-action-menu-editor.spec.js` without weakening UI-02 create
  assertions.
- **Paths:** no filesystem I/O; REST keys use `encodeURIComponent`.
- **Companions:** product-docs 8.2 admin Action Menus + REST note; Playwright
  surface path; design-gap strings drop UI-03 completeness.

## Gate

Pass for this slice. Do not claim UI-04 cascading children.
