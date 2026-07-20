# Erlang Review — fix/sitemanage-portable-path-test

**Reviewer**: Erlang (strict, independent, read-only, fresh subagent)
**Date**: 2026-07-20
**Branch**: `fix/sitemanage-portable-path-test`
**Base**: `origin/development` @ `8b3ce6cf0` (fetched successfully; HEAD == `origin/development`)
**Intent**: harden the `legitimateInRootPathIsAccepted` regression test in
`PSFileSystemServiceSecurityTest` so it does not depend on the OS path-text of
the Windows 8.3 vs. canonical `@TempDir` alias, while preserving the behavioral
intent (a legitimate in-root path is accepted by `getFile`/`getChildren`).

## Summary

This is a single-test, test-only change. It replaces a brittle
`File.getAbsolutePath().startsWith(...)` containment check (which fails on
Windows when `@TempDir` returns a short-name alias such as
`C:\Users\VIJAYA~1.BOD\...\junit-XXXX\` while `requireUnderBase` returns the
long-form canonical path) with `Files.isSameFile(target, resolved.toPath())`,
the canonical portable cross-platform comparison. The test now also uses
`Path.resolve` + `Files.createDirectories` / `Files.createFile` to create the
fixture (instead of `new File(root, "a/b/c")` + `mkdirs()`) and explicitly
creates the target file so `getFile` resolves a real existing file. The
behavioral assertion is **strengthened**, not weakened: the test now proves
`getFile` resolves to the same actual file as the input target (handles
short-name aliases, symlinks, and case-insensitive filesystems) and that the
parent directory actually exists on disk.

No production code is touched. Validation already run by the author on the
changed module (JDK 21, `mvn-env.bat -pl projects/sitemanage test`) reports
533 tests / 0 failures / 0 errors / 129 skipped; the changed file passes
Spotless and Checkstyle; `git diff --check` is clean. The pre-existing
`WidgetRegistry.xml` Spotless violation is unrelated to the diff.

## Scope

- Base: `origin/development` @ `8b3ce6cf0` (fetched successfully — note
  `RemoteException` from PowerShell error formatting, but the fetch itself
  succeeded per the subsequent `git diff origin/development...HEAD` returning
  no output: HEAD == origin/development post-fetch).
- Head: working tree on branch `fix/sitemanage-portable-path-test` (no new
  commits vs origin/development).
- Files: **1** changed
  - `projects/sitemanage/src/test/java/com/percussion/designmanagement/service/impl/PSFileSystemServiceSecurityTest.java`
- Prior reports (topic continuity):
  - `docs/ai-generated/code-reviews/2026-07-18-pr-1362-path-injection-residuals-erlang.md`
    (the production code under test — `PSFileSystemService.validatePath` +
    `getFile`/`getChildren` — was previously hardened; this review confirms
    the test change does not regress that defense)
  - `docs/ai-generated/code-reviews/2026-07-17-004-t043-psfilesystempathitem-erlang.md`
    (same author pattern: portable `Path`/`Files` + behavioral assertions)
- Memory patterns hit: `paths.startsWith-os-path-text`, `paths.use-Files.isSameFile`,
  `paths.Path.resolve-over-string-concat` (already represented in
  `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
  and root `AGENTS.md` → Cross-Platform File I/O & Paths).

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

(none)

## Cross-platform path review

Explicitly applied to the diff:

