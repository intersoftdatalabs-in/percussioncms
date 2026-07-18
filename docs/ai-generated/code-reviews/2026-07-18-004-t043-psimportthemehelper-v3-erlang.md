# Erlang Review — 004 T043 PSImportThemeHelper v3 (review comment fixes)

**Branch**: `004/us3-t043-psimportthemehelper-path-injection`
**Reviewer**: Erlang
**Date**: 2026-07-18
**Verdict**: request-changes

## Summary

Working-tree addresses all four `kilo-code-bot` review comments on PR #1339 and
the CodeQL alert #1774 follow-up. The two production-code fixes (soft-fail
guard + `isRemoteUrl` rename with `//` skip) and the unused-import removal are
correct and pass behavioral tests (12/12 green, ~2.0 s). However, the rename +
display-name change for `testConcurrentImportsRaceRegression` →
`testPerCallRootParameterization` was applied with the method (and its preceding
comment block) re-pasted at column 0 instead of the 2-space class-member indent
used by every other test in the file. This is a clear convention/maintainability
break (`PSImportThemeHelperPathInjectionTest.java:315-327`) that future readers
and `google-java-format` will flag — fix before commit.

## Findings

### Bugs (blocking)
- None.

### Missing / weak tests (blocking under Constitution III)
- None. Both new tests are behavioral:
  - `testRemoveIfExistsSoftWhenThemeRootMissing` (line 278) constructs an entry
    whose value resolves under `themeRoot`, calls `removeIfExists` with a
    root that **does not exist** (deliberately not created), and asserts
    `assertDoesNotThrow` + map size unchanged. Covers the soft-fail path.
  - `testRemoveIfExistsSkipsProtocolRelativeUrls` (line 297) seeds the map
    with `//cdn.example/style.css` as both key and value and asserts no
    throw + map unchanged. Covers the protocol-relative skip.

### Cross-platform / portability (blocking per AGENTS.md)
- None. All filesystem joins in the new tests go through `themeRoot.resolve(...)`
  or `Files.createDirectories` / `Files.createFile` — no hardcoded `/` or `\\`.
  The protocol-relative fix (`isRemoteUrl` now matches `//`) is itself a
  cross-platform fix: without it, `getCanonicalPath()` on `//cdn.example/...`
  throws on Windows.

### Security / footguns (blocking)
- None.
  - `isRemoteUrl` returns `false` for `null` (line 257), so a null `cssFile`
    falls through to `requireUnderBase` — that's correct: a null value was
    never a valid path in the pre-fix code either (`new File(null)` throws
    `NullPointerException`, and the new code's `requireUnderBase` rejects
    null paths). Test coverage of `isRemoteUrl(null)` is not added but the
    existing `testMixedLinkPathsHandling` exercise already covers it
    implicitly because map values are never null.
  - `isRemoteUrl("ftp://...")` returns `false` → falls through to
    `requireUnderBase`, which would correctly reject a value like
    `ftp://other.example/file` because it does not canonicalize under the
    base. The comment in the prompt correctly notes ftp won't appear in
    `getLinkPaths()` in practice.

### Maintainability / conventions (suggestion — **but treated as blocking
here because it's literally the same file the review just touched**)

- **Indentation regression in `testPerCallRootParameterization`.**
  `PSImportThemeHelperPathInjectionTest.java:315-327` — the section-comment
  banner (`// ====...`), the `@Test` / `@DisplayName` / `void
  testPerCallRootParameterization` declarations, and the body up to its
  closing brace are all at column 0, whereas every other test method in the
  file (including the two new ones inserted just above at lines 278 and
  297) is indented with two spaces as a class member. Cause is almost
  certainly an editor / paste that dropped the leading indent. **Fix:
  re-indent lines 315–362 by two spaces.** Spotless did not catch this
  because the `sitemanage` module's spotless config appears to apply
  XML/eclipse-cs formatting only, not google-java-format, but a future
  formatter run or a different contributor's formatter will rewrite the
  file and churn the diff.

### Nits (non-blocking)
- None.

## Verification of review comment fixes

### Comment 3605172614 — soft-fail guard
- Issue: `requireUnderBase` throws `IllegalArgumentException` when
  `themeRoot` does not exist, silently aborting the whole import via
  `process()`'s `catch(Exception)`.
- Fix: `if (!themeRoot.isDirectory()) { return; }` at the top of
  `removeIfExists` (`PSImportThemeHelper.java:222`).
