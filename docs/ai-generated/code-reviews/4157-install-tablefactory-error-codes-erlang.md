# Erlang review: #4157 install IPSTableFactoryErrors typed TableFactoryErrorCodes

- **Branch:** `fix/issue-4157-install-tablefactory-error-codes`
- **Parent:** #2616
- **Date:** 2026-09-02
- **Recommendation:** approve
- **Gate:** pass
- **May commit/push:** yes
- **Memory patterns hit:** leftover IPS*Errors retype + dual-write skip tests; shrink residual allow-list; behavioral XML/init throw coverage (not token grep only)

## Summary

Retype remaining production `IPSTableFactoryErrors` throw sites in `system/.../install` JDBC helpers (`PSJdbcNextNumberColumn`, `PSJdbcTableMapper`, `PSJdbcTransitionRoles`, `PSJdbcUniqueColumn`) to existing `TableFactoryErrorCodes` / typed `PSJdbcTableFactoryException` constructors. Numeric codes stay bridged via `IPSTableFactoryErrors`. All leftover catalog codes are non-auditable (`isAuditable()==false`), so dual-write skip is asserted; there is no auditable code in this catalog to dual-write. Allow-list shrinks by those four paths. Behavioral tests cover `fromXml` wrong-type/null/invalid-attr, `init` missing column, and private `getRequiredColumnValue` missing/null column.

## Issues

None (hard-gate).

### Suggestions (non-blocking)

- `PSJdbcNextNumberColumn` still throws `new PSJdbcTableFactoryException(0, e)` on generic execute/next-number SQL failures (pre-existing; not `IPSTableFactoryErrors`). Out of this slice.

## Cross-platform path checklist

N/A — no new filesystem path construction or path assertions.

## Change-class companions

- Production throw-site retype (peer: TableFactory `PSJdbcTableSchema` / #3741)
- Dual-write skip tests on typed exceptions
- `scripts/ipserrors-residual-allowlist.txt` shrink
- Module standalone `mvnw clean install` (system)

## Product documentation

N/A — internal error-catalog retype; no operator/admin/REST/UI surface change.

## C2 API shape

Did not apply: no `final`/`sealed` types; no public/protected signature changes.

## Build

- `cd system && ../mvnw.cmd clean install` → BUILD SUCCESS
- Focused: `PSInstallJdbcTypedErrorCodeSliceTest` Tests run: 16, Failures: 0
