# Erlang review: PR #3209 Nate review — list all sites

## Summary

Human review (`natechadwick`, CHANGES_REQUESTED) on PR #3209 was ignored by the
overnight Kilo follow-up (which only answered four inline Kilo threads).

Requested product behavior:

- Sample / FastForward sites are **Rhythmyx**, not CM1 — `IS_PAGE_BASED` must be
  false (already true in seed XML).
- `PSSiteDataService.findAll` must **not** drop sites for page-based, pub
  server, or nav tree. The list is all sites; Explorer varies later by type.

This change removes `.filter(PSSiteSummary::isPageBased)` from `findAll`, adds
behavioral unit tests, and documents the list contract in product-docs.

## Scope

- Worktree: `.kilo/worktrees/pr-3209` on `fix/issue-2989-demo-sites-pagebased-folders`
- Files:
  - `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java`
  - `projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSiteDataServiceFindAllTest.java`
  - `product-docs/8.2/admin/sites.md`
  - this report
- Memory patterns hit: missing behavioral tests; product-docs for operator-facing
  list behavior
- Cross-platform path review: N/A (no file I/O)

## Recommendation

**approve**

## Gate

- May commit/push: **yes**
- Bugs: none
- Missing behavioral tests: no — `PSSiteDataServiceFindAllTest` covers mixed
  page-based/Rhythmyx listing and copy-target omission
- Change-class closure: site-list service + unit test + product-docs admin page
- Agent rule files: none

## Issues

None.

## Verification noted

```text
cd projects/sitemanage
..\..\mvnw.cmd clean install
# BUILD SUCCESS — Tests run: 1102, Failures: 0, Errors: 0, Skipped: 125
# PSSiteDataServiceFindAllTest: 2 tests, 0 fail
```