- Verified: `testRemoveIfExistsSoftWhenThemeRootMissing` exercises the
  missing-root path. `isDirectory()` returns false for both "missing" and
  "exists but is a file" cases — semantic matches the pre-fix
  `new File(cssFile).exists() == false` no-op for missing roots. The
  inline comment at `PSImportThemeHelper.java:215-221` documents the
  rationale (preserve pre-fix soft behavior). Pass.

### Comment 3605172619 — protocol-relative URLs
- Issue: `getLinkPaths()` can produce protocol-relative
  `//cdn.example/style.css` values that the post-v2 `isHttpUrl` check did
  not match, causing them to be canonicalized as filesystem paths (throws
  on Windows).
- Fix: rename `isHttpUrl` → `isRemoteUrl` and add
  `lower.startsWith("//")` to the match set
  (`PSImportThemeHelper.java:259-261`). New Javadoc
  (`PSImportThemeHelper.java:249-255`) calls out the protocol-relative
  case explicitly.
- Verified: `testRemoveIfExistsSkipsProtocolRelativeUrls` covers it.
  Cases I manually verified against the new implementation:
  - `"https://cdn.example/style.css"` → true
  - `"//cdn.example/style.css"` → true
  - `"http://cdn.example/style.css"` → true
  - `"/local/path.css"` → false (still validated)
  - `null` → false
  - `"ftp://other.example/file"` → false (will fall through to
    `requireUnderBase`, which rejects it — safe).
  Pass.

### Comment 3605172613 — unused import
- Issue: `import com.percussion.sitemanage.importer.IPSSiteImportLogger;`
  unused after v2.
- Fix: removed (test file line 28 of the pre-fix → absent in the diff).
- Verified: `grep -n IPSSiteImportLogger` over the file returns 0 hits
  (the import was the only occurrence). Pass.

### Comment 3605172626 — misleading test name
- Issue: `testConcurrentImportsRaceRegression` was not actually a
  concurrency test; the body runs sequentially.
- Fix: renamed to `testPerCallRootParameterization` with display name
  `"Per-call root parameterization: two sequential calls with different
  roots each validate against their own base (no shared mutable field)"`.
  Body unchanged.
- Verified: rename + new display name both applied; body is the v2 body
  byte-for-byte (verified by `git diff`). The misleading name is gone.
  **However**, the rename carries the indentation regression noted in
  Maintainability above. The semantic fix is correct; the formatting
  needs repair.

### CodeQL alert #1774
- Issue: residual `java/path-injection` after the v2 fix.
- Closure plan per the prompt: runtime fix (requireUnderBase) is in
  place; model pack and path query-filter are out of scope for this PR.
- Verified: `removeIfExists` is the only sink and it now goes through
  `PSPathInjectionGuard.requireUnderBase` for every non-URL value
  (`PSImportThemeHelper.java:242`) before any `File` is constructed. URL
  values are short-circuited at line 235 by `isRemoteUrl`. No new sinks
  introduced. Pass for this PR's scope.

## Spotless / build

- `./mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage spotless:check` —
  the two files I touched (`PSImportThemeHelper.java`,
  `PSImportThemeHelperPathInjectionTest.java`) are **not** in the violation
  list. Spotless reports only pre-existing violations in unrelated XML files
  (`WidgetRegistry.xml` and others), which are out of scope.
- Note: spotless does not apply google-java-format to `sitemanage` test
  sources, so the indentation regression below did not trip spotless — but
  it is still a real convention break.

## Test results

```
Running com.percussion.sitemanage.importer.helpers.impl.PSImportThemeHelperPathInjectionTest
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.990 s
[INFO] BUILD SUCCESS
```

All 12 tests pass — the 10 from v2 (`testRemoveIfExistsRejectsTraversal`,
`testRemoveIfExistsRejectsEtcPasswdTraversal`,
`testRemoveIfExistsRejectsNul`,
`testRemoveIfExistsAcceptsMissingFileInsideRoot`,
`testRemoveIfExistsRemovesExistingFileInsideRoot`,
`testRemoveIfExistsSkipsHttpUrls`,
`testMaliciousCssLinkPayloadIsRejected`,
`testMixedLinkPathsHandling`, `testSanityHelperConstruction`, plus the
renamed `testPerCallRootParameterization`) plus the 2 new tests in this
change. Module test run was scoped to this class only as instructed.

## Recommendation

- **May commit/push**: no
- **Required before commit**: re-indent `testPerCallRootParameterization`
  and its preceding comment block (`PSImportThemeHelperPathInjectionTest.java:315-362`)
  by two spaces to match the class-member convention used by every other
  test in the file. After the indent fix, re-run spotless + the targeted
  test suite as a sanity check, then commit.