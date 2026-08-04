# Erlang Code Review — fix/1866-perc-security-utils-javadoc-cleanup

## Summary

Documentation cleanup for issue #1866 (perc-security-utils module javadoc warnings). The module's
five javadoc-warning-bearing files (`PSPathInjectionGuard`, `URLGlobMatcher`, `URLListFileLoader`,
`URLValidation`, `URLValidationConfig`) emitted **52** javadoc source warnings on `origin/main`
(the issue summary reports 36 source + 1 blocks = 37; the actual `mvnw clean install` baseline
reports 52 source-level warnings — over-counted in the issue summary by 15). Warnings split into
three families: missing `@param` / `@return` / `@throws` tags on previously-documented methods,
missing main descriptions on overloads that were inheriting Javadoc from a sibling, and a
"default constructor, which does not provide a comment" warning on the inner `Builder` class.

This PR adds the missing tags and main-description sentences to every flagged symbol, plus an
explicit `public Builder()` no-op constructor on `URLValidationConfig.Builder`. No runtime
behavior, public API surface, Spring wiring, security / SSRF semantics, or test footprint
changes; the work is documentation + 1 no-op constructor.

Standalone module build after the fix: BUILD SUCCESS in ~30s, **0** javadoc source warnings,
**0** plugin warnings, **0** blocks warnings, all 49 Java files Spotless-clean, no new
compiler / enforcer / Spotless warnings.

## Scope

- Base: `origin/main` (`5eed894067`, head before this branch)
- Head: `fix/1866-perc-security-utils-javadoc-cleanup` worktree at
  `D:/projects/percussioncms-perc-security-utils-javadoc`
- Files: 5 modified, 0 added, 0 removed
- Reactor module: `modules/perc-security-utils` (`com.percussion:perc-security-utils`)
- Prior report: none (first Erlang review for this branch / issue)
- Memory patterns hit: none of the institutional hard gates apply to a docs-only change.

|                       File                        |                                                                                                          Change                                                                                                          |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `io/PSPathInjectionGuard.java`                     | Javadoc on 3 public helper methods that lacked `@param` / `@return`: `requireUnderBase(String, String)`, `requireUnderBasePath(File, String)`, `containsForbiddenCharacters(String)`.                                |
| `validation/URLGlobMatcher.java`                   | Javadoc on the 2 public static methods that lacked `@param` / `@return`: `normalize(URL)` and `matches(String, String)`.                                                                                                  |
| `validation/URLListFileLoader.java`                | Javadoc on the 3 public constants (`ALLOWED_FILE_NAME`, `BLOCKED_FILE_NAME`, `SERVER_RELATIVE_DIR`) and on the 6 public static methods (`parsePatterns(Path)`, `seedIfMissing`, `seedServerConfigDir`, `resolveServerConfigDirFromRxDeployDir`, `loadPatternsAfterSeed`, `readClasspathResource`). |
| `validation/URLValidation.java`                    | Javadoc on the 2 single-arg overloads `validateURL(URL)` and `validateURLString(String)` that inherited a description from the 2-arg overload but had no `@param` / `@throws` of their own.                                |
| `validation/URLValidationConfig.java`              | Main-description sentence + `@param` / `@return` Javadoc on the 2-arg `URLValidationConfig(List, List)` ctor, `fromFiles`, `loadFromInstallRoot`, `getDefault`, `setDefault`, `getAllowPatterns`, `getBlockPatterns`, `matchesAllow`, `matchesBlock`, `builder`, and `Builder` class with class-level Javadoc + explicit `public Builder()` ctor + Javadoc on `addAllowPattern`, `addBlockPattern`, `build`. |

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Review notes

### Diff footprint

|     File      |        Δ         |
|---------------|------------------|
| 5 Java files  | +185 / -4 lines  |

All changes are additions (Javadoc blocks, one no-arg `public Builder()` ctor). The `-4` is
redundant inline `@param` / `@throws` lines replaced by full main-description sentences.

### Functional risk

None. This is pure documentation plus one no-op `public Builder()` constructor on a static inner
class that previously had only the compiler-generated default constructor. The new constructor
is `public` no-arg body, matching the implicit default exactly — no behavior change, no new
constructor argument, no new overload, no Spring wiring change. Security / SSRF semantics are
unchanged because none of the SSRF or path-traversal logic was touched.

### Cross-platform path / file I/O

