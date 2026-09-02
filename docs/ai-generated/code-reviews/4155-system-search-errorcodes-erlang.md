# Erlang review: #4155 system search IPSSearchErrors typed SearchErrorCodes

**Scope:** uncommitted work on `fix/issue-4155-search-error-codes` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-09-02

## Summary

Parent #2616 leftover slice. Production `com.percussion.search` throw sites (`PSSearchEngine`, `PSGenerateSearchResultsExit`, `PSAdminLockedException`, lucene `PSSearchQueryImpl` / `PSSearchEngineImpl` / `PSSearchIndexerImpl`) plus `PSSearchHandler` search-engine required now construct typed `SearchErrorCodes` / `LuceneErrorCodes` / `ServerErrorCodes.RAW_DUMP` via additive `PSSearchException(IPSErrorCode…)` constructors. `IPSSearchErrors` remains the numeric bridge. Dual-write skip tests cover leftover non-auditable search/lucene codes; `SEARCH_ENGINE_AUTHENTICATION_FAILED` remains auditable. Allow-list shrunk for fully converted paths only. `PSServer` / other handler families left for #4150 / #4153. No product UI/config surface.

Memory patterns hit: change-class closure (typed ctors + production throws + dual-write skip + allow-list shrink + freeze-gate pytest); no signature-breaking API changes (additive ctors; no `(String, IPSErrorCode)` overload).

## Gate

No bugs. Behavioral tests cover typed construction, production `PSAdminLockedException` / `PSSearchQueryImpl` throws, dual-write skip vs AUTH dual-write. Freeze gate `python scripts/verify-no-bare-ipserrors.py` PASS. Pytest `scripts/test_verify_no_bare_ipserrors.py` 23 passed. Standalone `cd modules/perc-auditlog && ../../mvnw.cmd clean install` BUILD SUCCESS, Tests run: 315, Failures: 0. Standalone `cd system && ../mvnw.cmd clean install` BUILD SUCCESS, Tests run: 2700, Failures: 0, Skipped: 245. Cross-platform path checklist: N/A (no new filesystem path construction).

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New tests do not assert Unix-only absolute path shapes
- [x] N/A for product scripts / installers (allow-list paths stay posix `/` as required by the freeze gate)
