# Erlang Review — PR #1362 T043 path-injection residuals batch

**Reviewer**: Erlang (strict, independent, read-only)  
**Date**: 2026-07-18  
**PR**: #1362  
**Branch**: `004/us3-t043-path-injection-residuals`  
**Head**: `b00558f0f34a5a24d9569df3d43dc7ca480fe712`  
**Base**: `origin/development`  
**Worktree**: `/home/nate/projects/percussioncms.worktrees/004-java-path-injection-batch`  
**Intent**: multi-pass `java/path-injection` residual clearance (~14 product files) + consolidated path query-filters + sink-line suppressions  

## Summary

PR #1362 closes a large residual `java/path-injection` thrash set by combining real structural hardening (`PSPathInjectionGuard.requireUnderBase` / `requireSafeFileName` / `safeThemeFolder`) with whole-file path query-filters and many sink-line `// codeql[java/path-injection]` markers. The strongest structural work is in `PSThemeService` (create/cache/clearCache), `PSFileSystemService` (getFile/getChildren), `PSSiteDataService` (thumbnail cache rename), `PSRenderLinkService`, and `PSCloudService`. Those structural moves are mostly directionally correct (trusted-root containment, not parent-of-input), but **new non-trivial logic is under-tested**: only two new assertions land in `PSThemeServiceSecurityTest`, while `PSCloudService.generateThumbUrl`, the theme temp-cache session sanitization rewrite, and `PSRenderLinkService`’s `requireUnderBase` path have no dedicated behavioral coverage. Separately, `PSThemeService` now builds cache **files** under a sanitized session segment while still returning the **relative URL** from the raw session id — a consistency regression that can break cache hit/clear paths. Gate: **request-changes**.

## Scope

- Base: `origin/development`
- Head: `004/us3-t043-path-injection-residuals` @ `b00558f0` (commits `96d9379351` structural + `b00558f0` suppressions docs)
- Files reviewed (from PR diff): 14
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

**request-changes**

## Gate

- Blocking bugs: **4**
- May commit/push: **no**
- Author must not treat this PR as merge-ready until blocking items are fixed and Erlang re-reviews.

## Issues

### Issue 1 -- Severity: bug
- File: `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:217-245` and `:611-629`
- Description: **Session segment sanitization is applied inconsistently between the on-disk cache path and the relative URL path.**  
  `getCachedRegionCSSFileOnly` and `clearCacheRegionCSS` normalize the session id to  
  `null/blank → "pssession"` else `replaceAll("[^a-zA-Z0-9._-]", "_")`, then resolve under the temp root via `requireUnderBase`.  
  `getCachedRegionCSSRelativePath` still returns `getCurrentSessionId() + "/" + theme + "/" + THEME_REGION_CSS_PATH` with the **raw** session id.  
  `getCachedRegionCSSRelativeURL` writes/reads the file via the sanitized path, then returns the unsanitized relative path. Any session id that is null/blank (when a request exists but session id is null) or contains a character outside `[a-zA-Z0-9._-]` produces:
  - file written under `pssession/...` or sanitized name
  - URL / clear targeting a different segment  
  Pre-fix, file construction and relative path shared one formula (`getCachedRegionCSSRelativePath`), so they stayed consistent. This batch **introduces** the split.
- Suggestion:
  1. Extract a single `safeSessionSegment()` helper used by **all three** of: `getCachedRegionCSSFileOnly`, `getCachedRegionCSSRelativePath`, and `clearCacheRegionCSS`.
  2. Add a behavioral test that stubs/overrides session id to a value with a forbidden char (or null) and asserts relative path segment equals the directory segment used for the File under a `@TempDir` temp root.
- Status: open
- Pattern-id: `paths.dual-path-inconsistency` / correctness regression

