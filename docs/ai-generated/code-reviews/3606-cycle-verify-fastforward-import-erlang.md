# Erlang review — #3606 Cycle Verify residual: FastForward import + ISO-Z dates

**Scope:** uncommitted vs `HEAD` on `fix/issue-3606-fastforward-import` (ports PR #3602 / #3592 onto current `main` for cycle-verify residual #3606). Prior report: `docs/ai-generated/code-reviews/3592-h2-demo-fastforward-import-erlang.md`.

**Recommendation:** approve

**Gate:** May commit/push: yes

**Memory patterns hit:** behavioral unit tests for changed logic; incomplete change-class (companions: persist + date parse + tests); Hibernate persist/merge / cascade; empty-catch with justified ignore.

## Summary

Stock CMS+H2 `--demo-sites` `qa-up` failed cycle-verify because FastForward import logged ERROR (`TransientPropertyValueException` on `PSBinary` → `PSBinaryData`) and timed out. Search-index ISO `…Z` dates also ERROR. This residual lands the same product fix as PR #3602 on a branch from current `main` so a fresh `qa-up` / `qa-health` is RESULT:OK without allowlisting `PSDbStorageService` / generic `server_log_errors`.

1. `PSBinary.data` inverse `@OneToOne` now cascades `PERSIST`+`MERGE` (not `ALL`/`REMOVE`).
2. `PSHashedFileDAO.save` persist-orders parent then blob; `save(null)` throws; `getBinary` skips save when the row is missing.
3. `PSDateUtils.getDateFromString` accepts Instant / trailing `Z` / `OffsetDateTime`.

No public/protected method or ctor signature change. Product-docs N/A (internal persist + parse). No WebUI product-screen change (Playwright golden/login is issue-acceptance proof after `qa-health`, not a screen companion).

Does not close #3592.

## Cross-platform path checklist

No filesystem path join, install path, or OS-specific I/O in this diff. N/A / clean.

## Issues

None (hard-gate).

### Notes (not blocking)

- `getDateFromString` swallows `DateTimeException` then falls through to `OffsetDateTime` / `SimpleDateFormat` so invalid input still throws `ParseException` (existing contract + test).
- `save` uses `id == 0` as new-entity (next-number generator; `PSBinary.version` has no accessor). Acceptable.
- Persist/import tests cover cascade mapping, bidirectional link, persist-vs-merge order, `create()` graph, `save(null)`, and missing-hash skip. Not a live Hibernate Session; H2 `qa-up`/`qa-health` is the runtime proof.

## Tests

- `PSBinaryPersistImportTest` (system): 7 tests, 0 fail
- `PSDateUtilsTest` (sitemanage): ISO-Z including lowercase `z`
- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS (Tests run: 2170, Failures: 0, Errors: 0, Skipped: 238)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS (Tests run: 1250, Failures: 0, Errors: 0, Skipped: 125)
