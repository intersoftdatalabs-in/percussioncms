# Erlang review — follow-up to #936, fix pack 2, re-review

**Reviewer:** Erlang (independent pre-merge reviewer; not the author)
**Branch:** `development` (uncommitted changes, working tree)

**Recommendation:** `commit` — all four prior findings are fixed; no
new bugs introduced.

## Verification of the prior findings

1. **DB property-key alignment.** `DatabaseDriverFields.tsx` now
   renders per-driver: server/port/userid/password always required;
   MySQL+`database`; MSSQL+`database`+`owner` (not `schema`);
   Oracle+`sid`+`schema` (not `database`). `requiredFieldsForDriver()`
   split into MySQL/MSSQL/Oracle/`DATABASE` branches with per-driver
   canonical keys. `DB_FIELDS` removed. New tests:
   `databaseDriverFields.test.tsx` (4 cases) verifies each driver
   emits the canonical key and not the wrong one; `serverValidation.test.ts`
   (3 new cases) verifies each DB driver passes with canonical keys
   and fails without them.

2. **Driver-scoped alias.** `applyLegacyAliases(map, driver)` now
   only deletes `user` when driver ∈ {FTP, FTPS, SFTP} and only
   deletes `bucketName` when driver ∈ {AMAZONS3, S3}. New tests
   prove Local-with-`user`, Local-with-`bucketName`, MySQL-with-`user`
   all preserve the unrelated property; SFTP-with-`user` aliases;
   FTP-save-no-legacy-leak.

3. **SFTP/DB validation tests.** New behavioral cases for SFTP
   (canonical `userid`, rejects legacy `user`), MySQL (canonical,
   no `schema`/`sid`), MSSQL (canonical + `owner`), Oracle (`sid`+
   `schema`, no `database`), and whitespace blankness.

4. **Whitespace blankness.** Shared `isBlank()` helper in
   `serverFormModel.ts` (`value == null || String(value).trim() === ""`);
   matches the validation semantics. Whitespace-only `userid`
   correctly falls back to legacy `user`; whitespace-only canonical
   is reported as missing.

## New-bug scan

- Existing happy-path tests broken? No — three tests in
  `serverValidation.test.ts` were *updated* to use canonical keys;
  assertions tightened, not weakened. All pass.
- `serverToModel()` driver resolution when `properties` is a plain
  object? Correct — both array and record branches resolve via
  `(Array.isArray(...) ? find : record.driver) ?? ""` then
  `|| rawMap.driver || "Local"`.
- `propsToMap(props)` no-arg still works? Yes —
  `applyLegacyAliases(out, undefined)` uppercases to `""`, neither
  set contains `""`, no-op. Backward compatible.
- Other callers of `propsToMap`? None outside `serverFormModel.ts`
  and its test. Signature change safe.
- `DatabaseDriverFields` conditional rendering assumptions?
  `ServerEditor.tsx:224` is the only call site;
  `DATABASE_DRIVERS = ["MSSQL", "MYSQL", "Oracle"]` covers every
  driver it receives. The `d === "DATABASE"` branch in
  `requiredFieldsForDriver` is validation-only fallback and never
  reaches the renderer.

## Nits (not gating)

- `serverFormModel.ts:164` — `rawMap` is computed only to read
  `driver`; could simplify. Cosmetic.
- `requiredFieldsForDriver` uses `d.includes("ORACLE")` (substring)
  but the renderer uses exact `d === "ORACLE"`. No current driver
  string relies on the substring; flag if a new driver appears.
- **Release note worth:** existing MSSQL/Oracle servers saved with
  the legacy `user` key will not be auto-promoted (intentional, since
  MSSQL/Oracle are not in `USER_ALIAS_DRIVERS`). Users will see a
  blank userid and must re-enter. This is the explicit trade-off
  in Finding 2's fix design.

## Verification

- `npx vitest run` on the 4 changed/new files: **32/32 passed**.
- `npx tsc --noEmit` clean.
- Cross-platform path review: N/A (pure TS/React).

## Handoff

**May commit / push: yes.**