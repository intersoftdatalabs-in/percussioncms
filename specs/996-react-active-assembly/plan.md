# Plan: React Active Assembly (996) — verify preview host, decide #6104 + Compare

**Spec**: `spec.md` (this directory) · **Issue track**: #3475 (CLOSED, scalar overlay), #6104 (OPEN, related-content menu parity)
**Branch**: `spec-plan-995-996` (planning only; implementation branches per phase)

## Goal

Prove the preview-first host + slot ops + scalar overlay work end-to-end (FR-001–006), then disposition the two explicit later items — #6104 menu parity (FR-007: build or recorded reduced-scope decision) and Compare add-on (FR-008: build or defer decision). Rule: verify before building.

## Functional requirements (derived from spec.md)

| ID | Requirement | Status in-tree |
|---|---|---|
| FR-001 | Assembly window opens from Explorer Active Assembly actions (known content id + template) | Shipped — verify |
| FR-002 | Template resolve via `GET /actions/find/templates/{id}?isAA=true` (preview fallback) | Shipped — verify |
| FR-003 | Preview via `GET /services/assembly/preview-location` + `/assembler/render` iframe + light overlay | Shipped (`AssemblyHost.tsx`) — verify |
| FR-004 | Slot add/create/arrange via canvas relationship REST (selected slot; relationship id for arrange) | Shipped (#3480 lineage) — verify |
| FR-005 | Scalar contenteditable overlay persists via itemmanagement | Shipped (#3475 CLOSED) — verify |
| FR-006 | Denied/failed ops message; no silent loss | Shipped pattern — verify |
| FR-007 | Related-content menu parity replacing `updaterelateditems.html` (R17) | OPEN issue #6104 — build or recorded reduced-scope decision |
| FR-008 | Compare (`sys_Compare`) add-on | No implementation found — build or defer decision |

Out of scope (spec): Explorer browse, Publish Now, revisions, translations, purge, Arrange_* from folder browse with no slot, Content Editor forms, dual-shipping Dojo AA JSPs.

## Workstreams

- **WP0** — inventory: map FR-001–006 to in-tree files + covering tests; confirm FR-007/008 have no hidden implementation.
- **WP1** — verification battery: Vitest for pure helpers (`overlayFields`, `slotContext`) + Playwright specs (open/template/preview, slot add/create/arrange, overlay persist) on H2 cell.
- **WP2** — decisions + builds: FR-007 (implement #6104 scope or record reduced-scope with rationale in the issue), FR-008 (implement Compare or defer with rationale).
- **WP3** — closeout note + tick tasks.

## Gates

Constitution III (tests), WebUI Playwright hard gate, Erlang pre-commit review, per-module `clean install` for production changes.