| Check | Result |
|-------|--------|
| Hardcoded `"/"` / `"\\"` filesystem joins in new test logic | **None.** Uses `Path.resolve("themes")`, `Path.resolve("site")`, `Path.resolve("page.html")`. |
| Unix-only absolute roots (`/tmp`, `/var`, …) in test | **None.** Uses JUnit `@TempDir java.nio.file.Path root`. |
| Windows-only paths in test | **None.** |
| `File.pathSeparator` misuse | **N/A** |
| Path string equality / regex assuming Unix shapes | **Removed.** Replaced `resolved.getAbsolutePath().startsWith(root.toFile().getAbsolutePath())` with `Files.isSameFile(target, resolved.toPath())`. The latter is filesystem-fidelity-aware (handles short-name aliases, symlinks, case-insensitive volumes). |
| Case-sensitive filesystem assumptions | **None.** `Files.isSameFile` is case-insensitive on Windows and case-sensitive on POSIX, matching the OS semantics. |
| Line-ending assertions requiring `\n` only | **N/A** (no multi-line file content asserts). |
| Unix-only scripts | **N/A** (test-only change). |
| Hardcoded OS path separators in API calls | **Acceptable portability note.** `root.relativize(target).toString()` produces the platform separator (`/` on Unix, `\` on Windows). The service's `validatePath` explicitly splits on `[/\\\\]` (production source `projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:178`), so the API accepts both. This is intentional test fidelity: the test now exercises the exact platform-native path string a caller using `Path.relativize(...).toString()` would feed in. |
| Assertion strength | **Strengthened.** Old: `startsWith` (string-level prefix). New: `Files.isSameFile` (resolves both paths through the filesystem and confirms they refer to the same inode). Old `parent.exists()` is replaced by `Files.exists(parent)` — semantically equivalent but portable. |

**Cross-platform path review: no issues.** The change moves the test from a
known Windows-fragile pattern (literal prefix on raw `getAbsolutePath()`) to
the canonical portable comparison (`Files.isSameFile`) recommended by
`AGENTS.md` → Cross-Platform File I/O & Paths and the Erlang cross-platform
checklist. Behavior is preserved (and slightly tightened): the test still
verifies that a legitimate in-root path resolves to the intended file and that
the parent directory exists.

## Behavioral assertions (preserved / strengthened)

| Aspect | Pre-fix | Post-fix |
|--------|---------|----------|
| `getFile` returns non-null for in-root path | `assertNotNull(resolved)` ✓ | `assertNotNull(resolved)` ✓ |
| `getFile` resolves to the intended file | `assertTrue(resolved.getAbsolutePath().startsWith(root.toFile().getAbsolutePath()))` (string prefix, Windows-fragile under 8.3 aliases) | `assertTrue(Files.isSameFile(target, resolved.toPath()))` (filesystem-fidelity, portable) |
| Target file actually exists on disk | Not explicitly created; assertion was on the path text | `Files.createFile(target)` then `Files.isSameFile` — the resolved File must point at a real file, not just share a string prefix |
| `getChildren` returns non-null for in-root path | `assertNotNull(children)` ✓ | `assertNotNull(children)` ✓ |
| Parent directory exists | `parent.exists()` via `new File(root, "themes/site")` | `Files.exists(parent)` via `target.getParent()` |
| Constructor / path handling | `new PSFileSystemService(root.toString())` ✓ | unchanged ✓ |
| Other tests in class (`dotDotSegmentAtAnyDepthIsRejected`, `bareDotDotSegmentIsRejected`, `pathEscapingViaCanonicalizationIsRejected`, `renameFolderRejectsBadNewName`) | unchanged | unchanged |

## What looks good

1. **Minimal blast radius.** One test method edited; no production code, no
   other test files, no pom changes.
2. **Correct portable idiom.** `Files.isSameFile(Path, Path)` is the
   recommended approach for cross-platform "is this the same file?" checks;
   matches `AGENTS.md` → Cross-Platform File I/O & Paths and the Erlang
   cross-platform checklist.
3. **No assertion weakening.** The new `Files.isSameFile` check is strictly
   stronger than the old `startsWith` check (proves inode equivalence, not
   just textual prefix).
4. **Improved fixture realism.** `Files.createFile(target)` makes the test
   exercise the real "file exists" branch of `getFile` rather than relying on
   a side-effect of `getFile`'s resolution.
5. **Imports tidied.** Old imports (e.g. `java.io.File`, `java.util.List`) are
   re-grouped with the new `java.nio.file.Files` / `java.nio.file.Path`
   imports in conventional JUnit 5 / Spotless order — no dead imports.
6. **Aligns with adjacent test style.** Other T043 tests in this module
   (`PSFileSystemPathItemServicePathInjectionTest`, `PSCloudServicePathInjectionTest`,
   `PSRenderLinkServicePathInjectionTest`, `PSThemeServiceSecurityTest`) all
   use `@TempDir` + `Path` + `Files` per the prior Erlang reviews.
7. **Author transparency.** The branch name, the Javadoc header referencing
   "spec 004 / T043d, PR #1210", and the in-method intent comments make the
   defense boundary clear to future maintainers.
8. **Build hygiene confirmed by author.** Spotless + Checkstyle pass on the
   changed file; full module test run is green; `git diff --check` is clean.
   The pre-existing `WidgetRegistry.xml` Spotless violation is unrelated to
   this diff and was previously documented as out-of-scope in the T043
   PSFileSystemPathItem review.

## Required before commit / PR

None. May commit and open PR.

## Optional follow-ups (non-blocking, not raised here as findings)

- Consider adding `@DisplayName` annotations on the test class / methods for
  CI readability (matches the convention already used in
  `PSFileSystemPathItemServicePathInjectionTest`). Out of scope for the bug
  being fixed.
- A thin positive-path assertion that exercises `getFile` with a **leading
  slash** input (`"/themes/site/page.html"`) would document the
  `path.startsWith("/") ? path.substring(1) : path` branch in
  `PSFileSystemService.getFile` (`projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSFileSystemService.java:274`).
  The current legitimate-path test uses `root.relativize(target).toString()`
  which never yields a leading `/`. Non-blocking; existing `dotDot*` and
  `pathEscapingViaCanonicalization` tests cover the security boundary, just
  not this benign normalization.

## Reviewer independence

This review was performed by a fresh Erlang subagent session independent of
the implementer. Pattern memory and prior topic reports were loaded before
reading the diff (see Scope). The implementer is not the reviewer.

## Memory patterns hit

- `paths.startsWith-os-path-text` — exactly the smell removed by this diff.
- `paths.use-Files.isSameFile` — exactly the recommended replacement applied.
- `paths.Path.resolve-over-string-concat` — applied for fixture construction.

No new generalized patterns were discovered that are not already represented
in `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
or root `AGENTS.md`. Per the Erlang agent's "Memory touch" guidance, this
review does **not** modify `patterns.md`.
