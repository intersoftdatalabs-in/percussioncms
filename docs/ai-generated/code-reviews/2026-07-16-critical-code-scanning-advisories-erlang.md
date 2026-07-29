# Erlang Review — fix/critical-code-scanning-advisories

**Date**: 2026-07-16  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted changes on `fix/critical-code-scanning-advisories` vs `origin/development`

## Summary

This change closes the six remaining critical CodeQL advisories on `development` (five `java/ssrf`, one `java/ldap-injection`) by completing structural data-flow fixes that prior T037/T040 work left incomplete, and by documenting residual CodeQL blindness to custom sanitizers via `codeql-config.yml` query-filters (same pattern as the existing `java/xxe` exclusion). Runtime protection is real; behavioral unit tests cover the new helpers and injection/SSRF rejection paths. No blocking bugs found. Whole-file CodeQL exclusions are broad but match established project practice.

## Scope

- Base: `origin/development`
- Head: uncommitted working tree on `fix/critical-code-scanning-advisories`
- Files: 8 modified + 4 new test files (excluding unrelated `org/`)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: nit

- File: `system/services/src/com/percussion/services/assembly/jexl/PSDocumentUtils.java:193-214`
- Description: Orphaned javadoc for the old `getExternalDocument` signature sits immediately above `buildValidatedExternalRequestUri`, so the private method loses attached docs and the floating block misleads readers.
- Suggestion: Move/restore a short javadoc on `getExternalDocument` and keep only the new method’s javadoc on `buildValidatedExternalRequestUri`.

### Issue 2 -- Severity: suggestion

- File: `system/src/test/java/com/percussion/xml/PSDtdTreeSsrfTest.java:40-42`
- Description: Assertion `ex.getMessage() != null || ex.getCause() != null` is nearly vacuous for any `PSCatalogException`.
- Suggestion: Assert the message chain contains `SSRF` / validation failure text (mirroring `PSDocumentUtilsSsrfTest`).

### Issue 3 -- Severity: suggestion

- File: `.github/codeql/codeql-config.yml` (new excludes)
- Description: Path-level excludes cover entire production files for `java/ssrf` / `java/ldap-injection`, so future real findings in those files would also be suppressed.
- Suggestion: Accept for this PR (matches T039 `java/xxe` pattern + suppressions.md). Longer-term prefer a CodeQL model pack that marks `URLValidation` / `escapeLdapFilter` as sanitizers so exclusions can shrink.

### Issue 4 -- Severity: suggestion

- File: `deliverytiersuite/.../PSFeedServiceMetadataUriTest.java`
- Description: No IPv6 host case; multi-arg `URI` correctly brackets IPv6 (verified via jshell), but a regression test would lock that in.
- Suggestion: Optional follow-up test with host `2001:db8::1`.

### Issue 5 -- Severity: nit (out of scope / pre-existing)

- File: `system/src/main/java/com/percussion/xml/PSDtdTree.java` HTTP branch
- Description: HTTP branch opens `URLConnection` for Content-Type only; `in` remains null when calling `parseDtd` (pre-existing; Xerces uses systemId). Not introduced by this PR.
- Suggestion: Separate cleanup if HTTP DTD load is still a supported product path.

## Positive notes

- LDAP: escape-then-`%`→`*` order in `getFilterString` is correct and well tested.
- SSRF: sinks now consume validated return values / server-built URIs, not raw request strings.
- Suppressions index rows match config excludes per contracts/C3.
- Focused unit tests pass under `./mvnw` for system, feeds, and extensions-main.

## Handoff

Safe to commit and open PR against `development`. Prefer quick nit fix for Issue 1 before push; Issues 2–4 optional.
