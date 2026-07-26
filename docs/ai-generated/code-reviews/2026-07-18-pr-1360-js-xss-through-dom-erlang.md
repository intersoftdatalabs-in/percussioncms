# Erlang review — PR #1360 `js/xss-through-dom` DOM-text-as-HTML

**Date:** 2026-07-18  
**Reviewer:** Erlang (strict independent pre-merge)  
**Worktree:** `/home/nate/projects/percussioncms.worktrees/004-js-xss-through-dom`  
**Branch:** `004/us3-js-xss-through-dom-dom-text`  
**Head:** `85bf50742802bef8cdb7f1a1c4dcc7785e782e25`  
**Base:** `origin/development` (`78b1465f9b8b4daeace4c5434cd9d91fff106a5f`)  
**PR:** https://github.com/intersoftdatalabs-in/percussioncms/pull/1360

## Summary

PR #1360 closes product CodeQL **high** `js/xss-through-dom` (DOM text reinterpreted as HTML) sinks across WebUI views/plugins, widget packages (form, imageSlider, socialButtons, directory, lightbox), TinyMCE `percglobalvariables`, and RSS/most-read common-ui-bundle views. Structural fixes are largely correct: jQuery factories + `.text()` / `.val().text()` instead of HTML string concat; heading allow-list; CSS-safe platform tokens; lightbox attribute equality filters; form metadata HTML-escape including Kilo-flagged `saveToUrl`; imageSlider lockstep copies with `safeThumbnailSrc` + residual CodeQL path excludes.

**Overall assessment:** product sink remediations are directionally sound and multi-copy imageSlider/socialButtons stay in lockstep, but the gate fails on (1) a **non-trivial security helper** (`safeThumbnailSrc`) with **no behavioral tests** while escalating to whole-file CodeQL path excludes, and (2) **missing `suppressions.md` rows** required by the CodeQL PR playbook (contracts/C3) for those path excludes / residual disposition. Most other new tests are source-grep-only (acceptable for pure mechanical `.html`→`.text` when paired with existing PercUserView behavioral coverage; inadequate as sole proof for new sanitizers).

## Scope

