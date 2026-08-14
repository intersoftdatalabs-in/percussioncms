# Erlang-style review: #3364 Create Site addNavTreeToFolder 500

Reviewer: night-issue-prs (independent of implementer)
Date: 2026-08-14
Verdict: **pass for this slice** (nav-tree seed no longer 500s). Residual: homepage/template save after seed.

## Change class

Traditional site create (`POST /sitemanage/site/`, managedNavigation default true) seeds a NavTree via `PSManagedNavService.addNavTreeToFolder`.

## Findings

### Hard-gate bugs

None remaining in the nav-tree seed path.

Prior defect: `saveItems(..., checkin=true)` failed Default Workflow check-in (`sys_wfPerformTransition` / `m_nextAgingTransition` NPE), wrapped as `PSErrorResultsException` → `PSNavException` → HTTP 500.

Fix: save without check-in, attach to the folder, do **not** call `checkinItems` in the same site-create transaction (a failed check-in marked the outer Spring tx rollback-only).

### Residual (out of this slice, filed separately)

H2 live create still fails later in `createHomePageAndTemplate` → `createSiteTemplate` (`percPageTemplate` save): Hibernate `StatementPreparerImpl.connection()` is null, then `UnexpectedRollbackException`. NavTree attach itself succeeds.

### Tests

- `PSManagedNavServiceAddNavTreeToFolderTest` — save without check-in; no `checkinItems`; already-has-navon/navtree codes; save failure wrapped with cause.
- `PSSiteContentDaoManagedNavTest` — invalid nav create rethrown typed.
- `PSSiteDataRestServiceSaveNavTest` — already-has-nav → HTTP 400.

### Cross-platform

No filesystem path construction. N/A.

### Companions

REST 4xx mapping on existing `PSSiteDataRestService.save` (no new site API). Product-docs `product-docs/8.2/admin/sites.md` notes 400 vs 500.
