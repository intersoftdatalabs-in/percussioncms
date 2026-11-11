# Gap matrix: rx732 Workbench → React Developer module

**Parent**: #1690 · **Baseline**: `docs/developer-module/workbench-functional-inventory.md`
**Evidence date**: 2026-11-11 · **Slice rollup**: 105 #1690 rows (~41 done, ~61 PR-open, 1 blocked, remainder verified/triaged)

**Rule**: Status reflects shipped React product behavior, not slice-PR existence. **Present** requires merged implementation **plus** closed human QA (agent merge alone is insufficient). Silent omission is not allowed — unmapped capabilities must be **Missing**.

Legend: **Present** | **Partial** | **Missing**

| Capability (inventory) | React Developer today | Status | Slice evidence |
|---|---|---|---|
| Content Design: types/fields/searchable/required (§6) | Type/field editors shipped in slices | **Partial** (slices merged; human QA pending) | Content Design bucket 21/43 done (labels/descriptions/keywords/locales landing) |
| Assembly Design: templates/slots/display formats (§7) | Label/description editors shipped | **Partial** | Assembly bucket 11/17 done |
| UI Elements: menus/searches/views (§8) | Early coverage | **Partial** | UI Elements bucket 0/1 done — thinnest area, needs slice growth or explicit scope note |
| Security: roles/users/communities/ACLs (§9) | Role/user/community editors landing | **Partial** | Security bucket 5/23 done; 61-pr-open bulk sits here + workflow |
| Workflow catalog: transitions/steps/aging/inbox (§4) | Transition/step/role editors landing | **Partial** | Workflow bucket 3/10 done |
| System Design objects (§10) | Per-slice coverage | **Partial** | Rolled into slice table; reconcile per-object in T004 |
| XML Server / Data Pipeline (§11) | Per data-pipeline inventory | **Partial** | Tracked alongside pipeline inventory; reconcile in T004 |
| Navigators: sites/folders/communities (§12) | Early coverage | **Partial** | Navigators/Sites bucket 0/2 done — needs slice growth or scope note |
| Wizards: create flows (§13) | Per-object create flows | **Partial** | Covered within object slices; reconcile in T004 |
| Primary journeys end-to-end (§14) | Slice-complete paths only | **Partial** | No journey-level trial yet — SC-002 needs it |

## Reconciliation notes (for T004)

- Bucket counts above are keyword rollups of #1690 slice titles (2026-11-11), not per-capability audits. T004 must map every inventory §4–14 capability to either a slice row or an explicit **Missing** row here.
- "Other" bucket (6 rows) needs classification into the areas above.
- UI Elements (0/1) and Navigators (0/2) are the thinnest — first candidates for new slices or explicit out-of-scope notes with rationale.
