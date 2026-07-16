# Erlang pattern harvest candidates

**Generated:** 2026-07-16 16:27 UTC  
**Repo:** `intersoftdatalabs-in/percussioncms`  
**Authors:** `kilo-code-bot[bot]`, `kilo-code-bot`  
**Review comments scanned:** 770  
**Top-level comments kept:** 197  
**Clusters:** 178  
**Promotion threshold (multi-PR):** count ≥ 2 **and** distinct PRs ≥ 2 (use `--promote-critical` for single-PR CRITICAL gates)  
**Patterns file:** `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`  

## Auto-apply selection

Selected **1** theme(s) for merge into patterns:

- **[Cross-platform / I/O]** `parseLongIdOrNull` Javadoc contradicts implementation  
  seen 2× · PRs [1202, 1207] · severity=suggestion

## All clusters (by frequency)

| Count | PRs | Sev | Category | Principle |
|------:|----:|-----|----------|-----------|
| 3 | 1 | suggestion | Maintainability | Copyright year regression from 2025 to 2023 |
| 3 | 1 | suggestion | Maintainability | Misleading dead default for `autoCollapse` |
| 3 | 1 | warning | Maintainability | Swallowed exception can mask a broken-tree state |
| 2 | 2 | suggestion | Cross-platform / I/O | `parseLongIdOrNull` Javadoc contradicts implementation |
| 2 | 1 | suggestion | Security / config | <test-symbol> is too broad to prove SSRF rejection |
| 2 | 1 | warning | Cross-platform / I/O | Error handler signature changed from <symbol> to <symbol> |
| 2 | 1 | critical | Installer / Ant / distribution | exec-maven-plugin `<systemProperty>` uses `<key>` instead of `<name>` |
| 2 | 1 | warning | Cross-platform / I/O | Hardcoded expected dependabot count (229) is brittle |
| 2 | 1 | warning | Cross-platform / I/O | Unguarded `result.PathItem.path` dereference introduces a new crash path |
| 2 | 1 | warning | Maintainability | Unreachable <symbol> violates "never null" contract |
| 1 | 1 | suggestion | Maintainability | "Complexity Tracking table" does not match the template format |
| 1 | 1 | suggestion | Cross-platform / I/O | <symbol> assumes the test is executed exactly two directory levels below the monorepo root ( `projects/sitemanage` ),… |
| 1 | 1 | warning | Cross-platform / I/O | <symbol> can never catch the `SecurityException` thrown by `validatePath` |
| 1 | 1 | suggestion | Tests | <symbol> derives the monorepo location from the current working directory ( <symbol> + `../..` ). |
| 1 | 1 | suggestion | Maintainability | <symbol> here is unreachable dead code. `checkAndThrowValidationException` always throws (either `PSParametersValidat… |
| 1 | 1 | suggestion | Maintainability | <symbol> is called twice (again at line N) |
| 1 | 1 | suggestion | Tests | <symbol> is implicitly working-directory dependent. |
| 1 | 1 | warning | Maintainability | <symbol> may be an invalid flag |
| 1 | 1 | suggestion | Cross-platform / I/O | <symbol> misclassifies macOS. |
| 1 | 1 | critical | Cross-platform / I/O | <symbol> rejects ANY string containing a forward or back slash (path separators), but every caller in `PSThemeService… |
| 1 | 1 | suggestion | Installer / Ant / distribution | <symbol> returns a boolean indicating whether the operation succeeded, and here the result is discarded. On filesyste… |
| 1 | 1 | warning | Maintainability | <symbol> treats `\|` as a regex. In gawk `\|` matches the empty string, splitting the filename into individual charac… |
| 1 | 1 | warning | Maintainability | <symbol> truncates the diff to 20 files |
| 1 | 1 | critical | Cross-platform / I/O | <symbol> validates containment against the input-derived parent, not the trusted region CSS root — path traversal is … |
| 1 | 1 | suggestion | Installer / Ant / distribution | `--expected-driver-set` example uses renamed names that won't match the shipped artifact |
| 1 | 1 | warning | Installer / Ant / distribution | `_jdbc-stage` staging directory is never cleaned and leaks into the shipped distribution |
| 1 | 1 | warning | Maintainability | `aria-label` is ineffective because the element is also marked `aria-hidden="true"` . |
| 1 | 1 | warning | Cross-platform / I/O | `basehome:` prefix removed from `[lib]` path |
| 1 | 1 | warning | Cross-platform / I/O | `containsForbiddenCharacters` returns `true` for safe filenames containing `..` . |
| 1 | 1 | suggestion | Tests | `escapeHtmlForResponse` is only ever called from the test (no service method invokes it), yet it ships in production … |
| 1 | 1 | warning | Maintainability | `parseLongId` Javadoc (line N) claims the caller catches `NumberFormatException` and that "an empty result is returne… |
| 1 | 1 | suggestion | Maintainability | `parseLongId` returns `0L` when the value is `null` (line N). If a search-field key is present but its value is `null… |
| 1 | 1 | warning | Installer / Ant / distribution | `PSValidateRepositoryConnection` is gated only by <path> so upgrades are correctly excluded. But validation now runs … |
| 1 | 1 | suggestion | Maintainability | `Ratified` date dropped from the version line |
| 1 | 1 | suggestion | Maintainability | `requireSafeFileName` can throw `IllegalArgumentException` that escapes the surrounding try/catch |
| 1 | 1 | warning | Cross-platform / I/O | `requireSafeFileName` rejects forward slashes, breaking multi-segment relative paths |
| 1 | 1 | warning | Cross-platform / I/O | `requireUnderBase` fast-path <symbol> is overly broad and causes false positives on legitimate inputs. |
| 1 | 1 | warning | Maintainability | `requireUnderBase` throws `IllegalArgumentException` when the base directory does not exist |
| 1 | 1 | warning | Cross-platform / I/O | `rootpath` is concatenated raw into the query string, while `sitename` is now `encodeURIComponent` -encoded. |
| 1 | 1 | suggestion | Maintainability | `state_arg` is taken verbatim from `$2` with no validation. An invalid value is passed straight into the <symbol> URL… |
| 1 | 1 | critical | Cross-platform / I/O | `testConstructSafePathRejectsAbsolutePath` uses a non-existent base directory |
| 1 | 1 | suggestion | Tests | `window.pwned` is never reset in `afterEach` , but the injection assertions depend on it being `undefined` . |
| 1 | 1 | warning | Installer / Ant / distribution | A null or closed connection is reported with a generic "Check host, credentials, ..." message, but <symbol> may retur… |
| 1 | 1 | suggestion | Maintainability | Add `XMLConstants.FEATURE_SECURE_PROCESSING` for defense-in-depth |
| 1 | 1 | suggestion | Tests | Add unit-test coverage for the new helper-level `requireSafeFilePath` validations |
| 1 | 1 | warning | Maintainability | Appending null buildNum produces literal "null" in version string |
| 1 | 1 | suggestion | Tests | Asserted exception type couples the test to the dead- `catch` behavior |
| 1 | 1 | suggestion | Tests | Assertion is redundant and does not verify the guard |
| 1 | 1 | warning | Cross-platform / I/O | Because line N already rejects every path containing a separator, the canonical-containment check here is effectively… |
| 1 | 1 | suggestion | Tests | Brittle monorepo-root resolution tied to working directory |
| 1 | 1 | warning | Tests | Brittle string-matching test |
| 1 | 1 | warning | Maintainability | Broad <symbol> masks `NullPointerException` from `requireNonNull` |
| 1 | 1 | suggestion | Maintainability | Broad <symbol> silently swallows every error. |
| 1 | 1 | suggestion | Maintainability | Broadening the substring markers widens the false-positive surface |
| 1 | 1 | warning | Cross-platform / I/O | Buffering the full serialized document in a StringWriter doubles peak memory for this serialization path |
| 1 | 1 | suggestion | Maintainability | Catching `IllegalArgumentException` broadly and re-throwing only when <symbol> silently swallows any other `IllegalAr… |
| 1 | 1 | suggestion | Installer / Ant / distribution | Comment overstates the ANT copy failure behavior |
| 1 | 1 | suggestion | Maintainability | Comment says "merge date desc" but sort is ascending |
| 1 | 1 | critical | Installer / Ant / distribution | Commit message pattern mismatch - condition won't prevent redundant runs from automated build-number workflow |
| 1 | 1 | suggestion | Maintainability | Concurrency branch still marks the item FAILED |
| 1 | 1 | warning | Maintainability | Concurrency detection by class-name substring misses common Spring/Hibernate lock exceptions |
| 1 | 1 | suggestion | Maintainability | Confirm the 500 -> 400 status change is intended |
| 1 | 1 | suggestion | Maintainability | Constitution Check no longer prompts for Complexity Budget / Complexity Tracking |
| 1 | 1 | warning | Cross-platform / I/O | Constructor now rejects absolute `config` paths (regression risk + untested) |
| 1 | 1 | suggestion | Cross-platform / I/O | CRLF normalization only handles \ `\r\n\` pairs |
| 1 | 1 | warning | Security / config | Dangerous-element blocklist is incomplete |
| 1 | 1 | suggestion | Maintainability | Dead/unused priority helpers |
| 1 | 1 | suggestion | Cross-platform / I/O | Delivery module path is imprecise |
| 1 | 1 | suggestion | Installer / Ant / distribution | Demoting "type not found" from WARN to DEBUG reduces production diagnosability |
| 1 | 1 | suggestion | Tests | Description assertion's broad <symbol> fallback defeats the check |
| 1 | 1 | suggestion | Cross-platform / I/O | Direct-cause-only check may miss nested `IPSNotFoundException` causes |
| 1 | 1 | suggestion | Maintainability | Double-write risk if `writeErrorResponse` fails in the `else` branch |
| 1 | 1 | suggestion | Cross-platform / I/O | Drive-letter detection can over-reject legitimate relative filenames on Unix. |
| 1 | 1 | critical | Maintainability | Duplicate method declaration <symbol> prevents compilation |
| 1 | 1 | suggestion | Tests | Exact <test-symbol> is brittle and inconsistent with the theme test |
| 1 | 1 | warning | Cross-platform / I/O | Exception filtering relies on <symbol> to distinguish the parent-missing case from a traversal-escape detection. This… |
| 1 | 1 | suggestion | Maintainability | Extension check is case-sensitive |
| 1 | 1 | suggestion | Maintainability | Fix rationale may be incomplete for the stated root cause. |
| 1 | 1 | warning | Cross-platform / I/O | Fragile working-directory dependency in resolveRoot() |
| 1 | 1 | suggestion | Installer / Ant / distribution | Garbled Apache license header — <symbol> is a duplication. The standard boilerplate is <symbol> |

_…and 98 more clusters omitted._

## Sample evidence (top clusters)

### Copyright year regression from 2025 to 2023

- category: **Maintainability** · count: **3** · PRs: [1262] · severity: suggestion
- paths: `projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderQueryHandler.java`, `projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHelper.java`, `projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHandler.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1262#discussion_r3588423461
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1262#discussion_r3588423452
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1262#discussion_r3588423446

### Misleading dead default for `autoCollapse`

- category: **Maintainability** · count: **3** · PRs: [1256] · severity: suggestion
- paths: `WebUI/src/main/webapp/cm/app/js/legacy/views/PercCategoryView.js`, `WebUI/src/main/webapp/cm/views/PercCategoryView.js`, `WebUI/war/views/PercCategoryView.js`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295162
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295158
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295156

### Swallowed exception can mask a broken-tree state

- category: **Maintainability** · count: **3** · PRs: [1256] · severity: warning
- paths: `WebUI/src/main/webapp/cm/app/js/legacy/views/PercCategoryView.js`, `WebUI/src/main/webapp/cm/views/PercCategoryView.js`, `WebUI/war/views/PercCategoryView.js`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295150
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295147
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1256#discussion_r3589295144

### `parseLongIdOrNull` Javadoc contradicts implementation

- category: **Cross-platform / I/O** · count: **2** · PRs: [1202, 1207] · severity: suggestion
- paths: `modules/perc-security-utils/src/main/java/com/percussion/security/io/PSPathInjectionGuard.java`, `projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDaoHelper.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1207#discussion_r3576060202
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1202#discussion_r3573169134

### <test-symbol> is too broad to prove SSRF rejection

- category: **Security / config** · count: **2** · PRs: [1198] · severity: suggestion
- paths: `modules/extensions-main/src/test/java/com/percussion/extensions/general/PSProxyQueryResourceTest.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1198#discussion_r3571674169
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1198#discussion_r3571674165

### Error handler signature changed from <symbol> to <symbol>

- category: **Cross-platform / I/O** · count: **2** · PRs: [1152] · severity: warning
- paths: `WebUI/war/plugins/perc_path_manager.js`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1152#discussion_r3477680046
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1152#discussion_r3477680036

### exec-maven-plugin `<systemProperty>` uses `<key>` instead of `<name>`

- category: **Installer / Ant / distribution** · count: **2** · PRs: [1181] · severity: critical
- paths: `modules/perc-distribution-tree/pom.xml`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1181#discussion_r3560877626
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1181#discussion_r3560877618

### Hardcoded expected dependabot count (229) is brittle

- category: **Cross-platform / I/O** · count: **2** · PRs: [1206] · severity: warning
- paths: `scripts/release-audit/tests/test_inventory.sh`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1206#discussion_r3575771614
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1206#discussion_r3575771612

### Unguarded `result.PathItem.path` dereference introduces a new crash path

- category: **Cross-platform / I/O** · count: **2** · PRs: [1246] · severity: warning
- paths: `WebUI/war/plugins/perc_utils.js`, `WebUI/src/main/webapp/cm/plugins/perc_utils.js`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1246#discussion_r3585231412
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1246#discussion_r3585231404

### Unreachable <symbol> violates "never null" contract

- category: **Maintainability** · count: **2** · PRs: [1262] · severity: warning
- paths: `projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHelper.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1262#discussion_r3588423437
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1262#discussion_r3588423433

### "Complexity Tracking table" does not match the template format

- category: **Maintainability** · count: **1** · PRs: [1194] · severity: suggestion
- paths: `.specify/memory/constitution.md`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1194#discussion_r3571642945

### <symbol> assumes the test is executed exactly two directory levels below the monorepo root ( `projects/sitemanage` ), relying on <path> .

- category: **Cross-platform / I/O** · count: **1** · PRs: [1247] · severity: suggestion
- paths: `projects/sitemanage/src/test/java/com/percussion/pagemanagement/service/impl/SocialButtonsXRebrandTest.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1247#discussion_r3585258573

### <symbol> can never catch the `SecurityException` thrown by `validatePath`

- category: **Cross-platform / I/O** · count: **1** · PRs: [1222] · severity: warning
- paths: `system/src/main/java/com/percussion/process/PSLocalCommandHandler.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1222#discussion_r3582725842

### <symbol> derives the monorepo location from the current working directory ( <symbol> + `../..` ).

- category: **Tests** · count: **1** · PRs: [1236] · severity: suggestion
- paths: `projects/sitemanage/src/test/java/com/percussion/pagemanagement/service/impl/WidgetRegistryRegistrationDeprecationTest.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1236#discussion_r3584469713

### <symbol> here is unreachable dead code. `checkAndThrowValidationException` always throws (either `PSParametersValidationException` for a `ParseException` , or rethrows the original `e` ), so control never reaches line…

- category: **Maintainability** · count: **1** · PRs: [1250] · severity: suggestion
- paths: `projects/sitemanage/src/main/java/com/percussion/searchmanagement/service/impl/PSSearchRestService.java`
- https://github.com/intersoftdatalabs-in/percussioncms/pull/1250#discussion_r3585394162

## How to promote

```text
python3 scripts/erlang-harvest-review-patterns.py --apply
```

Or on Windows:

```text
scripts\erlang-harvest-review-patterns.bat --apply
```

Review the diff to `patterns.md` before committing. Harvested bullets are
marked `_(harvested, seen N×)_` so humans can later rewrite them to cleaner
principles and drop the marker.

