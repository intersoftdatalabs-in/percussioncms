# Erlang Review — PR #1362 T043 path-injection residuals batch

**Reviewer**: Erlang (strict, independent, read-only)  
**Date**: 2026-07-18  
**PR**: #1362  
**Branch**: `004/us3-t043-path-injection-residuals`  
**Head**: `6d8cce1e67de6aeb859da7c4d7cefc004a4b8f7c`  
**Prior review head**: `b00558f0f34a5a24d9569df3d43dc7ca480fe712`  
**Base**: `origin/development`  
**Worktree**: `/home/nate/projects/percussioncms.worktrees/004-java-path-injection-batch`  
**Intent**: multi-pass `java/path-injection` residual clearance (~14 product files) + consolidated path query-filters + sink-line suppressions; re-review of Erlang gate-close commit

## Summary

PR #1362 closes a large residual `java/path-injection` thrash set by combining real structural hardening (`PSPathInjectionGuard.requireUnderBase` / `requireSafeFileName` / `safeThemeFolder`) with whole-file path query-filters and many sink-line `// codeql[java/path-injection]` markers. The strongest structural work is in `PSThemeService` (create/cache/clearCache), `PSFileSystemService` (getFile/getChildren), `PSSiteDataService` (thumbnail cache rename), `PSRenderLinkService`, and `PSCloudService`.

**Re-review (tip `6d8cce1e67`)**: all four prior blocking findings are **fixed**. Session path/URL consistency is restored via a single `safeSessionSegment` helper used by relative path, cache file, and clearCache; behavioral tests cover clearCache, `generateThumbUrl` rejection, and region-CSS `requireUnderBase` containment. Residual items are suggestions/nits only. Gate: **approve**.

## Scope

- Base: `origin/development`
- Head: `004/us3-t043-path-injection-residuals` @ `6d8cce1e67`
  - `96d9379351` structural
  - `b00558f0` suppressions docs
  - `6d8cce1e67` session segment fix + behavioral tests (this re-review)
- Files reviewed (from PR diff + fix pack): product/sitemanage security paths + new tests
  - `.github/codeql/codeql-config.yml`
  - `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md`
  - `projects/sitemanage/.../AssetAdaptor.java`
  - `projects/sitemanage/.../PSAssetService.java`
  - `projects/sitemanage/.../PSCloudService.java`
  - `projects/sitemanage/.../PSFileSystemService.java`
  - `projects/sitemanage/.../PSWebResourcesRestService.java`
  - `projects/sitemanage/.../PSRenderLinkService.java`
  - `projects/sitemanage/.../PSFileSystemPathItemService.java`
  - `projects/sitemanage/.../PSSiteDataService.java`
  - `projects/sitemanage/.../PSRegionCSSFileService.java`
  - `projects/sitemanage/.../PSThemeService.java`
  - `projects/sitemanage/.../PSThemeServiceSecurityTest.java`
  - `projects/sitemanage/.../PSCloudServicePathInjectionTest.java` (**new**)
  - `projects/sitemanage/.../PSRenderLinkServicePathInjectionTest.java` (**new**)
  - `system/.../PSLocalCommandHandler.java`
- Prior reports (topic continuity, earlier T043 slices):  
  `docs/ai-generated/code-reviews/2026-07-17-004-t043-pssitedataservice-erlang.md`,  
  `docs/ai-generated/code-reviews/2026-07-17-004-t043-psfilesystempathitem-erlang.md`,  
  `docs/ai-generated/code-reviews/2026-07-17-004-t043-pscssparser-erlang.md`,  
  `docs/ai-generated/code-reviews/2026-07-18-004-t043-psimportthemehelper-v3-erlang.md`
- Memory patterns hit:  
  `tests.missing-behavioral` / structural-only coverage,  
  `paths.containment-trusted-root`,  
  `security.filename-only-not-path-traversal`,  
  `security.broad-path-excludes-need-runtime-tests`,  
  cross-platform path checklist applied

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**
- May merge (code gate): **yes** (subject to CI + CODEOWNERS + PR thread resolve protocol)

## Issues

