# Contract: Action menu API (US3 — post-cutover)

**Provider**: `rest` module — `com.percussion.rest.actions.ActionMenuResource` (`@Path("/actions")`)  
**Purpose**: Configuration-driven menus for modern Content Explorer (CE model), replacing the **ReducedAction set** (FR-010a) long-term.

## Discovery (expected surface)

Public REST exposes action menu finders via adaptor (`IActionMenuAdaptor`), including approximately:

- Find menus for UI context / visibility
- Allowed transitions for content id(s)
- Allowed content types / templates

Exact query parameters and payload fields: implementers MUST align TypeScript types to live OpenAPI/Javadoc of `ActionMenuResource` and related DTOs (`ActionMenu`, `ActionMenuVisibilityContext`, `ActionMenuModeUIContext`, `ActionMenuParameter`) at implementation time—**do not invent fields**.

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
