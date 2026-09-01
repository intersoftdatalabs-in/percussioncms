# Erlang review: #4105 H2 multiuser lock harness single-winner checkout

**Branch:** `fix/issue-4105-h2-multiuser-lock-single-winner`  
**Base:** `origin/main`  
**Scope:** uncommitted `PSH2MultiuserLockHarnessTest`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** tests.behavioral (CAS rowcount + hold-until-attempted latch); paths.hardcoded-sep false-positive (H2 JDBC URL `/`)

## Summary

Test-only harness fix for exclusive content checkout on file H2 (`FILE_LOCK=NO`). `SELECT FOR UPDATE` plus an unguarded `UPDATE` allowed two concurrent winners (and/or a serial handoff after check-in inflated the count). Checkout is now a compare-and-set `UPDATE … WHERE CHECKOUTUSER IS NULL` (rowcount == 1); the same-item test holds the winner's checkout until all editors have attempted. Serial handoff after check-in remains allowed and is covered separately.

## Scope

- Base: `origin/main` (`4f8c8dd957`)
- Head: `fix/issue-4105-h2-multiuser-lock-single-winner` (uncommitted)
- Files: 1 production-adjacent test + this review
- Prior report: none
- Memory patterns hit: tests.behavioral; paths JDBC URL `/` is not filesystem join

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Cross-platform path checklist

Applied. Existing JDBC URL still uses `Path` + `toAbsolutePath()` and replaces `\` with `/` only for the H2 `file:` URL (URL path, not OS filesystem join). `@TempDir` is portable. No new hardcoded separators, Unix-only roots, or line-ending assertions.

## Tests

- `PSH2MultiuserLockHarnessTest`: 7 run, 0 fail (focused `mvnw -Dtest=…`)
- `cd system && ../mvnw.cmd clean install`: BUILD SUCCESS; Tests run: 2692, Failures: 0, Errors: 0, Skipped: 247
- Change-class companions: same-class CAS contention + serial-handoff tests; no production API / Spring context / product-docs / Playwright

## Notes

- C2 reverse-deps: N/A (test-only, no `final`/signature change)
- C5 Playwright: N/A (no UI)
- Product documentation: N/A (test-only harness)
