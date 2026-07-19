# UAT sign-off checklist (SC-001 / SC-003 / SC-008)

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Evidence for success criteria that are manual/UAT (T125).  
**Status**: Seed — fill during UAT on PR #1370 or US9.

## Environment

| Field | Value |
|-------|--------|
| Build / PR | #1370 / branch `990-unified-publishing-ui` |
| Environment URL | |
| Date | |
| Tester | |

## SC-001 — Under 2 minutes to full publish path

| Step | Pass? | Notes |
|------|-------|-------|
| Open Publishing (modern shell) | | |
| Select site → server → full publish | | |
| Job visible in Status | | |
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
| Bad server config | | |
| Permission / FORBIDDEN | | |
| Job failed mid-run | | |

## US9 residual (optional before release claim)

| Matrix row | Verified Done after T119–T122? |
|------------|--------------------------------|
| OPS-18 approval path | |
| OPS-20 status sort | |
| OPS-22 log filters | |
| OPS-23 log item details | |

**Sign-off**: _name / date / PR_