### Issue 2 -- Severity: bug
- File: `projects/sitemanage/src/main/java/com/percussion/cloudservice/impl/PSCloudService.java:235-247`
- Description: **New non-trivial path-injection defense has zero behavioral tests.**  
  `generateThumbUrl` now:
  1. `requireSafeFileName(siteName)` / `requireSafeFileName(pageId)`
  2. builds multi-segment `rx_resources/images/TemplateImages/{site}/{page}-page.jpg`
  3. `requireUnderBase(PSServer.getRxDir(), ...)` before `exists()`  
  This is exactly the hard-gate category “missing behavioral tests for new/changed non-trivial logic.” There is no `PSCloudService*Test` (or equivalent) under `projects/sitemanage/src/test` covering rejection of `../`, separators, NUL, or acceptance of a safe existing thumb path. PR narrative cites only `PSThemeServiceSecurityTest` (+ prior suite counts).
- Suggestion: Add a unit test class that:
  - rejects traversal / separators / NUL in `siteName` and `pageId` via `assertThrows(IllegalArgumentException.class, ...)`
  - with a temporary fake `rxDir` (or package-visible hook / subclass) verifies `requireUnderBase` composition returns empty URL when file missing and non-empty when present
- Status: open
- Pattern-id: `tests.missing-behavioral`

### Issue 3 -- Severity: bug
- File: `projects/sitemanage/src/main/java/com/percussion/theme/service/impl/PSThemeService.java:223-246`, `:611-629`; test file `PSThemeServiceSecurityTest.java:140-158`
- Description: **Structural rewrite of theme temp cache / clearCache is untested; only `create()` rejection was extended.**  
  New logic includes: best-effort `tempRoot.mkdirs()`, session sanitization, multi-segment `requireUnderBase(tempRoot, session/theme/perc/perc_region.css)`, and clearCache early-return when temp root missing. The only new tests are:
  - `create_rejectsTraversalInNewThemeName`
  - `create_rejectsSlashInNewThemeName`  
  Those correctly cover the `create()` validator-first change but do **not** exercise cache path construction, clearCache containment, or the mkdirs/`requireUnderBase` interaction. Under Erlang / AGENTS.md, this is blocking for non-trivial new behavior.
- Suggestion: Extend `PSThemeServiceSecurityTest` (or a focused cache test) with `@TempDir` + injectable temp/themes roots (or package-visible test hooks already used elsewhere) to assert:
  - traversal theme name rejected before any File I/O on cache path
  - resolved cache File is under temp root (canonical prefix / `Files.isSameFile` parent chain)
  - clearCache does not throw when temp root missing; when present, deletes only the session segment under temp root
- Status: open
- Pattern-id: `tests.missing-behavioral`