### Issue 1 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:217-245` and `:611-629` (prior); now `:176-181`, `:230-255`, `:621-636`
- Description: **Session segment sanitization is applied inconsistently between the on-disk cache path and the relative URL path.**  
  *(Original)* `getCachedRegionCSSFileOnly` and `clearCacheRegionCSS` normalized the session id while `getCachedRegionCSSRelativePath` still used the raw session id.
- Suggestion:
  1. Extract a single `safeSessionSegment()` helper used by **all three** of: `getCachedRegionCSSFileOnly`, `getCachedRegionCSSRelativePath`, and `clearCacheRegionCSS`.
  2. Add a behavioral test that stubs/overrides session id to a value with a forbidden char (or null) and asserts relative path segment equals the directory segment used for the File under a `@TempDir` temp root.
- Status: **fixed**
- Pattern-id: `paths.dual-path-inconsistency` / correctness regression
- Verification (`6d8cce1e67`):
  - `public static String safeSessionSegment(String)` is the single source of truth.
  - Call sites all use it: `getCachedRegionCSSRelativePath`, `getCachedRegionCSSFileOnly`, `clearCacheRegionCSS`.
  - `safeSessionSegment_nullBlankAndSpecialChars` covers null/blank/safe/special-char cases.
  - Residual (non-blocking): no end-to-end test that stitches relative URL segment to `@TempDir` File parent via a stubbed session id (private methods); consistency is guaranteed by shared helper + unit tests.

### Issue 2 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/cloudservice/impl/PSCloudService.java:235-247`
- Description: **New non-trivial path-injection defense has zero behavioral tests.**  
  `generateThumbUrl` now applies `requireSafeFileName` on both segments then `requireUnderBase(PSServer.getRxDir(), …)` before `exists()`.
- Suggestion: Add a unit test class that:
  - rejects traversal / separators / NUL in `siteName` and `pageId` via `assertThrows(IllegalArgumentException.class, ...)`
  - with a temporary fake `rxDir` (or package-visible hook / subclass) verifies `requireUnderBase` composition returns empty URL when file missing and non-empty when present
