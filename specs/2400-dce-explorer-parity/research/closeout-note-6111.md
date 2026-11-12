# Closeout note + citation refresh + QA state checks (issue #6111)

**Parent:** #2400 (spec package `specs/2400-dce-explorer-parity/`) ·
**Disposition issue:**
[#6111](https://github.com/intersoftdatalabs-in/percussioncms/issues/6111) ·
**Evidence date:** 2026-11-12 (live GitHub state + `origin/main` tip)

## 1. Closeout: #2400 is closed, continuation is #4530

- [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)
  ([8.2] p1 research: DCE ↔ Explorer UI functional parity audit) **CLOSED
  2026-09-09**. This spec/plan/matrix package is a **frozen research record**;
  do not plan new implement work against it.
- Continuation tracker:
  [#4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530)
  ([8.2] Content Explorer — next product increments) **OPEN**. All new
  Explorer product slices, re-audits, and human-QA handoffs track there
  (e.g. re-audit #6107, Phase 3 disposition #6110, open retests #6210 /
  #6211 for preview and Inbox).
- Sibling slices already landed on this package in the same program:
  Phase 3 disposition **no-gap** ([#6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110),
  sibling research note landing separately via PR #6228) and the six-row stale-matrix re-audit
  ([#6107](https://github.com/intersoftdatalabs-in/percussioncms/issues/6107),
  recorded in [gap-matrix.md](../contracts/gap-matrix.md) §
  "2026-11-12 Stale gap-matrix re-audit").

## 2. QA state checks (acceptance row 2)

All four named QA / residual items verified **CLOSED** on live GitHub
2026-11-12. **No follow-up issues filed — none are still open.**

| Issue | Subject | Live state 2026-11-12 | Disposition |
|-------|---------|----------------------|-------------|
| [#2600](https://github.com/intersoftdatalabs-in/percussioncms/issues/2600) | QA (#2410): Folder security panel identities + properties | **CLOSED** 2026-08-17, labelled `QA: Passed` | Done. Matrix rows "Folder ACL view/edit" + "Folder properties" cite it as Passed. |
| [#2797](https://github.com/intersoftdatalabs-in/percussioncms/issues/2797) | QA (#2792): Subfolder copy wizard in Explorer shell | **CLOSED** 2026-08-18, labelled `QA: Passed` | Done. Matrix row "Subfolder copy wizard" cites it as Passed. |
| [#3546](https://github.com/intersoftdatalabs-in/percussioncms/issues/3546) | Relationships panel mounts for selected content row (residual of Failed QA #2778) | **CLOSED** 2026-08-18 (implement residual, shipped via cluster PR #3557) | Done. Matrix row "IA / relationships view" cites the residual as closed; row stays **Partial** per its own rule (original QA #2778 never Passed), not per #3546. |
| [#2799](https://github.com/intersoftdatalabs-in/percussioncms/issues/2799) | QA (#2793): ContentBrowser host SearchPanel (asset picker) | **CLOSED** 2026-08-25, labelled `QA: Failed` (closed by maintainer after Failed verdict 2026-08-14) | **Dispositioned, not open.** Failed QA was worked through residuals: [#3438](https://github.com/intersoftdatalabs-in/percussioncms/issues/3438) (asset-picker SearchPanel mount + search execute) **CLOSED** 2026-08-15, and [#3714](https://github.com/intersoftdatalabs-in/percussioncms/issues/3714) (Asset Picker Cannot Select Search Result for Confirmation) **CLOSED** 2026-08-25 labelled `QA: Passed` — the operator proof that the Failed steps now pass. Matrix row "Search in ContentBrowser hosts" citation refreshed accordingly (was stale "To Be Tested"). |

Related context (not in the acceptance list, checked to avoid stale cites):
- Saved-search human QA [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645)
  **CLOSED** 2026-08-21, labelled `QA: Passed` — the "remaining #2645 open
  To Be Tested" wording in the dated 2026-08-10/11 snapshot sections is
  historical record, not live state (those sections are explicitly labelled
  snapshots; left untouched).

## 3. Citation refresh (acceptance row 1)

Living-package edits shipped with this slice (historical dated snapshot
sections intentionally untouched):

- `spec.md` — closeout banner at top: #2400 closed 2026-09-09,
  continuation #4530, pointer to this note.
- `plan.md` — § "Epic #2400 remaining-open criteria" / "Current state"
  updated: epic **closed**, continuation #4530; pointer to this note.
- `contracts/gap-matrix.md` —
  - header **Evidence date** line extended with the 2026-11-12 closeout
    refresh (this note) alongside the frozen 2026-08-19 baseline;
  - capability-table row "Search in ContentBrowser hosts": stale
    "QA #2799 To Be Tested" → closed Failed with residuals #3438 closed /
    operator proof #3714 Passed;
  - rows "Folder ACL view/edit", "Folder properties": cite #2600 Passed;
  - "2026-08-10 Present re-confirm" table: #2797 → Passed, #2799 → closed
    Failed dispositioned via #3438 / #3714.

## Decision

- [x] Closeout note added pointing to #4530; stale citations refreshed —
      **done (this note + package edits above)**.
- [x] States of #3546 / #2600 / #2799 / #2797 verified; follow-ups filed
      only for still-open items — **all four CLOSED, no follow-ups filed**.