### Issue 4 -- Severity: bug
- File: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSRenderLinkService.java:684-688`
- Description: **Structural change to `requireUnderBase(themesRoot, regionCssPath)` has no behavioral security tests.**  
  Pre-fix used string concat + `new File(...)`. Post-fix rejects paths that canonicalize outside the themes root (correct trusted-root pattern). Exception path is swallowed (`catch (Exception)` → warn) then URL construction may still proceed depending on branch — behavior that needs explicit tests for:
  - traversal `regionCssPath` → no escape / empty or safe fail
  - legitimate in-root CSS path → exists check behaves  
  No new/updated unit test accompanies this change. Existing `PSRenderLinkServiceTest` is a thin web client shell, not a path-injection harness.
- Suggestion: Unit-test `renderLinkThemeRegionCSS` (or extract the file-resolution check) with a mocked `IPSThemeService` returning controlled roots and region CSS relative paths; assert traversal does not yield a File outside the root and does not throw uncaught.
- Status: open
- Pattern-id: `tests.missing-behavioral`

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
- Status: open

### Issue 9 -- Severity: nit
- File: `projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSFileSystemPathItemService.java:193`
- Description: Indent fix on `getPathItemFromFile` is good; remaining change is sink-line only. No new logic concern.
- Status: open (informational)

## Structural review notes (non-issue / pass)

| Area | Assessment |
|------|------------|
| `PSThemeService.create` | `requireSafeFileName` both names + `safeThemeFolder` / `requireUnderBase` under themes root — correct trusted-root pattern. |
| `PSFileSystemService.getFile/getChildren` | `validatePath` then `requireUnderBase(root, rel)` — defense in depth; leading `/` strip matches logical web-path root convention. Existing `PSFileSystemServiceSecurityTest` still exercises getFile/getChildren. |
| `PSFileSystemService.renameFolder` | Still relies on `containsInvalidChars` / reserved names for `newFolderName` (documented exception-type reasons). Parent from validated path. Acceptable; not filename-only as sole defense for multi-segment path. |
| `PSSiteDataService.updateThumbnailCache` | `requireSafeFileName` then `requireUnderBase(cacheRoot, siteName)` after ensuring cache root exists — correct. Prior `PSSiteDataServicePathInjectionTest` covers name validation. |
| `PSLocalCommandHandler` | Sink-line only after existing `validatePath` — residual thrash posture; no new structural risk in this diff. |
| `PSRegionCSSFileService` | Sink-line only; prior `requireSafeFilePath` uses allowedRoots containment (trusted roots). |
| `PSAssetService` | `FileOutputStream` on `PSPurgableTempFile` — residual after temp-file construction; suppression-only is reasonable. |
| Path separators in new structural code | Uses `requireUnderBase` / `File` / `File.separator` in `PAGE_IMAGE_CACHE_DIR`; relative multi-segment strings use `/` which Java `File` accepts cross-platform. No new Unix-only roots. |

## Cross-platform checklist

Applied to all structural I/O changes in this diff:

| Check | Result |
|-------|--------|
| Hardcoded `/` or `\\` filesystem joins in **new** logic | Mostly avoided via `requireUnderBase` / `File(parent, child)`. URL-style `THEME_REGION_CSS_PATH` and thumb URL paths correctly use `/`. |
| Unix-only absolute roots | None introduced. |
| Windows-only paths | None. |
| `File.pathSeparator` misuse | N/A |
| Path string equality assuming Unix `toString()` | Not introduced in new tests (only IAE on create). |
| Case-sensitive filesystem assumptions | None new. |
| Line-ending assertions | N/A (no multi-line file content asserts added). |
| Unix-only scripts | N/A |

**Cross-platform path review: no new blocking portability bugs.** Issue 1 (session sanitization) is functional consistency, not OS-separator portability. `requireUnderBase` itself normalizes `\\` → `/` for prefix checks (good for Windows).

## Memory patterns hit

- Missing **behavioral** unit tests for new/changed non-trivial logic (**hard gate**)
- Path containment / safe path must use a **trusted root**, not parent derived from untrusted input — largely followed in structural sites
- Filename-only sanitizers are not path-traversal protection when callers pass full paths — relevant to `AssetAdaptor` residual
- Broad whole-file CodeQL path excludes without documenting residual risk **and** runtime tests per file
- False-positive guard: URL/classpath `/` paths are fine

## What looks good

1. Preference for `PSPathInjectionGuard.requireUnderBase(trustedRoot, relative)` over raw `new File(root, user)` after only segment checks — matches institutional T043 pattern.
2. `PSThemeService.create` closes the previous raw `new File(getThemesRoot(), newTheme)` hole.
3. `PSFileSystemService` dual barrier (`validatePath` + `requireUnderBase`) is sound defense-in-depth.
4. `PSSiteDataService` rename under explicit `cacheRoot` is cleaner than string-concat absolute paths.
5. Suppressions.md gains a durable row for the residual ID set / expiry — good ops hygiene.
6. Sink-line comments placed on actual File API sinks (not multi-line builders above the sink) — matches CodeQL playbook guidance.

## Required before re-review

1. Fix session-segment single source of truth (Issue 1).
2. Add behavioral tests for `PSCloudService.generateThumbUrl` (Issue 2).
3. Add behavioral tests for theme cache / clearCache structural rewrite (Issue 3).
4. Add behavioral tests for `PSRenderLinkService` requireUnderBase resolution (Issue 4).
5. Re-run affected tests via `./mvn-env.sh` / `./mvn-env.bat` (sitemanage module security tests at minimum).
6. Request Erlang re-review; update this file with `## Re-review` and issue statuses.

## Gate language

**Do not commit additional “done” claims, force-merge, or treat PR #1362 as clearance-complete until blocking bugs are fixed and re-reviewed.**  
Recommendation: **request-changes**. May commit/push: **no**.
