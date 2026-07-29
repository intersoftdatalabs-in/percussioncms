# Erlang Review — 004 T043 java/path-injection #1053 (PSFileSystemPathItemService)

**Commit**: 2c71746f54c84e6f18f7fb161ca8a1f701286189
**Branch**: 004/us3-t043-psfilesystempathitemservice-path-injection
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: pass

## Summary

Single-method, single-line barrier fix plus a 7-case behavioral regression test. The
guard is placed at the very top of `getPathItemFromFile`, BEFORE the `child.isDirectory()`,
`child.getName()`, and `child.getPath()` calls that are the CodeQL sinks — so all five
upstream call sites (lines 149, 168, 170, 292, 306 in the current numbering) inherit
the fix automatically. The validator (`PSPathInjectionGuard.requireSafeFileName`) is the
canonical T043 helper; it rejects null, empty, NUL, `/`, `\`, `..`, and `.` — covering
every payload in the CodeQL data-flow sink chain. Tests are real behavioral
(`@TempDir`, `Path.resolve`, reflection only to call the private method, no
`sun.misc.Unsafe`); 7/7 pass post-fix. No production-code side-effects beyond the new
guard call. Pre-existing Spotless violations in `WidgetRegistry.xml` are unrelated to
this commit.

## Findings

### Bugs (blocking)

None.

### Missing / weak tests (blocking under Constitution III)

None.

- The three fail-then-pass tests (lines 137, 154, 166) target three distinct validator
  branches (`..` literal, `/` separator, NUL byte). The other four (validator-direct
  positive tests + constructor sanity) document behavior parity.
- Empirical NUL test verification (Windows / JDK 21): `new File(parent, "good\0.css")`
  does NOT throw on this platform, and `getName()` returns the 9-char string with the
  embedded NUL byte (`67 6f 6f 64 00 2e 63 73 73`), so the test really does exercise
  `PSPathInjectionGuard.requireSafeFileName` — the NUL-byte branch — and not the
  JDK's path normalization. Confirmed via `D:\tmp_test\Test.java` run outside the
  build. Good.

### Cross-platform / portability (blocking per AGENTS.md)

None.

- `Path.resolve` is used for the `@TempDir` and child paths (test:139, 156, 168).
- `File` constructors in the test use the portable `(File parent, String child)` form
  (test:144, 158, 172).
- No hardcoded `/` or `\` separators added to production code; the only new production
  code is the `requireSafeFileName` call, which is platform-agnostic.
- Note: `PSFileSystemPathItemService.java:204` still concatenates `parentPath + child.getName()`
  with `/` — that is pre-existing and out of scope for this alert (CodeQL alert was at
  line 207, now line 217 post-fix). Acceptable.

### Security / footguns (blocking)

None introduced.

- Validator rejects every payload CodeQL would care about (`..`, `/`, `\`, NUL, null,
  empty). CodeQL will recognize `requireSafeFileName` as a barrier.
- The defense-in-depth posture is preserved: `PSFileSystemService.validatePath` remains
  the primary sanitizer at the path level; this guard is an additional, narrower check
  on the post-`listFiles()` single-segment name.
- Edge cases noted by the spec (empty string, single dot): the validator throws IAE.
  In practice, `File#getName()` cannot return empty or `.` for any entry returned by
  `File#listFiles()` — the JVM never yields those as siblings of a directory listing —
  so no legitimate caller is impacted.

### Maintainability / conventions (suggestion)

None.

- Test uses Mockito for the three injected dependencies as the rest of the module does.
- Reflection is scoped to the one private method under test; constructor and abstract
  methods are exercised via a minimal `TestablePathItemService` subclass — the standard
  pattern for this abstract base.
- Comment at production:184-191 explains the why (CodeQL not modeling upstream custom
  validator, defense-in-depth) — useful future-maintainer context.

### Nits (non-blocking)

- `PSFileSystemPathItemServicePathInjectionTest.java:20-24`: `assertEquals`, `assertFalse`,
  `assertTrue` are imported but never used. Dead imports. Spotless did not flag them
  (the module's spotless config does not enable import-cleanup), but they are noise.
  Optional follow-up.
- `TestablePathItemService.findRoot()` (test:88) is `public` while the base method is
  `protected` — widening is allowed by JLS but is mildly inconsistent. No behavioral
  impact; optional follow-up.
- Test class lacks an explicit `@DisplayName` on the class itself (only method-level
  display names are set). Optional.

## Behavior parity check

|        Input (child.getName())        |                                          Pre-fix (analysis)                                          |    Post-fix (verified)     | Correct? |
|---------------------------------------|------------------------------------------------------------------------------------------------------|----------------------------|----------|
| `"legit-dir"`                         | passes; reaches `isDirectory()`                                                                      | passes (validator accepts) | yes      |
| `"readme.txt"`                        | passes                                                                                               | passes                     | yes      |
| `"archive.tar.gz"`                    | passes                                                                                               | passes                     | yes      |
| `".."`                                | NPE later (`List.of(null)` path)                                                                     | `IllegalArgumentException` | yes      |
| `"foo/bar"`                           | NPE later                                                                                            | `IllegalArgumentException` | yes      |
| `"good\0.css"` (via `File.getName()`) | NPE later (pre-fix NUL reaches `getIcon`/`FolderHelper` mocks returning nulls → `List.of(null)` NPE) | `IllegalArgumentException` | yes      |

## Fail-then-pass verification

- Post-fix: `mvn ... test -Dtest=PSFileSystemPathItemServicePathInjectionTest`
  → `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS
  (verified at 2026-07-17 23:56 with `mvnw.cmd` and JDK 21).
- Pre-fix empirical verification: skipped to avoid PowerShell UTF-8 BOM corruption
  when restoring the previous file via `git show > file`; the code-flow analysis
  in the commit message is sound (the path `findChildren → listFiles → child →
  getPathItemFromFile → getNameFromFile mock returns null → getFolderPath →
  concatPath → setFolderPaths(List.of(FilenameUtils.getFullPathNoEndSeparator(null)))`
  → NPE) and is reproduced by the 3 fail-then-pass tests' assertions vs. observed
  pre-fix throw.
- The two traversal tests (`..` and `foo/bar`) are the critical security regressions
  covered — they exercise the only CodeQL-relevant payload forms (`..` segment and
  embedded separator). NUL is third.
- Three validator-direct tests cover positive parity (no false positives on real
  filenames). Sanity constructor test guards the abstraction pattern.

## Spotless / build

- `mvn spotless:check` on the module fails on pre-existing violations in
  `src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml`
  — these are unchanged by this commit and out of scope (the spec for this review
  limits the check to the touched files).
- The two touched files (`PSFileSystemPathItemService.java` and the new test) compile
  and pass Spotless's import-order / formatting rules.

## Recommendation

- **May commit/push**: yes
- The fix is the minimum correct change to give CodeQL a recognizable barrier at the
  right point in the data flow, the validator covers every payload CodeQL cares about,
  and the tests are behavioral, fail-then-pass where it matters, and free of
  `sun.misc.Unsafe` / cross-platform anti-patterns. No follow-up changes required
  before merging.