- Base: `origin/development`
- Head: `004/us3-js-xss-through-dom-dom-text` @ `85bf507428` (PR #1360 tip)
- Files: **20** changed (+435 / −110)
- Prior report: none for this PR/topic
- Memory patterns hit:
  - Hard gate: missing **behavioral** tests for non-trivial security helpers
  - Hard gate / tests: source-grep-only / structural-only treated as sole proof
  - Maintainability: multi-copy shared WebUI/package assets (checked)
  - Security/config: broad CodeQL path excludes without complete residual documentation + runtime tests
  - CodeQL playbook: disposition ladder + `suppressions.md` C3

### Changed files (review coverage)

|                               Path                               |                                     Intent                                      |
|------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `.github/codeql/codeql-config.yml`                               | Path `query-filters` exclude for both imageSlider copies (`js/xss-through-dom`) |
| `WebUI/.../legacy/plugins/PercRedirectHandler.js`                | Dialog: jQuery factories + `.text(getRelativePath(...))`                        |
| `WebUI/war/plugins/PercRedirectHandler.js`                       | Lockstep war copy of redirect dialog fix                                        |
| `WebUI/.../legacy/views/PercUserView.js`                         | Role transfer options: `.html` → `.text`                                        |
| `WebUI/war/views/PercUserView.js`                                | Lockstep war copy                                                               |
| `WebUI/src/test/js/percUserView.test.js`                         | Source-pattern lockstep residual for role-transfer sinks                        |
| `WebUI/src/test/js/percRedirectHandlerDomText.test.js`           | Source-pattern anti-concat for plugins + legacy                                 |
| `WebUI/src/test/js/percRssAndMostReadDomText.test.js`            | Source-pattern RSS + mirrored heading allow-list unit                           |
| `WebUI/src/test/js/percFormViewDomText.test.js`                  | Source-pattern form metadata escape incl. `saveToUrl`                           |
| `modules/perc-common-ui-bundle/.../PercRssView.js`               | Description plain-text + escape before HTML embed                               |
| `modules/perc-common-ui-bundle/.../PercMostReadBlogPostsView.js` | `safeHeadingTag` whitelist                                                      |
| `.../perc.widget.directory/.../perc-directory.js`                | Options via `.val().text()`                                                     |
| `.../perc.widget.form/.../PercFormController.js`                 | Form element factory (no name-in-HTML-string)                                   |
| `.../perc.widget.form/.../PercFormView.js`                       | Escape all read-only metadata fields incl. `saveToUrl`                          |
| `.../imageSlider/.../percImageSlider.js` ×2                      | Factory img + `safeThumbnailSrc` + sink-line suppress                           |
| `.../socialButtons/.../percSocialButtons.js` ×2                  | CSS-safe platform token for class selectors                                     |
| `.../lightbox/lightbox.js`                                       | Filter by attr equality, not selector concat                                    |
| `.../percglobalvariables/plugin.js`                              | Regex title extract + `escAttr`                                                 |

### Lockstep verification (task-specific)

|              Asset set               |                              Copies                               |                                              In sync?                                               |
|--------------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| imageSlider `percImageSlider.js`     | SupportFile + sys__UserDependency                                 | **Yes** — same `safeThumbnailSrc` + `.attr("src", safe…)` pattern at both thumbnail sites           |
| socialButtons `percSocialButtons.js` | SupportFile + sys__UserDependency                                 | **Yes** — same `safePlatform` replace + selector usage                                              |
| PercRedirectHandler                  | main `cm/plugins` (already safe), legacy (this PR), war (this PR) | **Yes** for security semantics (war style differs slightly; both use `.text(getRelativePath(...))`) |
| PercUserView                         | main `cm/views` (already `.text`), legacy + war (this PR)         | **Yes** for role-transfer sinks                                                                     |
| directory `perc-directory.js`        | Single web_resources path (no SupportFile twin for this file)     | N/A                                                                                                 |
| form PercFormView/Controller         | SupportFile only (no UserDependency JS twin)                      | N/A                                                                                                 |

### Kilo / CodeQL residual notes

- Kilo WARNING on raw `saveToUrl` → fixed in tip (`safeSaveToUrl`); source-pattern test added.
- CodeQL residual on imageSlider after structural fix (#1790–#1797) → sink-line `// codeql[js/xss-through-dom]` + whole-file path excludes; author mitigation replies present; **suppressions.md not updated**.

## Recommendation

**request-changes**

## Gate

- Blocking bugs: **2**
- May commit/push: **no**

Do not merge until blocking issues are fixed and Erlang re-reviews the fix pack. Also complete PR review-thread protocol (`resolveReviewThread` after mitigation replies) for merge readiness where threads remain open.

## Issues

### Issue 1 -- Severity: bug

- File: `modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js:26` (and lockstep UserDependency copy:26)
- Description: New non-trivial security helper `safeThumbnailSrc` (scheme rejection for `javascript:` / `data:` / `vbscript:`) has **zero** unit/behavioral tests, while the PR escalates residual CodeQL to **whole-file** `query-filters` excludes for both package copies. Hard gate: missing behavioral tests for new security helpers / rejection paths; source-grep-only elsewhere does not cover this helper. Without tests, a future edit can re-break scheme rejection and CI stays green under the path exclude.
- Suggestion:
  1. Extract or mirror a pure test of `safeThumbnailSrc` (prefer testing production code, not a reimplemented twin) with cases: empty/null; relative CMS path passthrough; reject `javascript:…`, `JAVASCRIPT:…`, `data:…`, `vbscript:…`; trim whitespace.
  2. Optional: one DOM behavioral assert that setting a malicious title does not produce an img whose `src` starts with a blocked scheme.
  3. Place tests under `WebUI/src/test/js/` (or package test home) so Vitest runs them with the rest of the PR suite.
- Status: open
- Pattern-id: `tests.missing-behavioral` / `security.sanitizer-without-rejection-tests`

### Issue 2 -- Severity: bug

- File: `.github/codeql/codeql-config.yml:263-281` (and related sink-line `// codeql[js/xss-through-dom]` in both imageSlider copies ~:138–139 / ~:341–343)
- Description: CodeQL PR playbook + contracts/C3 require every `codeql-config.yml` path `query-filter` (and documented residual suppressions) to have a matching row in `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` with `file_path = .github/codeql/codeql-config.yml` (and rows for sink-line suppressions on the product files). This PR adds two path excludes for imageSlider residuals (#1431–#1434 / #1790–#1797) but **does not update `suppressions.md`**. That is incomplete disposition and breaks the repo’s residual-audit trail.
- Suggestion: Add suppressions rows for:
  - path excludes on both imageSlider paths (rule `js/xss-through-dom`, residual reason matching config `reason`, cite `safeThumbnailSrc` + regression test name once Issue 1 exists);
  - sink-line suppressions on the `.attr("src", …)` sites if retained after path exclude (or remove redundant sink-line comments if whole-file exclude is intentional sole residual barrier — prefer ladder: tests → model if possible → sink-line → path filter).
- Status: open
- Pattern-id: `security.broad-codeql-exclude-undocumented`

### Issue 3 -- Severity: suggestion

- File: `WebUI/src/test/js/percFormViewDomText.test.js:1-44`, `percRedirectHandlerDomText.test.js:1-30`, `percRssAndMostReadDomText.test.js:1-57`, `percUserView.test.js:349-370`
- Description: New tests for form escape, redirect dialog, RSS description, and UserView residual role-transfer are **source-grep-only** (or reimplemented allow-list logic that does not call production `safeHeadingTag`). Existing `percUserView.test.js` behavioral suite covers `updateAssignedRoles` / `updateAvailableRoles` / narrow-search well; residuals and other sinks do not get runtime proof. Acceptable as *supplemental* anti-regression for mechanical `.html`→`.text`, inadequate as sole claim of “cleared XSS” for non-trivial paths.
- Suggestion: Prefer at least one jQuery/DOM behavioral case per non-trivial fix (form: malicious `saveToUrl` renders as text; RSS: description with `<img onerror>` does not create elements; redirect: path with `<>` is textContent only). For `safeHeadingTag`, export or eval production function instead of mirroring.
- Status: open
- Pattern-id: `tests.structural-only`

### Issue 4 -- Severity: suggestion

- File: `WebUI/src/test/js/percRedirectHandlerDomText.test.js:13-18` (and `percUserView.test.js:349-370`)
- Description: Lockstep product copies edited in this PR include **war** (`WebUI/war/plugins/PercRedirectHandler.js`, `WebUI/war/views/PercUserView.js`), but regression tests do not assert war paths. Main plugins/views + legacy are covered; war can drift again unnoticed. (`WebUI/war/**` is paths-ignored for CodeQL, so scan will not catch war regressions either.)
- Suggestion: Extend source-pattern loops to include war paths (or document that war is build-generated from a single source of truth and stop dual-editing if that is the real model).
- Status: open
- Pattern-id: `maintainability.multi-copy-partial-test`

### Issue 5 -- Severity: suggestion

- File: `modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js:48-59` (pre-existing residual, not introduced by this PR)
- Description: Description sink is fixed (plain text + escape). Feed **title** and **link** are still concatenated into HTML attributes/body (`feed.title`, `feed.link`) before `parseHTML` + href sanitize. Href sanitize closes some link schemes; title attribute/body remain HTML-injection surfaces from external feeds.
- Suggestion: Follow-up ticket: escape `feed.title` (and other feed fields) the same way as description; keep `sanitizeUrlForHref` on links. Out of scope to block this PR if not claimed as fully closed for all RSS sinks.
- Status: open

### Issue 6 -- Severity: suggestion

- File: `modules/perc-common-ui-bundle/src/main/js/views/PercMostReadBlogPostsView.js:66-69`
- Description: `safeHeadingTag(name, fallback)` returns **unvalidated** `fallback` when `name` fails the allow-list. Call sites pass literal `"h2"`/`"h3"` today (safe), but the helper is footgun if reused with untrusted fallback.
- Suggestion: Validate `fallback` with the same allow-list before return; default to `"h2"` only after both fail.
- Status: open

### Issue 7 -- Severity: nit

- File: `WebUI/src/test/js/percRssAndMostReadDomText.test.js:8-9`
- Description: Imports `jquery` and `beforeEach` but never uses them (dead imports).
- Suggestion: Remove unused imports.
- Status: open

### Issue 8 -- Severity: nit

- File: `modules/perc-packages/.../imageSlider/.../percImageSlider.js:137-140` (both copies)
- Description: `// codeql[js/xss-through-dom]` sits on the `"src"` string line; residuals still hit the next argument line, then whole-file exclude was added. Suppression placement is noisy relative to playbook “exact sink line.”
- Suggestion: After Issue 1–2, either put suppression on the exact residual line CodeQL cites or rely on documented path exclude alone and drop redundant comments.
- Status: open

## Cross-platform path checklist

Applied (diff touches tests that resolve filesystem paths; no installer/packaging I/O).

|                         Check                          |                   Outcome                    |
|--------------------------------------------------------|----------------------------------------------|
| Hardcoded `/` or `\\` filesystem joins in product code | **No issues** (URL/CSS/class selectors only) |
| Tests use `path.resolve` / `dirname` / `fileURLToPath` | **OK** (portable Node path APIs)             |
| `srcPath.split("/")` in test display names             | **Not a gate** (display only; not FS open)   |
| Unix-only roots / Windows-only drives                  | **No issues**                                |
| Line-ending assertions                                 | **No issues**                                |
| Required Unix-only scripts                             | **No issues**                                |

**Cross-platform path review: no issues.**

## Memory patterns hit

- Missing **behavioral** unit tests for new/changed non-trivial logic (`safeThumbnailSrc`)
- Tests that only grep source / reimplement logic without exercising production
- Multi-copy shared WebUI/package assets (imageSlider + socialButtons correctly dual-edited; war/legacy RedirectHandler + UserView dual-edited)
- Broad CodeQL path excludes without full residual documentation (`suppressions.md`) and without runtime tests for the sanitizer
- Security helpers without rejection-path tests

## What looks good

- Mechanical XSS fixes (`.html`→`.text`, option factories, form element factory) match known-good patterns used elsewhere in WebUI.
- PercUserView already has solid fail-then-pass **behavioral** coverage for role list / i18n sinks; residual transfer path fix is consistent.
- Kilo `saveToUrl` finding addressed consistently with sibling metadata fields.
- imageSlider and socialButtons SupportFile / UserDependency pairs edited in lockstep (no multi-copy half-fix).
- PercRssView description path: intentional safer plain-text behavior documented in PR body.
- lightbox selector fix avoids attribute interpolation into jQuery selectors (correct approach).
- TinyMCE `escAttr` + regex title parse closes DOM-parse-of-HTML-fragment title sink.

## Handoff

1. **Recommendation:** `request-changes`
2. **May commit/push:** **no**
3. Fix Issue 1 (behavioral tests for `safeThumbnailSrc`) and Issue 2 (`suppressions.md` rows) first; re-run Vitest WebUI suite; re-run Erlang.
4. Durable report: `docs/ai-generated/code-reviews/2026-07-18-pr-1360-js-xss-through-dom-erlang.md`
5. After code fixes, complete PR thread resolve protocol for remaining CodeQL residual threads.

