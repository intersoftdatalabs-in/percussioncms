# Contract: Action menu API (US3 — post-cutover)

**Provider**: `rest` module — `com.percussion.rest.actions.ActionMenuResource` (`@Path("/actions")`)  
**Purpose**: Configuration-driven menus for modern Content Explorer (CE model), replacing the **ReducedAction set** (FR-010a) long-term.

## Maturity note (added per PR review)

The public REST surface under `/actions` is **not feature-complete** today. It exposes only a subset of the menu-finding and visibility machinery that the legacy **XML action manager** (`sys_ActionPage`, `ActionMenu` configuration) provides to Desktop Content Explorer. The long-term intent is for `/actions` (and any thin sitemanage façade needed for **execution**) to fully replace the XML action manager so the modern explorer can render the same configurable menu tree CE does today. Until that parity lands:

- The **ReducedAction set** (FR-010a; see [../data-model.md](../data-model.md) § *ReducedAction*) ships at the Finder / Desktop CE intermediate hard cut to keep the intermediate bar achievable without the full action manager.
- The full configuration-driven menu phase (US3, FR-010) **requires** either `/actions` reaching feature parity with the XML action manager, **or** a thin sitemanage façade in front of the XML action manager exposing its data over web (see `plan.md` Complexity Tracking and `tasks.md` T052).
- Capability matrix rows marked **P-Menu** track each CE menu capability against the source-of-truth (XML action manager vs REST). See [./capability-matrix.md](./capability-matrix.md) P-Menu rows + Translation (P-Trans) row.

This note was added per PR review on `specs/992-react-content-explorer/contracts/action-menu-api.md` line 8.

## Discovery (expected surface)

Public REST exposes action menu finders via adaptor (`IActionMenuAdaptor`), including approximately:

- Find menus for UI context / visibility
- Allowed transitions for content id(s)
- Allowed content types / templates

Exact query parameters and payload fields: implementers MUST align TypeScript types to live OpenAPI/Javadoc of `ActionMenuResource` and related DTOs (`ActionMenu`, `ActionMenuVisibilityContext`, `ActionMenuModeUIContext`, `ActionMenuParameter`) at implementation time—**do not invent fields**. When a row in the capability matrix P-Menu or P-Trans lists cannot be served by `/actions` at US3 implementation, follow the gap policy at the bottom of this file.

## Client responsibilities

1. On selection change, request allowed actions for current context (folder vs item, multi-select rules).
2. Render context menu + optional toolbar from server list (labels: TMX when keys exist; else server label).
3. Hide or disable actions not returned / not permitted (FR-011).
4. On invoke:
   - Prefer documented REST/itemmanagement/path endpoints mapped from action.
   - Or navigate to server-provided URL in product-safe frame/dialog pattern used by CM.
5. Refresh tree/list after successful mutating actions (FR-012).
6. Keyboard: open menu, focus items, activate (FR-013).

## AuthZ

- Server is authoritative; never enable an action solely because the client cached it.
- CSRF on any POST execution paths.

## Gap policy

If menu **listing** works but **execution** has no safe web path for a high-value CE action:

1. Record gap in [capability-matrix.md](./capability-matrix.md).
2. Prefer existing sitemanage/itemmanagement services.
3. Only then add a thin authenticated façade (plan Complexity Tracking).

## Hard-cut relationship

- **Not required** for Finder or Desktop CE hard cut (FR-010a ReducedAction set).
- Required for SC-003 full-menu phase and long-term FR-010.

## Out of scope here

- Redesigning action configuration admin UI (Eclipse/workbench).
- Desktop CE action manager port.

