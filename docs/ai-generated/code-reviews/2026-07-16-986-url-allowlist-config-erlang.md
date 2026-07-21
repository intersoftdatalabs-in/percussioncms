# Erlang Review — 986-url-allowlist-config (issue #1205)

**Date**: 2026-07-16  
**Reviewer**: Erlang (strict pre-PR)  
**Scope**: Uncommitted implementation of URL allow/block lists

## Summary

Implements install-root `allowedUrls.properties` / `blockedUrls.properties` with full-URL globs, additive allow (private unlock via match), block precedence, seed-if-missing, removal of unreleased system-property config, packaging with never-overwrite, and unit tests. Decision order matches the clarified spec. Tests: 28 unit + 14 proxy + 7 system SSRF green (integrity ledger regenerated after packaging edit).

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: suggestion
- File: `URLValidation.java` decision order
- Description: Hard metadata deny runs before block-list match; operators cannot “allow” metadata even if they remove block lines (intentional defense-in-depth per research R8). Document clearly in release notes (already in snippet).
- Suggestion: Keep as-is.

### Issue 2 -- Severity: nit
- Description: Optional server-init wiring (tasks T029) skipped when `rxdeploydir` is set at process start; production CMS sets this. Document that tests must inject config via `setDefault`/`fromFiles`.
- Suggestion: Accept for v1.

### Issue 3 -- Severity: nit
- Description: `new URL(...)` deprecation warnings on Java 21 in tests — pre-existing style in this module.
- Suggestion: Follow-up to URI if desired.

## Positive notes

- No utils dependency cycle; seed/no-overwrite covered by unit tests.
- Builder ignores lone `*`; block wins proven for private host.
- Consumer SSRF suites still pass without call-site changes.

## Handoff

Safe to commit and open PR linking #1205.
