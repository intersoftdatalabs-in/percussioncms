# Plan: React Content Editor (995) — verify shipped slice, close control gaps

**Spec**: `spec.md` (this directory) · **Issue track**: #4530-era editor slices (#3452 merged); rich-control follow-ups as filed
**Branch**: `spec-plan-995-996` (planning only; implementation branches per phase)

## Goal

Prove the shipped editor host works end-to-end (FR-001–009 below), then close any gaps in the "Later" control set (FR-010–014). Rule: verify before building — the host, widgets, and REST wiring already exist in-tree.

## Functional requirements (derived from spec.md)

| ID | Requirement | Status in-tree |
|---|---|---|
| FR-001 | Edit/Quick Edit open host in edit mode; View opens view mode (no checkout) | Shipped (#3452) — verify |
| FR-002 | Edit mode checks out via `GET .../workflow/checkOut/{id}` | Shipped (`editorCheckout.ts`) — verify |
| FR-003 | Scalar fields load/save via `GET`/`PUT .../item/fields/{id}` (`sys_*` except `sys_title` omitted; binary omitted) | Shipped — verify |
| FR-004 | Labels from `GET /services/contenttypes/{type}` | Shipped — verify |
| FR-005 | Save + Check In via existing checkIn REST | Shipped — verify |
| FR-006 | New Item (type under New) creates in-folder (`POST /item/create`) and opens editor | Shipped (`editorCreate.ts`) — verify |
| FR-007 | percPage template flow (one auto, many picker, `templateId` through `IPSPageService`) | Shipped (#3455) — verify |
| FR-008 | Home/TopNav entries (Recent/Bookmarks/Search/Library; Home Create; TopNav Editor = `/editor`) | Shipped — verify |
| FR-009 | Denied/failed saves message, never silent loss | Shipped (`editorFieldErrors.ts`) — verify |
| FR-010 | Rich HTML editing (TipTap; TinyMCE only as escape hatch) | Widget exists (`TipTapFieldWidget`, `HtmlFieldWidget`) — verify, build only gaps |
| FR-011 | File/image upload controls | Widgets exist (`FileFieldWidget`, `ImageFieldWidget` + `editorBinary.ts`) — verify, build only gaps |
| FR-012 | Keyword + community controls | Widgets exist — verify, build only gaps |
| FR-013 | Revision promote form | Exists (`PromoteForm.tsx`) — verify, build only gaps |
| FR-014 | New strings resolve via TMX/fallback; axe clean on editor host | Convention check — verify |

Out of scope (spec): Explorer browse, preview-by-template, purge, folder ops, Data Flow Server as renderer, slot add/arrange (996).

## Workstreams

- **WP0** — inventory: map each FR to in-tree file + covering test (Vitest/Playwright), record gaps.
- **WP1** — verification battery: Vitest for pure helpers + Playwright specs per control group on H2 cell (follow the 008 surface-filter pattern).
- **WP2** — gap fixes only for failures WP1 finds (real bugs, not re-verification loops).
- **WP3** — closeout note + tick tasks.

## Gates

Constitution III (tests), WebUI Playwright hard gate for screen changes, Erlang pre-commit review, per-module `clean install` for any production change (tests-only changes still run the WebUI gate).
