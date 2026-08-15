# Erlang-style review: #3393 Create Site homepage/template after NavTree seed

Reviewer: night-issue-prs (independent of implementer)
Date: 2026-08-15
Verdict: **approve**

## Change class

Traditional site create (`POST /sitemanage/site/`, managedNavigation default true) after #3364
NavTree seed: `createHomePageAndTemplate` → `createSiteTemplate` (`percPageTemplate` save).

## Findings

### Hard-gate bugs

None.

Prior residual: `PSContentItemDao.save` still called `find()` after `contentWs.saveItems`.
Workflow JDBC (`sys_wfUpdateHistory`) can leave the Hibernate session connection null.
Nested `@Transactional` `loadBodies` then NPEs (`StatementPreparerImpl.connection()`) and
marks the outer site-create transaction rollback-only (`UnexpectedRollbackException`).

Fix matches peer #1563: return the in-memory item with id set; do not re-find.

Homepage `checkinItems` after landing-page attach is the same Default Workflow check-in
that #3364 removed from NavTree seed. Removed so a failed check-in cannot mark the
surrounding transaction rollback-only. Item is valid once it is a folder child.

### Tests

- `PSContentItemDaoSaveAfterNavSeedTest` — save does not call `find` / `loadBodies`;
  returns the same item with saveItems id.
- `PSSiteContentDaoManagedNavTest.createsHomepageAndTemplateAfterNavSeedWithoutCheckin`
  — after nav seed, createTemplate + page save + landing page + folder add; no `checkinItems`.

### Cross-platform

No filesystem path construction. N/A.

### Companions

Product-docs `product-docs/8.2/admin/sites.md` notes NavTree + site template + homepage
in one Create Site operation. Playwright happy path spec comment updated (#3393).

Memory patterns hit: missing behavioral tests (covered); change-class closure
(product-docs + peer #1563/#3364); check-in / post-save find marks rollback-only;
non-portable path I/O (none).
