# UAT sign-off checklist (SC-001 / SC-003 / SC-008)

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Evidence for success criteria that are manual/UAT (T125).  
**Status**: Ready for human UAT on PR #1370 after US9 code land (2026-07-19). Automated Vitest covers unit parity for residual OPS rows; wall-clock SC criteria need environment runs.  
**Tracking issue**: [#1371](https://github.com/intersoftdatalabs-in/percussioncms/issues/1371) (milestone **8.2**)

## Environment

| Field | Value |
|-------|--------|
| Build / PR | #1370 / branch `990-unified-publishing-ui` |
| Environment URL | _fill during UAT_ |
| Date | _fill during UAT_ |
| Tester | _fill during UAT_ |

## SC-001 — Under 2 minutes to full publish path

| Step | Pass? | Notes |
|------|-------|-------|
| Open Publishing (modern shell) | | `publishModern.jsp` / nav Publish |
| Select site → server → full publish | | Sites section → workspace Full |
| Job visible in Status | | Status section poll ≤ 5s |
| Elapsed wall time (excl. job runtime) | | Target &lt; 2 min |

## SC-003 — Ops without Design (≥80% usability target)

| Participant | Completes full publish without Design? | Notes |
|-------------|----------------------------------------|-------|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
| 5 | | |
| **Rate** | | Target ≥ 4/5 |

## SC-008 — Diagnose failed publish via Status/Logs only

| Failure scenario | Diagnosable in modern Status/Logs? | Notes |
|------------------|------------------------------------|-------|
| Bad server config | | FORBIDDEN / BADCONFIG messaging + server editor alerts |
| Permission / FORBIDDEN | | |
| Job failed mid-run | | Status progress + Logs filters + structured details panel |

## US9 residual (code complete 2026-07-19)

| Matrix row | Verified Done after T119–T122? |
|------------|--------------------------------|
| OPS-18 approval path | Yes — unit: `incrementalApproval.test.ts`; UI: related checkboxes → `publishIncrementalWithApproval` |
| OPS-20 status sort | Yes — unit: `statusSort.test.ts`; UI: sortable Status headers |
| OPS-22 log filters | Yes — unit: `logsFilter.test.ts`; UI: site/server/days/maxcount |
| OPS-23 log item details | Yes — unit: extract/filter in `logsFilter.test.ts`; UI: `LogDetailsPanel` |

**FR-020 review (T126)**: `ServerEditor` and design panels surface save failures with `role="alert"`; parent workspace shows success status after server save. No additional toast framework required for this cut.

**Sign-off**: _name / date / PR_ (human after environment UAT)
