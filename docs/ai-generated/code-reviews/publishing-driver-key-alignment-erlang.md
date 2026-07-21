# Erlang review — follow-up to #936, fix pack 2 (driver property-key alignment), initial

**Reviewer:** Erlang (independent pre-merge reviewer; not the author)
**Branch:** `development` (uncommitted changes, working tree)
**Scope:** follow-up to #936 (which closed the BADCONFIG-UX bug). The
prior commit landed the response-body mapper; this commit aligns the
React driver-field components with the canonical backend property keys
so credentials are no longer silently dropped.

**Files:** `WebUI/src/main/ts/publishing/components/drivers/FileDriverFields.tsx`,
`WebUI/src/main/ts/publishing/components/drivers/DatabaseDriverFields.tsx`,
`WebUI/src/main/ts/publishing/serverValidation.ts`,
`WebUI/src/main/ts/publishing/serverFormModel.ts`,
plus four test files.

**Recommendation:** `request-changes` — three real bugs and one
whitespace-handling polish.

## Findings

1. **bug — Database property-key alignment is still incomplete.**
   `DatabaseDriverFields.tsx` renders `schema` for MSSQL but the
   backend (`PSDatabasePubServer.java:110`) reads
   `IPSPubServerDao.PUBLISH_OWNER_PROPERTY` ("owner"); Oracle is
   required to provide `database`, but the backend reads `sid`
   (`PSDatabasePubServer.java:99`) for Oracle; Oracle never renders
   `schema` even though the backend reads `PUBLISH_SCHEMA_PROPERTY`;
   `DB_FIELDS` in `serverValidation.ts` omits `port` for every
   database driver. **Fix:** per-driver field matrices (MySQL:
   common + `database`; MSSQL: common + `database` + `owner`;
   Oracle: common + `sid` + `schema`); `DatabaseDriverFields` must
   emit `owner` for MSSQL and required `sid`/`schema` for Oracle.

2. **bug — Legacy alias deletion can corrupt unrelated custom
   properties.** `applyLegacyAliases()` runs unconditionally for
   every server and deletes `user` / `bucketName` regardless of
   driver. `PSPubServerService.setProperties()` clears the
   persisted property set and re-adds only what the client sends,
   so a Local / Database / custom server with an unrelated property
   literally named `user` or `bucketName` would be silently renamed
   or removed on next save. **Fix:** scope `user → userid` to
   FTP/FTPS/SFTP and `bucketName → bucketlocation` to AMAZONS3/S3.
   Add regression tests proving custom aliases survive round-trips
   on unrelated drivers.

3. **bug — Changed DB and SFTP validation paths lack behavioral
   tests.** `requiredFieldsForDriver()` changed SFTP from `user` to
   `userid` and split DB_FIELDS into per-driver matrices, but the
   test file only covers FTP/S3 happy-paths. **Fix:** add
   `validateServerForm()` tests for SFTP (succeeds with `userid`,
   fails with `user`), MySQL/MSSQL/Oracle (succeed with canonical
   keys, fail without them).

4. **suggestion — Alias blankness differs from validation blankness.**
   `applyLegacyAliases()` treats only `""` as empty while
   `validateServerForm()` treats whitespace-only values as empty.
   Use a shared `isBlank()` predicate so the read path does not
   promote a whitespace-only legacy value over a missing canonical
   value (and vice-versa).

## Summary

The direct FTP/FTPS/SFTP and S3 corrections are right: the modern
component now emits `userid` and `bucketlocation`, matching Minuet
and the backend read sites. The new component tests are behavioral
— they fire real input changes and inspect the callback contract —
and are not unacceptably implementation-coupled. Null/undefined
values and missing `key` with a populated `name` are handled; an
explicitly empty `key` remains skipped as before. Removing
`user`/`bucketName` does not create a redaction leak: neither is
secret-bearing.

Verification: **25 publishing test files / 105 tests passed**;
`npx tsc --noEmit` clean. **Do not commit or push this change pack
until the four findings above are addressed.**