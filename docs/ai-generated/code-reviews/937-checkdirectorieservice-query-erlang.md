# Erlang review — fix/937-checkdirectorieservice-query

## Summary

Issue #937 documented that `PSUserService.checkDirectoryService()` called
`findUsersFromDirectoryService("a")` with a meaningless literal query.
The branch swaps that literal call for the existing no-arg
`findUsersFromDirectoryService()` overload (defined in the same file,
`projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:1126`,
which already probes the directory with `%`), and replaces a disabled
placeholder `PSUserServiceMockTest` with six behavioral JUnit 5 + Mockito
tests that pin each of the four `ServiceStatus` mappings plus the
non-propagation contract. Production code is a one-method change with no
new callers or collaborator wiring; behavior is preserved on every error
path and on the happy path.

Overall: clean, targeted fix with strong behavioral coverage.
**Approve.**

## Scope

- Base: `origin/development` (HEAD `11e31f076e`)
- Head: `fix/937-checkdirectorieservice-query` (worktree-local, uncommitted)
- Files: 2 changed (`projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java`,
  `projects/sitemanage/src/test/java/com/percussion/user/service/impl/PSUserServiceMockTest.java`)
- +137 / -10 lines
- Prior report: none for this topic
- Memory patterns hit: `tests.structural-only` (false-positive guard — see
  Notes below)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

### Issue 1 — Severity: suggestion (not blocking)

- File: `projects/sitemanage/src/test/java/com/percussion/user/service/impl/PSUserServiceMockTest.java:146`
- Description: `doesNotPropagateDirectoryException()` is **redundant** with
  `returnsConfigErrorOnConfigException()` two methods above it: both inject
  a `PSDirectoryServiceConfigException` and the latter already proves
  non-propagation by virtue of the call returning a `PSDirectoryServiceStatus`
  (an uncaught exception would have surfaced as a JUnit error). The
  `assertDoesNotThrow` adds no behavioral coverage.
- Suggestion: Drop the test, or convert it to assert the failure mode
  explicitly — e.g. verify that when an
  `IllegalArgumentException` (a non-`PSDirectoryServiceException` runtime
  exception) leaks out of the no-arg probe, `checkDirectoryService()`
  propagates it (negative test, valid coverage for the contract that
  ONLY `PSDirectoryServiceException` is translated). Keep redundant tests are
  harmless, but the asymmetric phrasing adds more noise than value.
- Status: open
- Pattern-id: tests.redundant-assertion

### Issue 2 — Severity: suggestion (not blocking)

- File: `projects/sitemanage/src/test/java/com/percussion/user/service/impl/PSUserServiceMockTest.java:84-92`
- Description: The regression-guard `verify(userServiceSpy).findUsersFromDirectoryService()`
  (no-arg form) does defend against re-introducing a literal query, because
  `findUsersFromDirectoryService("a")` would call a **different overload** and
  leave the no-arg verify unsatisfied. However, this only protects the
  success-path test. A literal `findUsersFromDirectoryService("a")` does not
  change exception-path verification, so the CONFIG_ERROR/CONNECTION_ERROR/
  DISABLED/UNKNOWN_ERROR tests are not themselves regression guards for the
  fix — they pin only the existing behavior, not the no-arg choice.
- Suggestion: Optional. If you want explicit hardening, add a single
  assertion in `returnsEnabledWhenProbeSucceeds` that
  `Mockito.never()` recorded a call with the `@PathParam` overload, e.g.
  `Mockito.verify(userServiceSpy, Mockito.never()).findUsersFromDirectoryService(Mockito.anyString())`.
  Not required for the bug fix because `verify(no-arg)` is already effective.
- Status: open
- Pattern-id: tests.regression-guard

### Issue 3 — Severity: nit

- File: `projects/sitemanage/src/test/java/com/percussion/user/service/impl/PSUserServiceMockTest.java:71`
- Description: Two-line Mockito.mock call using the fully-qualified
  `com.percussion.services.security.IPSBackEndRoleMgr.class` while every
  other collaborator is statically imported. Inconsistent import hygiene.
- Suggestion: Add a static import for `com.percussion.services.security.IPSBackEndRoleMgr`
  and replace the FQN with the simple name to match the surrounding style.
- Status: open
- Pattern-id: nit.import-hygiene

## Cross-platform path review

The diff touches no file I/O, no paths, no installers, no packaging, and
no tests that assert path strings. **No issues.**

## Memory patterns hit

- `tests.structural-only` — false positive guard: the new tests do exercise
  the real `checkDirectoryService()` method through a Mockito **spy** (per
  AGENTS.md, `Mockito.spy(realInstance)` is the established idiom in this
  repo, e.g. `PSFolderRestServiceErrorExposureTest`). This is **behavioral**
  coverage, not source-string grep. No block.
- `tests.regression-guard` — see Issue 2. Not a block.
- No path / cross-platform patterns are relevant.

## Build verification

- `cd projects/sitemanage && ../../mvn-env.sh clean install` → **BUILD SUCCESS**
- 6 new tests pass (`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`)
- No new javac warnings attributable to the changed files (verified via
  `grep "PSUserService.java|PSUserServiceMockTest.java"` against the warning
  stream — empty)
- Pre-existing module-level warnings (serialVersionUID, non-transient
  instance fields, etc.) are unchanged and outside this diff's scope

## Notes

- The existing no-arg `findUsersFromDirectoryService()` at
  `PSUserService.java:1126` is the documented probe entry point (already
  bound to `@GET /external/find` for the public REST surface). The fix is
  not a workaround — it reuses the convention the maintainers already
  established in the same file.
- Behavior is preserved on every error path: `PSDirectoryServiceException`,
  `PSDirectoryServiceConfigException`, `PSDirectoryServiceConnectionException`,
  `PSDirectoryServiceDisabledException`, and the generic
  `PSDirectoryServiceException` are all thrown with the same status mapping
  whether the query is `"a"` or `"%"`. The success-path difference (a `%`
  wildcard vs. a literal `a`) is acceptable for a probe: a healthy LDAP
  returns the same `ENABLED`; an over-large directory that triggers
  LDAP size-limit returns `UNKNOWN_ERROR` either way (the size-limit branch
  at `PSUserService.java:1155-1158` throws `PSDirectoryServiceException`
  before any result processing), so observable status is unchanged.