- Status: **fixed**
- Pattern-id: `tests.missing-behavioral`
- Verification:
  - New `PSCloudServicePathInjectionTest` invokes real `generateThumbUrl` via `Unsafe.allocateInstance` (same pattern as other T043 service tests).
  - Covers siteName/pageId traversal, path separators (`/` and `\`), and NUL rejection — the security trust boundary.
  - Residual (non-blocking): no happy-path empty/non-empty URL under a fake `rxDir` (would need injectable `PSServer.getRxDir()`); `requireUnderBase` is covered elsewhere.

### Issue 3 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:223-246`, `:611-629`; test file `PSThemeServiceSecurityTest.java`
- Description: **Structural rewrite of theme temp cache / clearCache is untested; only `create()` rejection was extended.**
- Suggestion: Extend `PSThemeServiceSecurityTest` with `@TempDir` + injectable temp root to assert:
  - traversal theme name rejected before any File I/O on cache path
  - resolved cache File is under temp root
  - clearCache does not throw when temp root missing; when present, deletes only the session segment under temp root
- Status: **fixed**
- Pattern-id: `tests.missing-behavioral`
- Verification:
  - `create_rejectsTraversalInNewThemeName` / `create_rejectsSlashInNewThemeName` (prior).
  - `clearCacheRegionCSS_rejectsTraversalThemeName` / `_rejectsSlashInThemeName`.
  - `clearCacheRegionCSS_missingTempRootIsNoOp` (`@TempDir`, missing root → no create/no throw).
  - `clearCacheRegionCSS_deletesOnlySessionDir` (`@TempDir`: deletes `pssession` only; sibling preserved).
  - Residual (non-blocking): `getCachedRegionCSSFileOnly` multi-segment resolve under temp root is not invoked directly (private; needs cssFileService for public URL path). Composition is `safeSessionSegment` + validated theme + `requireUnderBase`, each exercised elsewhere.

### Issue 4 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSRenderLinkService.java:684-688`
- Description: **Structural change to `requireUnderBase(themesRoot, regionCssPath)` has no behavioral security tests.**
- Suggestion: Unit-test `renderLinkThemeRegionCSS` (or extract the file-resolution check) with a mocked `IPSThemeService` returning controlled roots and region CSS relative paths; assert traversal does not yield a File outside the root and does not throw uncaught.
- Status: **fixed**
- Pattern-id: `tests.missing-behavioral`
- Verification:
  - New `PSRenderLinkServicePathInjectionTest` exercises the call-site contract: legitimate multi-segment region CSS under themes root (`Files.isSameFile`), parent-traversal reject, absolute-outside reject, empty-file still contained.
  - Uses `@TempDir` / `Path` / `Files` — portable.
  - Residual (non-blocking): tests call `PSPathInjectionGuard.requireUnderBase` with the service’s argument shapes rather than `renderLinkThemeRegionCSS` itself; a silent revert of the service line would not fail these tests. Acceptable under prior “or extract the file-resolution check” escape hatch for Spring-heavy service; optional follow-up is a thin package-visible resolver or mock-driven service test.

### Issue 5 -- Severity: suggestion

- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/AssetAdaptor.java:894-905`
- Description: **Admin OS-folder check is only `getCanonicalFile()` + exists/isDirectory — not trusted-root containment.**  
  For bulk import preview, `osFolder` is documented as an admin-provided OS path, so full-filesystem access may be intentional. Canonicalization alone is **not** path-traversal protection against untrusted input; if this API is ever reachable without admin auth, the control is weak. Path query-filter + sink-line suppress the CodeQL residual without adding a deny-list or configured import root.
- Suggestion: Confirm `checkAPIPermission()` is strictly admin-only (document in code comment). Prefer an optional configured import-root allowlist + `requireUnderBase` if product policy allows. Add at least one test that missing/non-directory paths throw `FolderNotFoundException` and that `..` canonicalization still only accepts real directories the process can see (documents intended admin semantics).
- Status: open
- Pattern-id: `security.filename-only-not-path-traversal` (related: weak sanitizer presented as residual close)

### Issue 6 -- Severity: suggestion

- File: `.github/codeql/codeql-config.yml:232-264`
- Description: **Whole-file `java/path-injection` excludes expanded to 16 product files**, including several this PR only touches with sink-line comments (`PSLocalCommandHandler`, `PSRegionCSSFileService`, `PSAssetService`, `PSFileSystemPathItemService`) or does not touch at all in production code (`PSProcessDaemon`, `PSDtdTree`, `PSServer`). Playbook ladder allows path filters after structural + sink-line, but broad excludes amplify residual risk if any sink in those files lacks a real barrier. Suppressions.md row documents the set and cites `PSThemeServiceSecurityTest` + prior T043 tests — which do not cover every excluded file’s new residual claim.
- Suggestion: In suppressions.md / playbook notes, map each excluded file → named runtime barrier method + existing test class. For files with only sink-line and no test (e.g. `PSLocalCommandHandler` if prior validatePath tests are thin), add or cite a behavioral test before relying on path exclude as the close mechanism.
- Status: open
- Pattern-id: `security.broad-path-excludes-need-runtime-tests`

### Issue 7 -- Severity: suggestion

- File: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSRenderLinkService.java:684-687` (pre-existing interaction)
- Description: When `useCachedRegionCSS` is true, `regionCssPath` comes from `getCachedRegionCSSRelativeURL` (session/theme/… under **temp** themes), but the existence check still uses `themeService.getThemesRootDirectory()` (permanent themes root). Pre-fix used the same base; this PR preserves that. Cached edit mode may always see a non-existent File under the wrong root and return an empty link (`exists()` false → early return). Not introduced here, but the new `requireUnderBase` hardens the wrong base rather than fixing the base selection.
- Suggestion: When `useCached`, resolve under `getThemesTempRootDirectory()` (or a dedicated temp root File); when not cached, under themes root. Cover both with tests.
- Status: open

### Issue 8 -- Severity: nit

- File: `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:223-245` vs `:611-629`
- Description: Session sanitization regex is duplicated; easy to drift again (as Issue 1 already shows for the relative-path caller).
- Suggestion: Private `sanitizeSessionSegment(String)` used by all call sites.
- Status: **fixed** (superseded by Issue 1 fix: public static `safeSessionSegment` shared by all call sites)

### Issue 9 -- Severity: nit

- File: `projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:193`
- Description: Indent fix on `getPathItemFromFile` is good; remaining change is sink-line only. No new logic concern.
- Status: open (informational)

## Structural review notes (non-issue / pass)

|                   Area                    |                                                                                                     Assessment                                                                                                     |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PSThemeService.create`                   | `requireSafeFileName` both names + `safeThemeFolder` / `requireUnderBase` under themes root — correct trusted-root pattern.                                                                                        |
| `PSThemeService` session cache            | Single `safeSessionSegment` for relative path, file path, and clearCache — dual-path inconsistency closed.                                                                                                         |
| `PSFileSystemService.getFile/getChildren` | `validatePath` then `requireUnderBase(root, rel)` — defense in depth; leading `/` strip matches logical web-path root convention. Existing `PSFileSystemServiceSecurityTest` still exercises getFile/getChildren.  |
| `PSFileSystemService.renameFolder`        | Still relies on `containsInvalidChars` / reserved names for `newFolderName` (documented exception-type reasons). Parent from validated path. Acceptable; not filename-only as sole defense for multi-segment path. |
| `PSSiteDataService.updateThumbnailCache`  | `requireSafeFileName` then `requireUnderBase(cacheRoot, siteName)` after ensuring cache root exists — correct. Prior `PSSiteDataServicePathInjectionTest` covers name validation.                                  |
| `PSLocalCommandHandler`                   | Sink-line only after existing `validatePath` — residual thrash posture; no new structural risk in this diff.                                                                                                       |
| `PSRegionCSSFileService`                  | Sink-line only; prior `requireSafeFilePath` uses allowedRoots containment (trusted roots).                                                                                                                         |
| `PSAssetService`                          | `FileOutputStream` on `PSPurgableTempFile` — residual after temp-file construction; suppression-only is reasonable.                                                                                                |
| Path separators in new structural code    | Uses `requireUnderBase` / `File` / `File.separator` in `PAGE_IMAGE_CACHE_DIR`; relative multi-segment strings use `/` which Java `File` accepts cross-platform. No new Unix-only roots.                            |

## Cross-platform checklist

Applied to all structural I/O changes and the fix-pack tests:

|                          Check                          |                                                                 Result                                                                  |
|---------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Hardcoded `/` or `\\` filesystem joins in **new** logic | Mostly avoided via `requireUnderBase` / `File(parent, child)`. URL-style `THEME_REGION_CSS_PATH` and thumb URL paths correctly use `/`. |
| Unix-only absolute roots                                | None introduced.                                                                                                                        |
| Windows-only paths                                      | None.                                                                                                                                   |
| `File.pathSeparator` misuse                             | N/A                                                                                                                                     |
| Path string equality assuming Unix `toString()`         | New tests use `Path` / `Files.isSameFile` / `@TempDir` — not raw OS path strings.                                                       |
| Case-sensitive filesystem assumptions                   | None new.                                                                                                                               |
| Line-ending assertions                                  | N/A (no multi-line file content asserts that depend on `\n` only).                                                                      |
| Unix-only scripts                                       | N/A                                                                                                                                     |

**Cross-platform path review: no blocking portability bugs.** `requireUnderBase` normalizes `\\` → `/` for prefix checks (good for Windows).

## Memory patterns hit

- Missing **behavioral** unit tests for new/changed non-trivial logic (**hard gate**) — **resolved** for the four prior blockers
- Path containment / safe path must use a **trusted root**, not parent derived from untrusted input — largely followed in structural sites
- Filename-only sanitizers are not path-traversal protection when callers pass full paths — relevant to `AssetAdaptor` residual (suggestion)
- Broad whole-file CodeQL path excludes without documenting residual risk **and** runtime tests per file — still open as suggestion
- False-positive guard: URL/classpath `/` paths are fine

## What looks good

1. Preference for `PSPathInjectionGuard.requireUnderBase(trustedRoot, relative)` over raw `new File(root, user)` after only segment checks — matches institutional T043 pattern.
2. `PSThemeService.create` closes the previous raw `new File(getThemesRoot(), newTheme)` hole.
3. `PSFileSystemService` dual barrier (`validatePath` + `requireUnderBase`) is sound defense-in-depth.
4. `PSSiteDataService` rename under explicit `cacheRoot` is cleaner than string-concat absolute paths.
5. Suppressions.md gains a durable row for the residual ID set / expiry — good ops hygiene.
6. Sink-line comments placed on actual File API sinks (not multi-line builders above the sink) — matches CodeQL playbook guidance.
7. Fix pack `6d8cce1e67` closes consistency + test gaps without widening product scope.

## Required before re-review

1. ~~Fix session-segment single source of truth (Issue 1).~~ **done**
2. ~~Add behavioral tests for `PSCloudService.generateThumbUrl` (Issue 2).~~ **done**
3. ~~Add behavioral tests for theme cache / clearCache structural rewrite (Issue 3).~~ **done** (clearCache + session helper; cache File composition residual noted)
4. ~~Add behavioral tests for `PSRenderLinkService` requireUnderBase resolution (Issue 4).~~ **done** (call-site contract tests)
5. Re-run affected tests via `./mvnw` / `./mvnw.cmd` (sitemanage module security tests at minimum) — **author/CI responsibility**; not re-run in this read-only review.
6. ~~Request Erlang re-review; update this file with `## Re-review` and issue statuses.~~ **done** (this section)

## Re-review

**Date**: 2026-07-18  
**Reviewer**: Erlang (strict independent re-review, read-only)  
**Head reviewed**: `6d8cce1e67de6aeb859da7c4d7cefc004a4b8f7c`  
**Commit**: `fix(security): close Erlang gates on path-injection residual batch`  
**Prior gate**: `request-changes` @ `b00558f0` (4 blocking bugs)

### Blocker verification matrix

| # |                   Prior block                   |                            Fix evidence                            |                       Behavioral tests                       |  Status   |
|---|-------------------------------------------------|--------------------------------------------------------------------|--------------------------------------------------------------|-----------|
| 1 | Session file/URL inconsistency                  | `safeSessionSegment` used by relative path, cache File, clearCache | `safeSessionSegment_nullBlankAndSpecialChars`                | **fixed** |
| 2 | `PSCloudService.generateThumbUrl` no tests      | Unchanged structural defense                                       | `PSCloudServicePathInjectionTest` (traversal/separators/NUL) | **fixed** |
| 3 | Theme cache/clearCache under-tested             | clearCache early-return + session containment                      | `clearCacheRegionCSS_*` (+ prior `create_*`)                 | **fixed** |
| 4 | `PSRenderLinkService` requireUnderBase no tests | Service still uses `requireUnderBase(themesRoot, regionCssPath)`   | `PSRenderLinkServicePathInjectionTest` (accept/reject/empty) | **fixed** |

### New residual findings from fix pack

None at **bug** severity. Non-blocking residuals already noted under Issues 1–4 verification and open Issues 5–7, 9.

### Final Recommendation / Gate

|         Field         |                               Value                               |
|-----------------------|-------------------------------------------------------------------|
| Recommendation        | **approve**                                                       |
| Blocking bugs         | **0**                                                             |
| May commit/push       | **yes**                                                           |
| May merge (code gate) | **yes** (CI + CODEOWNERS + PR review-thread protocol still apply) |

Optional follow-ups only: Issues 5–7 (AssetAdaptor admin path, broad path excludes map, cached-vs-permanent themes root for render link), Issue 9 informational, and the documented residual test strengthenings (e2e session segment on File+URL, cloud happy-path under fake rxDir, service-level render-link test).

## Gate language

**Prior gate cleared.** Recommendation: **approve**. May commit/push: **yes**.  
Treat open Issues 5–7 as non-blocking follow-ups; do not re-open the hard gate for residuals alone unless product policy elevates AssetAdaptor / broad excludes.
