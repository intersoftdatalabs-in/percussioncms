# False Present vs open Failed QA reconcile

**Parent reality-check:** [#3102](https://github.com/intersoftdatalabs-in/percussioncms/issues/3102)  
**This slice:** [#3109](https://github.com/intersoftdatalabs-in/percussioncms/issues/3109)  
**Grandparent program:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Gap matrix:** [../contracts/gap-matrix.md](../contracts/gap-matrix.md)  
**Disposition date:** 2026-08-11  
**Status:** Docs-only reconcile complete — matrix **Present → Partial** where open Failed / open human QA contradicts release-ready claims.

---

## Rule applied

From #3102:

> **Present without closed human QA is not release-ready.** Flip or keep Partial until QA passes; do not treat agent merge alone as CE parity.

Agent-merged product-route wiring remains valuable evidence that code landed, but matrix **Present** means operators can rely on the capability. Open **QA: Failed** or still-open QA handoffs block that claim.

---

## Rows flipped Present → Partial

| Matrix capability | Linked QA / bugs (state at reconcile) | Why Partial |
|-------------------|----------------------------------------|-------------|
| Sites/folders tree | [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) open p1 (Sites not loaded / no create); [#3101](https://github.com/intersoftdatalabs-in/percussioncms/issues/3101) open dual-run folder QA | Hierarchy not operator-usable for Sites sample content |
| Full product shell composition | [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** | Shell/DF/search chrome QA failed |
| Display formats for columns | [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** | Same shell composition QA |
| Simple / extended search | [#2588](https://github.com/intersoftdatalabs-in/percussioncms/issues/2588) **Failed** | Same shell composition QA |
| Menu bar (Content / View / Help) | [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) open | Menu chrome not QA-passed |
| View options / refresh | [#2741](https://github.com/intersoftdatalabs-in/percussioncms/issues/2741) · [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) open | View refresh residual open |
| Open / preview item | [#2745](https://github.com/intersoftdatalabs-in/percussioncms/issues/2745) open | Preview + refresh residual open |
| Server-driven toolbar/context menus | [#2783](https://github.com/intersoftdatalabs-in/percussioncms/issues/2783) · [#2856](https://github.com/intersoftdatalabs-in/percussioncms/issues/2856) · [#2988](https://github.com/intersoftdatalabs-in/percussioncms/issues/2988) open | Nested menus still open QA; operators report flat label buttons |
| Saved searches catalog + run | [#2607](https://github.com/intersoftdatalabs-in/percussioncms/issues/2607) **Failed** · [#2645](https://github.com/intersoftdatalabs-in/percussioncms/issues/2645) open · [#2729](https://github.com/intersoftdatalabs-in/percussioncms/issues/2729) **Failed** | Picker / View_All / Playwright surface not operator-ready |

---

## Explicitly defended Present (not flipped)

| Capability | Defense |
|------------|---------|
| Multi-select list | No Failed QA tracker on selection model alone; #3102 targets toolbar/tree/search |
| Detail list of children | `DetailList` chrome present; Sites load failure tracked on tree row (#2989) |
| Open / reveal from results | Callbacks present; outcomes gated by Partial search rows |
| Search in ContentBrowser hosts | Host mount not cited Failed in #3102; product-shell search already Partial |
| Reduced folder actions | Outside #3102 false-Present cite list |
| Clipboard / wizards / dependency / relationships | Outside this slice’s cite list (re-audit if their QA fails) |
| Translation locales + create | Present + Explicit OUT per #2829; do not re-litigate OUT here |
| Object ACL full editor | Already Partial cross-epic (#2828) |

---

## Non-goals (this slice)

- Closing or reassigning human QA issues  
- Re-implementing Explorer chrome, Sites load, or saved-search bugs  
- Product IN/OUT for Views / Inbox (slice 1 #3108 / implement backlog #3110)  
- Full DCE re-audit of every Present row with only “To Be Tested” QA  

---

## Follow-up

1. When a linked QA issue **passes**, flip that matrix row back to **Present** and cite the QA close comment.  
2. When QA **Fails** with a fixable product defect, file/fix the p1 bug; keep matrix **Partial** until re-QA.  
3. Sites hierarchy: prioritize [#2989](https://github.com/intersoftdatalabs-in/percussioncms/issues/2989) before new chrome features (#3102 suggested next actions).  
4. Views (DCE category) + Inbox disposition remain on #3102 slices #3108 / #3110 — separate from this QA reconcile.  
5. **2026-08-18:** Closing a **QA: Failed** ticket after residuals is **not** a Pass. [#3577](https://github.com/intersoftdatalabs-in/percussioncms/issues/3577) refreshed Partial citations and re-audited Relationships (**Present → Partial**). See [closed-qa-partial-citation-refresh.md](./closed-qa-partial-citation-refresh.md). **Do not** treat that docs slice as a Present flip.

---

## Changelog

| Date | Note |
|------|------|
| 2026-08-11 | #3109: initial reconcile; nine capability rows Present → Partial; defend multi-select, detail list, host search, reduced actions, OUT translation, Object ACL Partial. |
| 2026-08-18 | #3577: citations only — Failed QA tickets closed after residuals; no Present flip. Pointer: [closed-qa-partial-citation-refresh.md](./closed-qa-partial-citation-refresh.md). |
