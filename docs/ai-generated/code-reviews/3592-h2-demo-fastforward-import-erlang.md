# Erlang review — #3592 H2 demo-sites FastForward import + ISO date index

**Scope:** uncommitted vs `HEAD` on `fix/issue-3592-h2-demo-fastforward-import` (parent #3102 residual after #3575 / PR #3591).

**Recommendation:** approve

**Gate:** May commit/push: yes

**Memory patterns hit:** behavioral unit tests for changed logic; incomplete change-class (companions: persist + date parse + tests); Hibernate persist/merge / `@Version` / cascade; empty-catch with justified ignore.

## Summary

Two stock H2 `--demo-sites` `server.log` ERROR families that fail `qa-health` `server_log_errors`:

1. FastForward file-store import: `PSBinary.data` is inverse `@OneToOne(mappedBy="binary")` with no persist cascade; `PSBinaryData` uses a foreign-id generator. `PSHashedFileDAO.save` `merge()` of the transient graph throws `TransientPropertyValueException`.
2. Search-index dates: `PSDateUtils.getDateFromString` used `SimpleDateFormat` `yyyy-MM-dd'T'HH:mm:ss.SSSZ` which does not parse demo ISO `2008-11-02T00:00:00.000Z`.

Fixes stay in `system` + `projects/sitemanage`. No public method signature change. Product-docs N/A (internal persist + parse). No WebUI.

## Cross-platform path checklist

No filesystem path join, install path, or OS-specific I/O in this diff. N/A / clean.

## Issues

None (hard-gate).

### Notes (not blocking)

- `getDateFromString` swallows `DateTimeException` then falls through to `OffsetDateTime` / `SimpleDateFormat` so invalid input still throws `ParseException` (existing contract + test).
- `save` uses `id == 0` as new-entity (next-number generator; `PSBinary.version` has no accessor). Acceptable.
- Persist/import tests cover cascade mapping, bidirectional link, persist-vs-merge order, and `create()` graph. Not a live Hibernate Session; H2 `qa-up`/`qa-health` is the runtime proof.

## Tests

- `PSBinaryPersistImportTest` (system): 5 tests, 0 fail
- `PSDateUtilsTest` (sitemanage): 2 tests including `2008-11-02T00:00:00.000Z`
- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS (Tests run: 2168, Failures: 0, Errors: 0, Skipped: 238)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS (Tests run: 1250, Failures: 0, Errors: 0, Skipped: 125)
