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

## Epic ownership map (issue #6102)

Spec bullets (`spec.md` "This slice" 1–9 + "Later" rich controls / promote form = 11 bullets) → owning epic. Shipped first-slice work stays closed under #3452/#3455; **all follow-on increments** route to exactly one of #4532 / #5622.

| # | Spec bullet | Status | Owning epic |
|---|---|---|---|
| 1 | Edit / Quick Edit / Edit content / Edit properties open host in edit mode | Shipped (#3452) | #4532 for any follow-on gaps |
| 2 | View content / View properties / revision view open view mode (no checkout) | Shipped (#3452) | #4532 for any follow-on gaps |
| 3 | Edit-mode checkout (`GET .../workflow/checkOut/{id}`) | Shipped (`editorCheckout.ts`) | #4532 |
| 4 | Scalar fields `GET`/`PUT .../item/fields/{id}` | Shipped | #4532 |
| 5 | Labels from `GET /services/contenttypes/{type}` | Shipped | #4532 |
| 6 | Save + Check In (existing checkIn REST) | Shipped | #4532 (required/invalid-field errors already closed via #5986) |
| 7 | New Item create-in-folder + open editor | Shipped (`editorCreate.ts`) | #4532 for any follow-on gaps |
| 8 | percPage template flow (`templateId` through `IPSPageService`) | Shipped (#3455) | #4532 for any follow-on gaps |
| 9 | Home / TopNav entries (Recent/Bookmarks/Search/Library; Home Create; TopNav Editor = `/editor`) | Shipped | #4532 for any follow-on gaps |
| 10a | Rich HTML editing (TinyMCE → TipTap) | Widget exists (`TipTapFieldWidget`, `HtmlFieldWidget`) | **#5622** (TipTap replacement epic; TinyMCE 6.8.6 stays frozen until it lands) |
| 10b | File / image upload controls | Widgets exist (`FileFieldWidget`, `ImageFieldWidget`) | **#4532** (fields/binary-API controls, not rich-text) |
| 10c | Keyword + community controls | Widgets exist | **#4532** — see D1 |
| 11 | Revision promote form (`PromoteForm.tsx`) | Exists | **#4532** — see D2 |

Out-of-scope pointer: AA contenteditable overlay for rich assembled fields belongs to spec 996 (issue #3475), not either epic here.

### Recorded decisions

- **D1 — keyword (+ community) control ownership → #4532.** Keyword/community controls persist through the itemmanagement fields API like other scalar controls; they are not rich-text editing, so they stay out of the TipTap replacement epic (#5622 covers HTML editing only).
- **D2 — revision promote form ownership → #4532.** The promote form is workflow/publish-adjacent work inside the editor host, alongside #4532's planned children (workflow transition actions with comments, publish now from the host).

### Duplicate-triage note

- #5986 (required/invalid field errors on save, #4532 child) is **CLOSED** (2026-11-11). The `done_duplicate`-yet-open confusion noted in #6102 is resolved; no further action.
