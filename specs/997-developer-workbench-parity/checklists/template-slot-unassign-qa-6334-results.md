# QA results — unassign one slot from a template region (#6334)

QA verification slice for merged PR #6323
(fix(WebUI): unassign one slot from a template region with confirm, #6067;
parent #1690). Executes the three numbered test-plan steps from issue #6334
against a fresh H2 QA cell on current `origin/main` tip and records the
results here.

**Result: PASS** — unassign spec 1/1 + assign-regression spec 2/2 green, zero
JS console/page errors (asserted in-spec), Vitest `TemplateDetailPanel` 43/43
green, zero new `server.log` ERROR/FATAL lines from the template paths.

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`,
  `TEST_CMS_URL=http://127.0.0.1:51965` (H2, `perc-matrix-cms-h2`;
  installer jar copied from the primary checkout built at the same tip
  commit — same source, QA-cell use only, not committed)
- `python docker/scripts/perc-devctl.py qa-deploy-webui` → `RESULT:OK`
  (current-tip SPA hot-copied)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK`,
  `HTTP:200 HEALTH:healthy` (before and after deploy)

## Test plan

### 1. Confirm dialog appears — PASS

Developer → Templates → `perc.page` detail, lock held: choosing **Unassign**
on an assigned member opens the confirm dialog
(`developer-catalog-confirm-dialog` visible) while the region still renders
assigned. Live proof:
`docs/ai-generated/proofs/issue-6334-template-slot-unassign.png`
(confirm open over the Slots table, `rffAutoPressReleases2007` still checked).

### 2. Confirm persists only the target slot; cancel is a no-op — PASS

`tests/developer-template-slot-unassign.spec.js` → **1 passed**: cancel sends
no template PUT and the mapping is kept; confirm PUTs 200, the PUT payload
drops only the target slot (all other `beforeNames` kept), a reload of the
detail still renders the region unassigned, and the member is restored so the
cell is left as found. Assign-regression
`tests/developer-template-slot-assign.spec.js` → **2 passed** (lock/assign/
save/restore/unlock; save-without-lock sends no PUT).

### 3. Error path keeps the mapping — PASS

Covered by the in-spec assertions plus the unit suite: cancel is a no-op
(step 2), and the save-failure contract (400/403/409 keeps the previous
mapping, slot still renders assigned) is pinned by
`WebUI/src/test/ts/developer/TemplateDetailPanel.test.tsx` → **43 passed**
(includes the #6067 unassign-confirm cases).

## Out of scope (per issue)

- Slot assignment flows (regression-covered, not the changed behavior).
- Backend datasource write (sibling slice).
