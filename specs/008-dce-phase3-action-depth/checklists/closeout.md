# Closeout Note — 008 DCE Phase 3 Action Depth

**Epic**: [#6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110) (Phase 3 disposition; parent program #2400 closed, continuation epic #4530)
**Branch**: `dce-phase3-action-depth`
**Date**: 2026-11-11

## WP0 — Coverage map (Option C enforced)

`checklists/coverage-map.md` dispositions every FR: FR-001/002/006–011 **verify**
(shipped surfaces referenced, not rebuilt), FR-003 build-or-rework, FR-004/005
**build**. Backend probe **GO** — `POST /actions/find/transitions` →
`IActionMenuAdaptor.findAllowedTransitions` → sitemanage `ActionMenuAdaptor`
chain intact, no new REST work.

## US1 — Transition offers (FR-001/002/010) — CLOSED (verify-grade)

No production change needed: offer projection (`workflowMenuActions.ts`),
eligibility guards (`workflowEligibility.ts`), catalog merge, dispatch routing,
and post-execute refresh were all wired (#2732, #3330/#3329). Delivered:
- `workflowOfferMatrix.test.ts` — 11/11 green (offers/intersection/recompute/eligibility)
- `explorer-workflow-offers.spec.js` — 2/2 green on live H2 cell (offers + empty cases, axe-scoped)

## US2 — Properties dialogs (FR-003/004/005/009) — CLOSED (build)

`ItemPropertiesPanel` grouped into General (name + new read-only path row) and
Display (shown + edit + clear) sections. Server surface is exactly the 3 fields
(`ItemProperties`: path/name/displayTitle), so parity = present all of them grouped.
Folder side verified no-build (`FolderPropertiesEditor` covers community/locale/DF/workflow).
- `itemPropertiesParity.test.ts` — 4/4 green (groups/values, edit-persist round trip incl. reload-commits-server-truth, read-only denied, 403 messaging)
- `explorer-item-properties.spec.js` — 2/2 green on live H2 cell (roundtrip + denied, axe-scoped)

## US3 — New-content menus (FR-006/007/008) — CLOSED (verify-grade)

Type listing, template branch, and create-flow entry all wired
(`contentTypeChoices`, pickers, `createPageInFolder`, `openInEditor`).
- `newContentMenuListing.test.ts` — 4/4 green (exact sets, host exclusion, de-dupe, empty)
- `explorer-new-content.spec.js` — 2/2 green on live H2 cell (stubbed exact type set; template branch via catalog templates)

## US4 — Bulk handling (FR-011/009) — CLOSED (verify + 1-line export)

`transitionMultiSelection` already evaluates per item and reports per-item
outcomes through the shell status/error path. Only change: exported pure
`partitionWorkflowSelection` for tests. No result-UI build needed.
- `workflowBulkResults.test.ts` — 3/3 green (skip buckets, 403 names item+reason, nothing-eligible invokes nobody)

## i18n / a11y

3 new `perc.ui.explorer@` keys (General/Display/Path) follow the established
fallback pattern — no CmsUi.tmx entries exist for the explorer surface at all
(T019/T026 N/A with reason). Axe evidence recorded in T034 (all live specs).

## PR strategy (T013/T020/T027/T032)

Single 008 feature PR, not four per-story PRs (precedent: 006/007 single-PR
ships). Rationale: US1/US3 are tests-only, US2/US4 are minimal + additive;
splitting would quadruple review overhead with no isolation benefit.

## Pending human gates (out of implementation scope)

- **T035 SC-004 operator trial** (≥3 operators, transition/review/create flows
  incl. one partial-failure case) — requires human operators; file QA tracking
  or run post-merge.
- **#6110 epic close** — after PR merge + T035.
- **qa-down** the H2 cell when verification is fully signed off.
