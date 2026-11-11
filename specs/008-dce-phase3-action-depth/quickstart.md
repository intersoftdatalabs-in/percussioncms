# Quickstart validation: DCE Phase 3 Action and Workflow Depth

**Purpose**: Runnable validation scenarios proving the feature against [spec.md](spec.md) success criteria.
**Not**: implementation code (see `/speckit-tasks` later).

## Prerequisites

- Branch: `dce-phase3-action-depth`
- Standing CMS install with workflow states covering the SC-001 sample (≥20 items, ≥5 states)
- DCE available as the behavior reference for offer/dialog/menu comparison

## Scenario matrix

| # | Scenario | Spec refs | Expected |
|---|----------|-----------|----------|
| Q1 | Transition offers match DCE | US1, SC-001, FR-001/002 | 100% match on the sample matrix; no dead options |
| Q2 | Offer refresh after execute | US1 | New state reflected; offer set recomputed |
| Q3 | Folder properties parity | US2, SC-002, FR-003 | All DCE groups beyond ACL present with equal values |
| Q4 | Item properties parity + edit persist | US2, SC-002, FR-004/005 | Groups equal; permitted edit persists and reopens correctly |
| Q5 | New-content menus | US3, SC-003, FR-006/007/008 | Exact type lists; template choice; create flow opens in-folder |
| Q6 | Bulk mixed-permission run | FR-011, FR-009 | Per-item outcomes; successes stand; failures named |
| Q7 | Operator trial | SC-004 | ≥3 operators complete transition/review/create flows with no escalation |
| Q8 | i18n/a11y gates | repo hard gates | No hardcoded new strings (TMX keys); axe checks pass on changed screens |

## Module verification (pre-PR gate)

For each changed module, standalone clean install from the module directory (e.g. `cd WebUI`, `cd rest`, `cd projects/sitemanage` → repo-root `mvnw clean install`). Record BUILD SUCCESS + test counts in the PR body. Docs-only changes: no Maven run required.

## Suggested automated test packages

| Area | Example focus |
|------|---------------|
| Menu offers | Offer-set equality vs DCE matrix (Vitest) |
| Properties dialogs | Group/value parity; edit-persist round trip |
| New-content menus | Allowed-type listing; template branch; create entry |
| Bulk | Mixed-permission selection; per-item result map |
| Playwright | Changed screens end-to-end incl. axe checks (Q1–Q6 surface flows) |
