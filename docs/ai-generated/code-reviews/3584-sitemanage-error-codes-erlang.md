# Erlang review — #3584 SiteManage IPSSiteManageErrors → SiteManageErrorCodes

- Branch: `fix/issue-3584-sitemanage-error-codes`
- Base: `origin/main`
- Date: 2026-08-19
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: behavioral tests for changed logic; incomplete change-class closure (enum + skip dual-write + production call-site already present)

## Summary

Leftover sitemanage production call site in `PSSiteDao.loadSite` now constructs `PSException` from typed `SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD` (`IPSErrorCode`). Legacy `IPSSiteManageErrors` remains as a documented int bridge. Dual-write skip already existed on the registry test and is now also asserted from `SiteManageErrorCodesTest`. Focused `PSSiteDaoLoadSiteBadNavTest` covers the missing-nav delete path and the non-matching nav rethrow without cactus.

## Change class

Typed ErrorCodes production call-site conversion (non-auditable CFG 18252).

Companions present: existing enum + registry registration; skip dual-write tests; same-package unit test for `loadSite`; bridge interface retained.

## Cross-platform path checklist

N/A — no filesystem path / I/O changes.

## Issues

None.

## Verification

- `cd modules/perc-auditlog && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 292, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1336, Failures: 0, Skipped: 125 (`PSSiteDaoLoadSiteBadNavTest` 3/3)
