# Erlang review — #3846 sitemanage leftover IPS*Errors typed ErrorCodes

- Branch: `fix/issue-3846-sitemanage-typed-errorcodes`
- Base: `origin/main`
- Date: 2026-08-26
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: behavioral tests for changed logic; incomplete change-class closure (enum + skip dual-write + production call-site + allow-list shrink)

## Summary

Ten leftover `projects/sitemanage` production `IPS*Errors` call-sites now compare/throw via existing typed catalogs (`AssemblyErrorCodes`, `NavigationErrorCodes`, `WebserviceErrorCodes`, `HttpErrorCodes`, `PublisherErrorCodes`). `PSSiteSectionService` only dropped an unused `IPSExtensionErrors` import. Numeric codes are unchanged (`*.numericCode()`), so REST/UI contracts are untouched. Allow-list rows for those exact paths were removed; freeze-gate pytest now uses a remaining `system/services` residual.

## Change class

Typed ErrorCodes production call-site conversion (non-auditable leftover ints).

Companions present: existing enums (no new catalog constants); adaptor/unit tests with production exception types; dual-write skip assertions on non-auditable codes; allow-list + pytest + scripts README; standalone sitemanage clean install.

## Cross-platform path checklist

N/A — no filesystem path / I/O changes. Allow-list remains POSIX relative paths.

## Issues

None.

Nit: `PSPageDatabaseAssemblerTypedErrorCodeTest` constructs `PSAssemblyException` rather than invoking `preProcessItemBinding` (parent assembler static init needs Spring). Production throw/compare paths are covered by `TemplateAdaptor*`, `PSSitePublishDaoFindSummaryTypedErrorCodeTest`, `PSAbstractTemplateExpanderAdapterTypedErrorCodeTest`, `PSSiteDaoLoadSiteBadNavTest`, and `PSSiteDataRestServiceSaveNavTest`.

## Verification

- `python scripts/verify-no-bare-ipserrors.py` — PASS (no sitemanage production residuals)
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 17 passed
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1600, Failures: 0, Errors: 0, Skipped: 125