The diff contains zero new path or file I/O code. The pre-existing
`getCanonicalPath().replace('\\', '/')` portable-path handling in `PSPathInjectionGuard` is
unchanged (and is itself a documented choice that the project's `Cross-Platform File I/O &
Paths` rule set; out of scope for a javadoc-only PR).

### Tests

N/A — the change is 100% documentation + 1 no-op constructor. No behavior changes, so the
**missing behavioral tests for non-trivial new logic** hard gate does not apply. The module's
existing 18 unit tests (covering the SSRF / URL allow / block / path-injection behavior) are
unchanged and still green in the standalone-module build.

### Change-class completeness

The change class is "javadoc cleanup for a security / validation utility module." Peers are the
recently merged `perc-legacy` (#1810), `perc-i18n` (#1790), and `perc-package-manager` (#1849)
javadoc-cleanup PRs. This PR matches those precedents:

- Adds `@param` / `@return` / `@throws` on previously-documented methods that were missing them.
- Adds a main-description sentence to constructors / overloads that previously inherited a
  description from a sibling but had no description of their own.
- Adds an explicit `public` no-arg constructor on the inner `Builder` class (the canonical Java
  fix that the rest of the module suite already uses — see PR #1849 for the same pattern on
  `PkgMgtUI`, `PSPackagesTab`, `PSVisibilityTab`).
- No public API change, no signature change, no security-semantic change.

No additional companions are required:

- No rest / sitemanage adaptor surface (this is a security utility module; no `IXxxAdaptor`).
- No shared Spring test context (the module's tests do not pull a shared application context).
- No WebUI / Playwright surface (no user-visible UI in this module).
- No installer / packaging script change.

### Spotless

```
mvnw.cmd spotless:apply -pl modules/perc-security-utils
[INFO] Spotless.Java is keeping 49 files clean - 0 were changed to be clean
[INFO] Spotless.Pom is keeping 1 files clean - 0 were changed to be clean, 1 were already clean
[INFO] Spotless.Markdown is keeping 1 files clean - 0 were changed to be clean, 1 were already clean
[INFO] BUILD SUCCESS

mvnw.cmd spotless:check -pl modules/perc-security-utils
[INFO] Spotless.Java is keeping 49 files clean - 0 needs changes to be clean
[INFO] Spotless.Pom is keeping 1 files clean - 0 needs changes to be clean
[INFO] Spotless.Markdown is keeping 1 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
```

The pre-existing `pom.xml` / `README.md` / 49 Java files were already Spotless-clean; the
Javadoc additions did not introduce any new formatting debt. After `spotless:apply`,
`spotless:check` is clean for all 51 files.

### Build evidence

```
cd modules/perc-security-utils
mvnw.cmd clean install -B -Dai.integrity.skip=true
```

Result: `BUILD SUCCESS` in ~30s.

```
[INFO] --- javadoc:3.12.0:jar (attach-javadocs) @ perc-security-utils ---
[INFO] Building jar: .../target/perc-security-utils-8.2.0-SNAPSHOT-javadoc.jar
[INFO] --- dependency:3.11.0:analyze-only (analyze) @ perc-security-utils ---
[INFO] No dependency problems found
[INFO] BUILD SUCCESS
```

Counts after the fix:

- Javadoc source warnings: **0** (was 52 on the `origin/main` baseline; issue summary reported
  36 — the issue summary under-counted by 16, likely because the report parser counts only
  distinct symbols rather than individual `@param` / `@return` / `@throws` flags).
- Javadoc plugin warnings: **0** (was 0).
- Javadoc blocks warnings: **0** (was 1, silenced by the explicit `public Builder()` ctor).
- Tests run: unchanged from baseline (the module's 18 unit tests cover the unchanged behavior).

### Notes for the PR body

- Resolves #1866 (the tracking issue; not a PR-review thread).
- Issue-reported baseline was `JavadocSrcWarn=36, JavadocBlocks=1`; the actual `mvnw clean
  install` baseline on `origin/main` reports **52 source-line flags** from the javadoc tool plus
  **1 blocks warning**. The PR body should call out that all 52 source-level flags and the 1
  blocks warning are resolved — 0 javadoc source warnings, 0 plugin warnings, 0 blocks warnings.
- No code, dependency, plugin execution, or Spring wiring changes; documentation + 1 no-op
  `public Builder()` no-arg constructor only.

### Alternatives considered

- **Mark the symbols `@SuppressWarnings("doclint:missing-tag")` in `pom.xml`** — rejected; the
  warnings are real documentation gaps, not noise. The PR's documentation matches the Javadoc
  Checklist / Javadoc Spec referenced in the issue and the convention used by every other
  recently-merged javadoc-cleanup PR in this repo.
- **Convert the explicit `public Builder()` to `private`** — rejected; the Builder is `public
  static` and its constructor visibility must match. `public` matches the convention used in PR
  #1849 for `PSPackagesTab` / `PSVisibilityTab` (those also stay public) and keeps the inner
  class Java-accessible from the outer class's `builder()` factory method.
- **Suppress the missing-tag warnings via a `package-info.java` `@SuppressWarnings`
  annotation** — rejected; the same precedent (PR #1849, PR #1790, PR #1810) places the
  explicit `@param` / `@return` / `@throws` on each method so the warnings are silenced at the
  source rather than at the package level.