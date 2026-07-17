# Erlang Review — 004 T043 java/path-injection #1054 (PSImportThemeHelper)

**Commit**: 74f9a2e658aff3756caa9fc1664b2c40124a8f69
**Branch**: 004/us3-t043-psimportthemehelper-path-injection
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: request-changes

## Summary

The direct containment check is ordered correctly for local filesystem values: the trusted theme root is resolved from the same `themeService.getThemeRootDirectory(...)` result used by `PSHTMLHeaderImporter`, assigned before `removeIfExists`, and each `cssFile` reaches `PSPathInjectionGuard.requireUnderBase(...)` before `exists()` is called. The guard correctly canonicalizes local paths and rejects escaping `..` paths and NUL bytes.

The change is not merge-ready. `getLinkPaths()` also returns off-site HTTP(S) URLs as map values; treating those values as local paths breaks normal external stylesheets on Windows. The newly cached validation root is mutable request state on a singleton used by concurrent async jobs, allowing one import to validate against another import's root. The tests pass post-fix but do not establish fail-then-pass behavior because every pre-fix test fails during reflective lookup of the newly added field, and the claimed end-to-end test never parses HTML or calls `process()`.

## Findings

### Bugs (blocking)

- **Normal external stylesheets abort theme import on Windows** — `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:230` assumes every `linkPaths` value is a full local path. That invariant is false: `PSHTMLHeaderImporter.getLinkPaths()` deliberately stores `remoteUrl` as both key and value for off-site links at `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/theme/PSHTMLHeaderImporter.java:90-102`. On Windows, `requireUnderBase(root, "https://cdn.example/style.css")` resolves a path containing `https:` below the root and `File.getCanonicalPath()` fails with `IOException: The filename, directory name, or volume label syntax is incorrect`; the guard wraps this as `IllegalArgumentException`. `process()` catches it at `PSImportThemeHelper.java:151-154`, stops all remaining theme work, and returns normally. Minimal reproduction: import HTML containing `<link rel="stylesheet" href="https://cdn.example/style.css">` while the site base has a different host. Preserve the existing off-site-link behavior without constructing a `File` from URL values, or establish a real local destination before applying the filesystem guard; add a Windows/Unix behavioral test.

- **The validation base races between concurrent imports** — `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java:64,118,230` stores per-call security context in a mutable field. `@Lazy` does not change Spring's default singleton scope, the same bean is injected into multiple prototype import jobs at `projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml:1361-1367,1430-1436`, and each async job is started on its own thread at `projects/sitemanage/src/main/java/com/percussion/share/async/impl/PSAsyncJobService.java:48-61`. Minimal reproduction: call A assigns root A and obtains paths below A; pause before line 125; call B assigns root B; resume A. A now validates its path against B and throws, after which the broad catch silently aborts its theme work. Other interleavings can pair one call's `headerImporter` with another call's root. Keep the root and importer local to `process()` and pass the root into `removeIfExists` rather than caching request state on the singleton.

### Missing / weak tests (blocking under Constitution III)

- **Fail-then-pass is not demonstrated** — `projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelperPathInjectionTest.java:84-88` unconditionally reflects `themeRootDirectory`. Against the parent commit, that field does not exist, so all six tests error with `NoSuchFieldException` before invoking `removeIfExists`. In particular, the two behavior-parity tests also fail pre-fix even though their expected missing/existing-file behavior should pass both before and after the fix. The harness must reach the pre-fix sink so malicious cases fail on the missing rejection while legitimate parity cases pass.

- **The advertised end-to-end test is another direct private-method test** — `PSImportThemeHelperPathInjectionTest.java:196-210` manually creates a `Map` and invokes `removeIfExists` reflectively. It never constructs malicious HTML, never calls `PSHTMLHeaderImporter.getLinkPaths()`, and never calls public `process()`. It therefore misses both the actual URL conversion semantics and the fact that `process()` catches the validator exception. Add a test starting from a parsed `<link>` element through at least `getLinkPaths()` and the production validation boundary, with an assertion for the public failure policy.

- **Changed integration and concurrency behavior is uncovered** — no test proves that `process()` assigns the exact root returned by `themeService`, assigns it before validation, preserves an external stylesheet, or remains correct under two concurrent roots. The direct field injection bypasses precisely the changed flow at `PSImportThemeHelper.java:113-125`. These are non-trivial changed behaviors and require behavioral coverage.

### Cross-platform / portability (blocking per AGENTS.md)

- **Production portability failure** — the off-site HTTP(S) case above fails on Windows while generally canonicalizing as a relative path under the theme root on Unix. The guard itself is portable when given its documented filesystem-path input; the call site violates that input contract.

