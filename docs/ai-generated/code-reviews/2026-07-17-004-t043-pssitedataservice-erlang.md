# Erlang Review — 004 T043 java/path-injection #1058 (PSSiteDataService)

**Commit**: 54953e346a03ea7bb73845057ad4dea9a2a72819
**Branch**: 004/us3-t043-pssitedataservice-path-injection
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: pass

## Summary

Fix closes CodeQL alert #1058 (`java/path-injection`) at
`PSSiteDataService.java:503` by adding
`PSPathInjectionGuard.requireSafeFileName(...)` on both `oldSiteName`
and `newSiteName` **before** any `File` construction in
`updateThumbnailCache`. The validator is the shared helper introduced
by commit `78f0cca57` (PR #1207) and reused by PSThemeService (PR

# 1208), PSRegionCSSFileService (PR #1209), PSFileSystemService

(PR #1210), PSLocalCommandHandler (PR #1261), and PSSiteConfigUtils
(PR #1296) — the pattern is the canonical T043 fix. The 9 new
behavioral tests invoke the private `updateThumbnailCache` via
`sun.misc.Unsafe.allocateInstance` to bypass the 21-arg constructor
and exercise the validator-first contract.

## Findings

### Bugs (blocking)

- `<none>`

The fix at `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java:498-499`
correctly runs **both** validator calls **before** the `new File(...)`
construction at lines 501 and 507. Validator-first ordering is the
critical invariant for path-traversal defense, and it is satisfied.

### Missing / weak tests (blocking under Constitution III)

- `<none>`

`PSSiteDataServicePathInjectionTest` has 9 tests:

| # |                 Test                 |                       Behavior verified                        |
|---|--------------------------------------|----------------------------------------------------------------|
| 1 | `testRejectsTraversalInOldSiteName`  | `"../../../etc/passwd"` → IAE with message containing `"path"` |
| 2 | `testRejectsTraversalInNewSiteName`  | `"../escape"` → IAE                                            |
| 3 | `testRejectsForwardSlashInSiteName`  | `"foo/bar"` → IAE (both positions)                             |
| 4 | `testRejectsBackslashInSiteName`     | `"foo\\bar"` → IAE                                             |
| 5 | `testRejectsAbsolutePathInSiteName`  | `"/etc/passwd"` → IAE                                          |
| 6 | `testRejectsNullAndEmptySiteName`    | null + empty in both positions → IAE                           |
| 7 | `testRejectsNulInSiteName`           | `"good\0site"` → IAE (both positions)                          |
| 8 | `testRejectsSingleDotOrDotDot`       | `"."` and `".."` → IAE (both positions)                        |
| 9 | `testAcceptsSiteNameWithPunctuation` | `"MySite"`, `"my-site_v1.0"` → no IAE (sanity)                 |

All assertions use `assertThrows(IllegalArgumentException.class, ...)`
— behavioral, fail-then-pass. The single sanity test exercises the
helper directly, which is appropriate for confirming the contract
without file I/O. Test 1 additionally checks the message text
contains `"path"`, which is reasonable (not vacuous). No Mockito or
PowerMock for static methods is used.

### Cross-platform / portability (blocking per AGENTS.md)

- `<none>`

Test uses only `sun.misc.Unsafe.allocateInstance` (JDK API, works on
Windows/Linux/macOS), `Class.getDeclaredMethod`/`setAccessible`, and
`Method.invoke`. No filesystem I/O, no path string construction, no
OS-specific assumptions. Production change also does not introduce
any new path-string handling — the existing `File.separator`-based
`PAGE_IMAGE_CACHE_DIR` constant is unchanged.

The single mild concern is that `Unsafe.allocateInstance` triggers 3
`javac` warnings: `sun.misc.Unsafe is internal proprietary API and may
be removed in a future release`. These are **warnings only**, not
errors, and the existing module still builds clean. Acceptable.

### Security / footguns (blocking)

- `<none>`

The validator runs **first** in `updateThumbnailCache` before any
`File(...)` is constructed. The two arguments are validated
independently, so a malicious `oldSiteName` is caught even if
`newSiteName` is benign (and vice versa). `validateSiteProperties` at
`PSSiteDataService.java:669-702` only checks for null/empty name and
duplicate names — it does **not** validate against path-traversal,
so the new `requireSafeFileName` calls are the **only** path-traversal
defense in this call chain (defense at the sink, good).

Edge-case considerations reviewed:

- **Unicode normalization (`．．` full-width dots, U+FF0E)**: not
  rejected by `String.contains("..")`. However, these are not path
  separators on any common OS, so the resulting `File` would be a
  literal directory named `．．` inside the cache dir — not an escape.
  This is a property of the shared `PSPathInjectionGuard`, not a
  regression introduced by this commit. Pre-existing.
- **URL-encoded sequences (`%2e%2e`)**: not rejected, but the sink is
  `new File(String)`, which does **not** URL-decode. The resulting
  `File` would be a literal directory named `%2e%2e`. Not a traversal
  risk. Pre-existing characteristic of the helper.
- **Symlink escape via `renameTo`**: out of scope for this CodeQL
  alert (alert is on `File` construction, not on the rename
  semantics). The validator already prevents traversal via the name
  string itself.

### Maintainability / conventions (suggestion)

- The use of `sun.misc.Unsafe.allocateInstance` to instantiate
  `PSSiteDataService` (which has only a 21-arg constructor) is **new
  to the sitemanage test suite**. Prior T043 tests in this module
  (`PSThemeServiceSecurityTest`, `PSRegionCSSFileServiceSecurityTest`,
  `PSFileSystemServiceSecurityTest`) use either no-arg or simple
  constructors. The `Unsafe` approach is acceptable and documented
  in the test class Javadoc; future T043 fixes against
  constructor-heavy services may legitimately follow this pattern.
  Not blocking.
- The fully qualified `org.junit.jupiter.api.Assertions.assertDoesNotThrow(...)`
  on lines 176/178 is redundant — the static import `assertThrows` is
  already present, so the convention would be to add a static import
  for `assertDoesNotThrow` and use it unqualified. Minor style nit.

### Nits (non-blocking)

- The `updateThumbnailCache` log message at
  `PSSiteDataService.java:487-490` is emitted **before** the
  validator runs. A malicious payload is therefore logged once
  before being rejected. This is a **debugging convenience**, not a
  leak — `log.info` with site names is consistent with the rest of
  the service — but in a SIEM context it would record the rejected
  payload. Acceptable for `INFO` level.
- The `ExceptionInInitializerError` thrown from the static block on
  lines 54-65 will produce a less-than-friendly error if the JVM
  ever denies reflective access to `Unsafe`. JDK 21 still permits it
  by default; this is forward-looking fragility, not a current bug.

## Behavior parity check

The validator is added **before** any state change. Pre-fix, the
private method would have built `new File(...)` strings using the
attacker-controlled name and then attempted a `renameTo`; in a test
environment the pre-fix call would proceed past `new File(...)` to
`PSServer.getRxDir()` (which in unit-test scope typically throws
because the deploy dir / static `RX_DIR` is uninitialized — see
`PathUtils.getRxDir(null)` at `modules/utils/src/main/java/com/percussion/utils/io/PathUtils.java:66`).

|     Input (`old`, `new`)     |                        Pre-fix                         |                     Post-fix                      |              Correct?               |
|------------------------------|--------------------------------------------------------|---------------------------------------------------|-------------------------------------|
| `"MySite", "MySiteRenamed"`  | throws from `PSServer.getRxDir()` (env-dependent)      | throws from `PSServer.getRxDir()` (env-dependent) | yes — happy path unchanged          |
| `"../../../etc/passwd", "x"` | File built, then throws at PSServer                    | IAE from validator at line 498                    | yes — reject before sink            |
| `"foo/bar", "goodSite"`      | File built, then throws at PSServer                    | IAE from validator                                | yes                                 |
| `"goodSite", "foo\\bar"`     | File built (Windows-only concern)                      | IAE from validator                                | yes                                 |
| `"/etc/passwd", "goodSite"`  | File built (absolute), then throws at PSServer         | IAE from validator                                | yes                                 |
| `null, "goodSite"`           | File built with "null" string, then throws at PSServer | IAE from validator                                | yes                                 |
| `"goodSite", null`           | File built with "null" string                          | IAE from validator                                | yes                                 |
| `".", "goodSite"`            | File built for "."                                     | IAE from validator                                | yes                                 |
| `"..", "goodSite"`           | File built for ".." — **would have escaped cache dir** | IAE from validator                                | yes — closes the actual CodeQL sink |
| `"good\0site", "x"`          | File built with NUL, downstream would throw on rename  | IAE from validator                                | yes                                 |

## Fail-then-pass verification

Fail-then-pass was **not** executed live (read-only review mode), but
the test class is structured so that the post-fix `IllegalArgumentException`
is thrown at `PSSiteDataService.java:498-499` — strictly **before**
the `PSServer.getRxDir()` call on line 503. Reverting the fix would
cause `updateThumbnailCache` to fall through to the `new File(...)`
construction; with `Unsafe.allocateInstance` skipping the 21-arg
constructor and field initializers, the pre-fix behavior would be
whatever `PSServer.getRxDir()` throws in a test environment (typically
`NullPointerException` or `IllegalStateException` from
`PathUtils.autoDetectRxInstallDir` at
`modules/utils/src/main/java/com/percussion/utils/io/PathUtils.java:163-184`
when the deploy dir system property is unset). The author committed
to "8/9 new tests fail pre-fix" in the message body; the structure of
the 8 fail-tests is consistent with that claim — they assert
`IllegalArgumentException`, which is only thrown by the new
validator. The 9th test (`testAcceptsSiteNameWithPunctuation`) is
sanity-only and would pass pre-fix as well.

## Spotless / build

- `mvnw.cmd -Dai.integrity.skip=true -pl projects/sitemanage spotless:check`:
  Spotless reports a single pre-existing violation in
  `src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml`
  (XML formatting). **Neither of the touched files
  (`PSSiteDataService.java`, `PSSiteDataServicePathInjectionTest.java`)
  appears in the violations list** — formatting on the change is clean.
- `mvnw.cmd -Dai.integrity.skip=true -pl projects/sitemanage test -Dtest=PSSiteDataServicePathInjectionTest -Dsurefire.failIfNoSpecifiedTests=false`:
  **9/9 tests pass**, 0 failures, 0 errors, 0 skipped. Three
  `sun.misc.Unsafe` API warnings emitted by `javac` (expected and
  acceptable).

## Recommendation

- **May commit/push**: yes

The change is a textbook T043 path-injection fix: validator-first
ordering at the sink, shared helper from `modules/perc-security-utils`
(per spec T043 "single helper shared across call sites"), behavioral
regression tests with reflective instantiation to keep the test
self-contained against the 21-arg constructor. No bugs, no missing
tests, no portability regressions. The two non-blocking nits (Unsafe
uniqueness in sitemanage tests; log-before-validate) do not warrant
blocking the PR. Recommend merge and a follow-up if the team wants to
unify test patterns across the T043 series.
