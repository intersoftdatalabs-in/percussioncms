# Partial citation residual after #3577 reconcile-closed QA

**Parent reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102)  
**This slice:** [#3619](https://github.com/intersoftdatalabs-in/percussioncms/issues/3619)  
**Grandparent program:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Gap matrix:** [../contracts/gap-matrix.md](../contracts/gap-matrix.md)  
**Prior reconcile:** [false-present-qa-reconcile.md](./false-present-qa-reconcile.md) (#3109, 2026-08-11)  
**Prior citation refresh:** [closed-qa-partial-citation-refresh.md](./closed-qa-partial-citation-refresh.md) (#3577, 2026-08-18)  
**Disposition date:** 2026-08-19  
**Status:** Docs-only. Citations refreshed to live GitHub state. **No row flipped to Present.**

---

## Why this residual exists

[#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577) / [PR #3594](https://github.com/intersoftdatalabs-in/percussioncms/pull/3594) updated **capability-table Slice cells** so reconcile-closed Failed QA is labeled **Failed, closed**. Two leftovers still read as **live open Failed**:

1. The **#3109 snapshot table** in `gap-matrix.md` (and the matching table in [false-present-qa-reconcile.md](./false-present-qa-reconcile.md)) still said **Failed** / **open** for `#2588`, `#2741`, `#2745`, `#2783`, `#2607`, `#2729`, `#2989`, `#3101` without a snapshot banner.
2. Proof tickets cited as **open** in those Slice cells (`#3575`, `#3576`, `#3592`) **closed after #3577 merged**.

Closed **QA: Failed** is still **not** a Pass. This slice does **not** flip any row to **Present**. Views / Inbox / saved-search stay **Partial** (human QA [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) still **To Be Tested**).

---

## Live state (2026-08-19)

| Ticket | Live state | Matrix rows (stay **Partial**) |
|--------|------------|--------------------------------|
| [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) | **CLOSED** Failed (reconcile 2026-08-18) | Shell composition; display formats; simple/extended search |
| [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) | **CLOSED** Failed (reconcile 2026-08-18) | Menu bar; View options / refresh |
| [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) | **CLOSED** Failed (reconcile 2026-08-18) | Open / preview item; View options / refresh |
| [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) | **CLOSED** Failed (reconcile 2026-08-18) | Server-driven toolbar/context menus |
| [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) | **CLOSED** Failed (reconcile 2026-08-18) | Saved searches catalog + run |
| [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) | **CLOSED** **Passed** | Saved searches catalog + run |
| [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) | **CLOSED** (bug, not QA Failed) | Sites/folders tree |
| [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) | **CLOSED** **Passed** | Sites/folders tree |
| [#3575](https://github.com/intersoftdatalabs-in/percussioncms/issues/3575) | **CLOSED** ([PR #3591](https://github.com/intersoftdatalabs-in/percussioncms/pull/3591) merged) | Sites/folders tree |
| [#3576](https://github.com/intersoftdatalabs-in/percussioncms/issues/3576) | **CLOSED** ([PR #3593](https://github.com/intersoftdatalabs-in/percussioncms/pull/3593) merged) | Saved searches catalog + run |
| [#3592](https://github.com/intersoftdatalabs-in/percussioncms/issues/3592) | **CLOSED** ([PR #3602](https://github.com/intersoftdatalabs-in/percussioncms/pull/3602) merged) | Sites/folders tree |
| [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) | **OPEN** To Be Tested | Saved searches catalog + run — **keep Partial** |
| [#3617](https://github.com/intersoftdatalabs-in/percussioncms/issues/3617) | **OPEN** ([PR #3620](https://github.com/intersoftdatalabs-in/percussioncms/pull/3620)) | Simple / extended search |
| [#3618](https://github.com/intersoftdatalabs-in/percussioncms/issues/3618) | **OPEN** ([PR #3621](https://github.com/intersoftdatalabs-in/percussioncms/pull/3621)) | Display formats for columns |

---

## Explicitly unchanged

| Capability | Status | Why |
|------------|--------|-----|
| Views (DCE navigation category) | **Partial** | Product-route proof #3561; never Present from this docs slice |
| Inbox | **Partial** | Same |
| Saved searches catalog + run | **Partial** | Human QA #2645 still open To Be Tested |
| Detail list / multi-select / reduced actions / host search / clipboard / wizards / dependency / translation / Object ACL | Unchanged | Out of #3619 scope; do not invent OUT or Present |

---

## Non-goals

- Product UI / REST changes  
- Closing #3102  
- Implementing qa-tasks  
- Inventing OUT rows  
- Flipping any row to **Present**  
- Closing or reassigning remaining human QA (#2645)

---

## Changelog

| Date | Note |
|------|------|
| 2026-08-19 | #3619: leftover of #3577 — snapshot table no longer reads as live Failed/open; proof tickets #3575/#3576/#3592 cited closed; remaining live #2645/#3617/#3618. Never Present. |