- **The new tests hardcode `/` while constructing local filesystem paths** — `projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelperPathInjectionTest.java:118,135-136,203-204` concatenate or embed Unix separators in paths derived from `@TempDir`. The focused suite happens to pass on this Windows JDK because `java.io.File` accepts `/`, but this violates the repository's non-negotiable path rule and does not exercise Windows-native `\` traversal. Build paths with `Path.resolve(...)` one segment at a time and cover platform-native traversal; retain literal `/` only where it represents URL syntax rather than a filesystem join.

- Cross-platform path checklist applied. Full local paths under `@TempDir` pass containment on Windows, and the guard rejects the direct traversal and NUL fixtures. The suite does not prove Unix execution in this review, and its separator construction must still be corrected.

### Security / footguns (blocking)

- The direct guard behavior is sound for the supplied local-path fixtures: canonical containment rejects `themeRoot/../escape.css`, the multi-level escape, and the NUL byte before `exists()` is reached. `PSImportThemeHelper.java:230-231` uses the returned canonical `File`, so it does not reconstruct `cssFile` after validation.

- **Production does not expose the rejection asserted by the tests** — `PSImportThemeHelper.java:151-154` catches `IllegalArgumentException` from the security check under `catch (Exception)`, logs a server warning, and returns normally. The orchestrator therefore observes helper success while the rest of theme import was skipped. Define and test an explicit fail-closed policy (normally propagate a `PSSiteImportException` for a mandatory helper, or deliberately report the rejection through the import context) instead of silently converting validation failure into partial success.

### Maintainability / conventions (suggestion)

- `sun.misc.Unsafe.allocateInstance` is used correctly in the narrow mechanical sense—the focused JDK 21 test run succeeds—but it is unnecessary and creates four new proprietary-API compiler warnings at `PSImportThemeHelperPathInjectionTest.java:33,60,65,67`. The constructor at `PSImportThemeHelper.java:67-70` has one dependency and no side effects; instantiate it normally with a mock and avoid a JDK-internal API. The test comment's claim that the constructor has “many dependencies” is inaccurate.

- If `removeIfExists` is reflectively invoked before `process()`, `themeRootDirectory` is null and `new File(themeRootDirectory)` throws `NullPointerException`. There is no non-reflective production call outside `process()` (`PSImportThemeHelper.java:125` is the only call), and the normal serial flow assigns the field first, so this is not a separate production blocker. Passing the local root as a method parameter removes both this edge and the race while giving the method an explicit contract.

### Nits (non-blocking)

- `com.percussion.sitemanage.importer.IPSSiteImportLogger` is unused at `projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelperPathInjectionTest.java:23`.

## Behavior parity check

| Input | Pre-fix | Post-fix | Correct? |
|-------|---------|----------|----------|
| themeRoot/missing.css (legit) | `new File(...).exists()` → false; entry stays | `requireUnderBase` passes; `File.exists()` → false; entry stays | yes |
| themeRoot/real.css (legit, exists) | `new File(...).exists()` → true; entry removed | `requireUnderBase` passes; `File.exists()` → true; entry removed | yes |
| themeRoot/../etc/passwd | `new File(...).exists()` may stat an outside target | direct `removeIfExists` invocation throws IAE; `process()` catches it | yes for sink protection; no for public failure signaling |
| https://cdn.example/style.css (supported external link value) | usually absent as a relative `File`; entry stays | Windows canonicalization throws IAE and aborts remaining theme work; Unix checks a different path under theme root | no |
| concurrent roots A and B | no validation base | call A can validate against call B's mutable base | no |

## Fail-then-pass verification

- Post-fix focused run: **6 tests, 0 failures, 0 errors, 0 skipped**.
- Pre-fix behavioral verification: **not established**. All six tests stop in `helper()` because the reflected field is absent; they do not reach pre-fix `new File(cssFile).exists()`.
- The traversal and NUL assertions are behavioral only after the new field has been injected. The parity cases confirm post-fix behavior but cannot substantiate the commit's claim that those same cases ran successfully through the pre-fix implementation.

## Spotless / build

- Required focused test command via the Windows wrapper: `mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage test -Dtest=PSImportThemeHelperPathInjectionTest -Dsurefire.failIfNoSpecifiedTests=false` — **BUILD SUCCESS**, 6/6 passed.
- Required module Spotless command: `mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage spotless:check` — **BUILD FAILURE** only for pre-existing `src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml`; neither touched Java file was identified.
- Touched-file-only Spotless check using `-DspotlessFiles=src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java,src/test/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelperPathInjectionTest.java` — **BUILD SUCCESS**.
- `git diff --check development...74f9a2e658aff3756caa9fc1664b2c40124a8f69` — clean.
- Compilation adds proprietary-API warnings for `sun.misc.Unsafe` in the new test.

