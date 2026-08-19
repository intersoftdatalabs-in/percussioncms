# Partial citation refresh after closed Failed-QA residuals

**Parent reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102)  
**This slice:** [#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577)  
**Grandparent program:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Gap matrix:** [../contracts/gap-matrix.md](../contracts/gap-matrix.md)  
**Prior reconcile:** [false-present-qa-reconcile.md](./false-present-qa-reconcile.md) (#3109, 2026-08-11)  
**Disposition date:** 2026-08-18  
**Status:** Docs-only. Citations refreshed to live GitHub state. **No row flipped to Present.**

---

## Rule applied

From #3102 / gap-matrix QA gate:

> **Present without closed human QA is not release-ready.** Agent merge (or closing a Failed qa-task after residuals) is not CE parity.

Reconcile close on 2026-08-18 shut several **unassigned QA: Failed** tickets after implement residuals landed. Those tickets still carry **QA: Failed** (not Passed). Citing them as *open Failed* was stale; citing them as a Present license is also wrong.

**This slice does not flip any row to Present.** Human QA still required. Views / Inbox stay **Partial**.

---

## Live state (2026-08-18)

| Original Failed QA | State | Residuals / retest | Matrix rows (stay **Partial**) |
|--------------------|-------|--------------------|--------------------------------|
| [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) shell/DF/search | **CLOSED** Failed (reconcile) | Residual [#3208](https://github.com/intersoftdatalabs-in/percussioncms/issues/3208) via cluster [PR #3278](https://github.com/intersoftdatalabs-in/percussioncms/pull/3278); retest [#3264](https://github.com/intersoftdatalabs-in/percussioncms/issues/3264) **Passed**; search chrome [#2858](https://github.com/intersoftdatalabs-in/percussioncms/issues/2858) **Passed**; free-text [#2966](https://github.com/intersoftdatalabs-in/percussioncms/issues/2966) **Passed** | Full product shell composition; display formats; simple/extended search |
| [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) menu bar | **CLOSED** Failed (reconcile) | Clipboard toggle [#3544](https://github.com/intersoftdatalabs-in/percussioncms/issues/3544) / [#3551](https://github.com/intersoftdatalabs-in/percussioncms/issues/3551) via cluster [PR #3557](https://github.com/intersoftdatalabs-in/percussioncms/pull/3557) | Menu bar; View options / refresh |
| [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) preview + refresh | **CLOSED** Failed (reconcile) | Listing [#3457](https://github.com/intersoftdatalabs-in/percussioncms/issues/3457); preview [#3456](https://github.com/intersoftdatalabs-in/percussioncms/issues/3456) / [PR #3464](https://github.com/intersoftdatalabs-in/percussioncms/pull/3464); console [#3458](https://github.com/intersoftdatalabs-in/percussioncms/issues/3458) | Open / preview item; View options / refresh |
| [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) saved-search picker | **CLOSED** Failed (reconcile) | [#3205](https://github.com/intersoftdatalabs-in/percussioncms/issues/3205) / [#3199](https://github.com/intersoftdatalabs-in/percussioncms/issues/3199) / [#3517](https://github.com/intersoftdatalabs-in/percussioncms/issues/3517); retest [#3234](https://github.com/intersoftdatalabs-in/percussioncms/issues/3234) **Passed**, [#3237](https://github.com/intersoftdatalabs-in/percussioncms/issues/3237) **Passed**, [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Passed**. Human QA [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) To Be Tested (still **open** as of 2026-08-19). Operator proof [#3576](https://github.com/intersoftdatalabs-in/percussioncms/issues/3576) **closed** 2026-08-19 ([PR #3593](https://github.com/intersoftdatalabs-in/percussioncms/pull/3593) merged — see #3619). | Saved searches catalog + run |
| [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) nested toolbar | **CLOSED** Failed (reconcile) | [#3379](https://github.com/intersoftdatalabs-in/percussioncms/issues/3379) / [#3500](https://github.com/intersoftdatalabs-in/percussioncms/issues/3500) / [#3560](https://github.com/intersoftdatalabs-in/percussioncms/issues/3560) / [PR #3565](https://github.com/intersoftdatalabs-in/percussioncms/pull/3565); prior QA [#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856) **Passed**, [#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988) **Passed** | Server-driven toolbar/context menus |
| [#2778](https://github.com/intersoftdatalabs-in/percussioncms/issues/2778) Relationships | **CLOSED** Failed (reconcile) | [#3546](https://github.com/intersoftdatalabs-in/percussioncms/issues/3546) via cluster [PR #3557](https://github.com/intersoftdatalabs-in/percussioncms/pull/3557). **No Passed human QA.** | IA / relationships view (**Present → Partial** — see below) |

**Sites/folders tree** (not a Failed qa-task, but citations were stale): original Sites-load [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) **closed**; dual-run QA [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) **Passed**. Operator proof [#3575](https://github.com/intersoftdatalabs-in/percussioncms/issues/3575) **closed** 2026-08-18/19 ([PR #3591](https://github.com/intersoftdatalabs-in/percussioncms/pull/3591) merged); H2 demo-sites qa-health residual [#3592](https://github.com/intersoftdatalabs-in/percussioncms/issues/3592) **closed** ([PR #3602](https://github.com/intersoftdatalabs-in/percussioncms/pull/3602) merged). Stay **Partial**. Live leftover: [#3619](https://github.com/intersoftdatalabs-in/percussioncms/issues/3619) / [partial-citation-residual-after-3577.md](./partial-citation-residual-after-3577.md).

---

## Relationships: Present → Partial (not Present)

#3109 defended Relationships as Present because it was outside the 2026-08-11 false-Present cite list, with “re-audit if Failed.” Human QA [#2778](https://github.com/intersoftdatalabs-in/percussioncms/issues/2778) **Failed** (hint-only; no parseable content id) and was later reconcile-closed after the id-bind residual. That is **not** a QA Pass.

Per the matrix QA gate, open Failed / closed-Failed-without-Pass → keep **Partial**. This slice therefore moves **IA / relationships view** from **Present** to **Partial**. It does **not** move it to Present.

---

## Explicitly unchanged

| Capability | Status | Why |
|------------|--------|-----|
| Views (DCE navigation category) | **Partial** | Product-route proof #3561; never Present from this docs slice |
| Inbox | **Partial** | Same |
| Detail list / multi-select / reduced actions / host search / clipboard / wizards / dependency / translation / Object ACL | Unchanged | Out of #3577 scope; do not invent OUT or Present |

---

## Non-goals

- Product UI / REST changes  
- Closing #3102  
- Inventing OUT rows  
- Flipping any row to **Present**  
- Closing or reassigning remaining human QA (#2645 and others)

---

## Changelog

| Date | Note |
|------|------|
| 2026-08-18 | #3577: refresh Partial citations after Failed-QA reconcile close; Relationships Present → Partial (no Passed QA); Sites/saved-search remaining proof tickets cited. |
| 2026-08-19 | #3619 leftover: proof tickets #3575/#3576/#3592 closed; snapshot table no longer live. See [partial-citation-residual-after-3577.md](./partial-citation-residual-after-3577.md). |