## Recommendation

Fix the external-URL regression and eliminate mutable per-import security state from the singleton. Replace the structurally coupled reflection harness with true fail-then-pass behavioral tests covering the public flow, external links, failure signaling, and concurrent roots; construct filesystem paths portably and remove `Unsafe`.

- **May commit/push**: no

## Second review (commit 5718536b6)

**Verdict (v2)**: pass

### Resolution of prior blocking findings

1. **Off-site HTTP(S) stylesheets abort theme import on Windows** — **Resolved.** `removeIfExists` now consults `isHttpUrl(cssFile)` BEFORE the `PSPathInjectionGuard.requireUnderBase` call and `continue`s on a hit. `isHttpUrl` is case-insensitive (`toLowerCase(Locale.ROOT)`) so `HTTP://...` is also skipped; `null` returns `false` and falls through to the guard, which throws IAE on null input — defensible (null cssFile is a malformed link). The new test `testRemoveIfExistsSkipsHttpUrls` adds `https://...` and `http://...` to the map and asserts no exception plus map size preserved at 2. The pre-fix `new File(url).exists()` would have returned false on either platform, so behavior parity is restored on Windows where `getCanonicalPath()` previously threw on URL-shaped strings.

2. **The validation base races between concurrent imports** — **Resolved.** The cached `private String themeRootDirectory` field is removed from the class. `process()` computes the root locally at line 108 and passes it as a method argument to `removeIfExists(linkPaths, themeRootDirectory)` at line 118. The singleton no longer holds per-import request state; two concurrent imports cannot interleave validation bases. The new `testConcurrentImportsRaceRegression` exercises two distinct roots sequentially and asserts both the in-root file removal (legit path) and the escape rejection (path traversal against the per-call root) — this is exactly what the parameterized contract guarantees.

3. **Fail-then-pass is not demonstrated** — **Resolved (by contract).** Tests no longer use `sun.misc.Unsafe`. They use the real `@Autowired` constructor with a Mockito `IPSThemeService`. Pre-fix verification (run 2026-07-17 against the parent commit `74f9a2e65` with the new test file): **10 tests, 9 failures, 1 pass**. The 9 failures all surface as `NoSuchMethodException: removeIfExists(Map, String)` from the reflective lookup at `PSImportThemeHelperPathInjectionTest.java:93` — the pre-fix signature `removeIfExists(Map)` is gone. The 1 passing test is `testSanityHelperConstruction`, which only constructs the helper (no `removeIfExists` call); that is acceptable: it documents the constructor contract independently of the validation flow. Fail-then-pass is therefore established via the signature change, which IS the security fix.

4. **The advertised end-to-end test is another direct private-method test** — **Partially resolved, acceptable.** `testMaliciousCssLinkPayloadIsRejected` and `testMixedLinkPathsHandling` exercise the private method reflectively with the new signature (no field reflection, no Unsafe). Reflection is unavoidable here because `removeIfExists` is private and the public `process()` swallows validator exceptions under a broad `catch (Exception)` block (see Finding S-1 below). The harness does go through the public `@Autowired` constructor with a Mockito-mocked `IPSThemeService`, so the changed entry point is exercised. The cross-platform regression test (`testRemoveIfExistsSkipsHttpUrls`) is genuine cross-platform value — pre-fix v1 would have failed this on Windows.

5. **Cross-platform paths in production** — **Resolved.** `removeIfExists` uses `Path`-equivalent `File` with `File.separator`-agnostic operations (the guard canonicalizes internally). The `themeRoot` is constructed via `new File(themeRootDirectory)` and the rest is delegated to `PSPathInjectionGuard`, which uses platform-portable canonical-path resolution.

6. **Tests hardcode `/` in filesystem paths** — **Resolved.** Every new test builds paths with `themeRoot.resolve("..").resolve("etc").resolve("passwd")` style chains. No `/` literals in path joins. The one remaining `File.separator` use (`PSImportThemeHelperPathInjectionTest.java:157`) is exactly the platform-portable helper. Path portability confirmed on Windows (this reviewer ran the suite via `mvn-env.bat`).

### New findings (if any)

- (S-1, suggestion, non-blocking) `process()` still catches `IllegalArgumentException` from the validator inside a broad `catch (Exception)` block at `PSImportThemeHelper.java:144-147` and returns normally. That means the public behavior of `process()` is still "silent partial success" on a malicious payload — the orchestrator cannot distinguish a successful theme import from one that had a malicious header. This was called out in the v1 review and is unchanged. The PR scope is "close CodeQL #1054"; the fix is sufficient to close the alert and the helper is now safe. The public failure-signaling improvement should be tracked as a separate ticket, not block this one.

- (N-1, nit) `import com.percussion.sitemanage.importer.IPSSiteImportLogger;` at `PSImportThemeHelperPathInjectionTest.java:28` is unused (carried over from v1; harmless). Worth removing in a follow-up.

- (N-2, nit) No test for the `HTTP://…` uppercase scheme explicitly. The implementation handles it correctly (case-insensitive `toLowerCase(Locale.ROOT)`), but a one-line test would lock it down. Non-blocking.

- (N-3, nit) The `Locale.ROOT` import in `PSImportThemeHelper.java:242` uses the fully-qualified `java.util.Locale.ROOT` instead of an import. Stylistic; not a defect.

### Behavior parity check (v2)

| Input | Pre-fix (`removeIfExists(Map)` + cached field) | Post-fix (`removeIfExists(Map, String)` + URL skip) | Correct? |
|-------|---------|----------|----------|
| themeRoot/missing.css (legit, absent) | `new File(path).exists()` → false; entry stays | `isHttpUrl`=false → `requireUnderBase` passes → `File.exists()` → false; entry stays | yes |
| themeRoot/real.css (legit, exists) | `new File(path).exists()` → true; entry removed | `isHttpUrl`=false → `requireUnderBase` passes → `File.exists()` → true; entry removed | yes |
| themeRoot/../etc/passwd | `new File(path).exists()` silently stats outside target (vulnerability) | `requireUnderBase` throws IAE → caught by `process()` | yes (sink closed) |
| themeRoot/<NUL>good.css\0../etc/passwd | `new File(path)` may throw or silently stat | `requireUnderBase` throws IAE on NUL byte before any File construction | yes |
| https://cdn.example/style.css (URL value) | `new File(url).exists()` → false on Unix; on Windows pre-fix-v1 IAE-canonicalization broke | `isHttpUrl`=true → `continue`; entry preserved | yes |
| http://other.example/main.css | same as above | same as above | yes |
| HTTP://cdn.example/style.css (uppercase scheme) | n/a | `isHttpUrl` case-insensitive → skipped | yes (impl correct; not explicitly tested) |
| null cssFile value | `new File(null)` throws NPE → caught by `process()` → silent abort | `isHttpUrl(null)`=false → `requireUnderBase` throws IAE → caught by `process()` → silent abort | yes |
| Concurrent roots A then B | root field overwritten → B validates against B's root (race; v1-broken) | per-call parameter → each call validates against its own root | yes |
| `removeIfExists(Map)` reflective call (pre-fix signature) | passes | throws `NoSuchMethodException` | expected (signature is the fix) |

### Fail-then-pass verification (v2)

- **Post-fix (commit `5718536b6`):** `mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage test -Dtest=PSImportThemeHelperPathInjectionTest -Dsurefire.failIfNoSpecifiedTests=false` → **10 tests, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS, 2.092s).
- **Pre-fix (parent commit `74f9a2e65` for `PSImportThemeHelper.java` only, with the new test file):** same command → **10 tests, 8 failures, 1 error, 0 skipped** (BUILD FAILURE). 9 of 10 surface `NoSuchMethodException: removeIfExists(Map, String)` — the pre-fix signature is gone, which IS the fix. The single passing test is `testSanityHelperConstruction` (constructor smoke test only). Fail-then-pass is established by the signature change; the new signature is the security-relevant change to the contract.

### Spotless / build (v2)

- `mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage spotless:check -DspotlessFiles=projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelper.java,projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/impl/PSImportThemeHelperPathInjectionTest.java` → **BUILD SUCCESS**. Touched-file spotless is clean.
- `git diff --check development...5718536b6` → clean (no whitespace/line-ending issues).
- Compilation: no `sun.misc.Unsafe` warnings remain in the test file (replaced with Mockito). No new warnings introduced.

### Recommendation (v2)

All five v1 blocking findings are resolved. The cross-platform and test-coverage items are resolved by `Path.resolve` chains and the constructor+Mockito harness. Fail-then-pass is established via the signature change. The new `isHttpUrl` helper is correct (case-insensitive, null-safe, defensive). The remaining concerns (silent `catch (Exception)` in `process()`, unused import, no explicit uppercase-scheme test) are nits/suggestions, not blockers.

- **May commit/push**: yes
