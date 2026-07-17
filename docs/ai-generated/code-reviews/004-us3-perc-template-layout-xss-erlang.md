## Summary

Commit `afef1ea75` fixes 6 CodeQL `js/xss-through-dom` alerts in
`perc_template_layout_helper.js` and `perc_template_layout_class.js` by
replacing `$(templateCodeText).hasClass("perc-horizontal")` with a new
pure-regex helper `percTemplateCodeHasClass(markup, className)` that never
hands untrusted `templateCode` text to jQuery's HTML parser. The change is
applied identically (logic-for-logic) across all 3 deployed lockstep copies
of each file, and is backed by two new Vitest files with genuinely behavioral
assertions (jQuery-call spy for the reachable helper; direct extraction +
execution of the real `percTemplateCodeHasClass()` for the dead-code class).
I independently reproduced the fail-then-pass claim by reverting only the 6
source files and rerunning the new tests, and independently confirmed
`Perc_Template_class` is dead code via repo-wide grep. The fix itself is
sound and I found no bugs in the diff. The one high-value finding is
pre-existing and out of this diff's file set: the Vitest `include` globs in
`WebUI/src/main/frontend/vite.config.ts` never discover
`WebUI/src/test/js/**` (including both new test files and 7 earlier ones)
when run via the documented `npm run test`/`npm run test:watch` workflow —
they only run when `vitest` is invoked with `--root` pointed at `WebUI/`. This
does not block this commit (it predates it and is not touched by it), but it
means the commit's own "tests pass" claim is only true under a non-default
invocation, and it silently suppresses regression coverage for this whole
test family in CI/local dev today.

## Scope

- Base: `development`
- Head: `afef1ea75` (single commit, tip of `004/us3-perc-template-layout-xss`)
- Files: 8 changed (6 production JS across 3 lockstep copies × 2 files; 2 new Vitest test files)
- Prior report: none found under `docs/ai-generated/code-reviews/` for this exact topic (the branch-referenced `004-us3-perc-widgetbuilder-xss-erlang.md` does not exist in this repo checkout — likely a different session/topic; skimmed `docs/ai-generated/code-reviews/README.md` conventions instead)
- Memory patterns hit:
  - `patterns.md` → Tests: "Structural / registry / XML string presence treated as sole proof of runtime behavior" — checked for and **not** present here (both files exercise real behavior)
  - `patterns.md` → Maintainability: "Multi-copy shared WebUI / package assets edited in only one of several lockstep paths" — checked for and **not** present here (all 3 copies of both files verified byte-for-byte-equivalent logic)
  - New candidate pattern (not yet promoted, see below): Vitest test files whose physical location does not match the `vite.config.ts` `test.include` root resolve silently to "not discovered" rather than an error when run via the plain `npm run test` script

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion
- File: WebUI/src/main/frontend/vite.config.ts:43-50 (pre-existing, not touched by this commit)
- Description: `test.include` is `["src/test/ts/**/*.{test,spec}.{ts,tsx}", "src/test/js/**/*.{test,spec}.js"]`, resolved relative to `root: "."`, which is the `frontend/` directory itself. The two new files under review (`WebUI/src/test/js/percTemplateLayoutHelper.test.js`, `WebUI/src/test/js/percTemplateLayoutClass.test.js`) — and 7 pre-existing sibling files in the same directory (`percListEditorWidget.test.js`, `percCssGalleryView.test.js`, `percUserView.test.js`, `percSectionTreeDialog.test.js`, `percGetDashboardColumn.test.js`, `percChangeTemplateDialog.test.js`, `compat.test.js`) — live at `WebUI/src/test/js/`, one directory level above `frontend/`. Running the documented command (`cd WebUI/src/main/frontend && npm run test`, per `WebUI/AGENTS.md`) therefore silently runs only the 2 files under `frontend/src/test/{js,ts}` (31 tests) and never touches this entire family of regression tests — no error, no "0 of N" warning, just absence. I confirmed this by running `npx vitest run --config vite.config.ts` with no path filter (31 tests from 2 files) versus the same command with an explicit path to one of the files under `WebUI/src/test/js` (`No test files found, exiting with code 1`), versus `--root ..\..\..` (correctly finds and runs them). This is **not introduced by this commit** — `vite.config.ts` is untouched in this diff, and the same gap already existed for the 7 earlier files (added across at least PRs `#1310`, `#1306`, which similarly live under `WebUI/src/test/js`). It is flagged here at `suggestion` (not `bug`, since it predates and is outside the file set of this diff) but with high urgency: the commit's own "Constitution III fail-then-pass" verification claim, and this file's own report verification, both required a non-default `vitest --root` invocation to actually execute — a developer or CI job following the documented `npm run test` workflow gets a false sense that these tests ran (exit code 0, tests "passed") when in fact 0 of the relevant assertions executed.
- Suggestion: File a follow-up to either (a) move `WebUI/src/test/js/**` and `WebUI/src/test/ts/**` under `WebUI/src/main/frontend/src/test/**` to match the `root: "."` resolution, or (b) change `test.include` in `vite.config.ts` to resolve against the `WebUI/src` tree (e.g. `"../../test/js/**/*.{test,spec}.js"`, `"../../test/ts/**/*.{test,spec}.{ts,tsx}"`) or set `test.dir` accordingly, and verify with a clean `npm run test` (no path arguments, no `--root` override) that the full known test count (39 tests across 9+ files, not 31) is reported. This is not required before merging this specific security-fix commit, since it is a pre-existing, wider-scope gap, but should not be treated as "solved" by this task either.
- Status: open
- Pattern-id: tests.vitest-include-root-mismatch

## Independent verification performed

- **Dead-code claim (`Perc_Template_class`)**: confirmed via `grep -r "Perc_Template_class"` across the full repo — the only matches are the three lockstep definition sites (`$.Perc_Template_class = function () {...}`) and the new test file's comments. No `new $.Perc_Template_class(...)`, `.Perc_Template_class(...)`, or any other call/instantiation site exists anywhere in the repo. Also confirmed real `templateCode` markup generators (`PercTemplateModel.js`, `perc_template_schema.js`) always emit the CSS class as the first/root-element attribute (`<div class="perc-horizontal">...`), which matches the "first `class="..."` match wins" heuristic used by `percTemplateCodeHasClass()` for real production data — the theoretical edge case where a root element has no class but a later nested element does (which would make the regex heuristic diverge from `$(html).hasClass()`'s root-element-only semantics) does not occur in any of this codebase's actual `templateCode` producers.
- **Fail-then-pass claim**: reproduced independently. Ran both new test files against the post-fix (committed) source (`npx vitest run --config vite.config.ts --root ..\..\.. src/test/js/percTemplateLayoutHelper.test.js src/test/js/percTemplateLayoutClass.test.js` from `WebUI/src/main/frontend`) — 2 files, 8 tests, all passed. Reverted only the 6 production source files (`git checkout afef1ea75~1 -- <6 files>`) without touching the tests, reran — 2 files failed, 6 of 8 tests failed exactly as the commit message describes (the security-relevant assertion in `percTemplateLayoutHelper.test.js` failed with "expected true to be false"; all 5 assertions in `percTemplateLayoutClass.test.js` failed, 4 with "percTemplateCodeHasClass() not found"; the 2 unrelated functional-correctness assertions in the helper test still passed since they don't depend on the fix). Restored the 6 source files (`git checkout HEAD -- <6 files>`), confirmed `git status` clean and both test files passing again (8/8).
- **Lockstep verification (all 3 copies × 2 files)**: diffed each of the 6 changed production files individually. `cm/plugins` and `legacy/plugins` copies of `perc_template_layout_helper.js` are byte-identical (same blob hash `676b94df3..78cbc0f7b`); `cm/classes` and `legacy/classes` copies of `perc_template_layout_class.js` are byte-identical (`2799113fc..10398c5f5`). The `WebUI/war/...` copies of both files carry the same logic (identical helper function, identical call-site refactor) but preserve that copy's pre-existing tabs/no-line-wrap formatting style rather than the 2-space/wrapped style used in the `cm`/`legacy` copies — consistent with "accounting for each copy's pre-existing formatting style" and not a lockstep violation.

## Scope-creep check (`this.Type` pre-existing bug)

`Perc_Template_class.parseRegions()` throws unconditionally today on `this.Type.TEMPLATE` because the constructor never initializes `this.Type` — an unrelated, pre-existing defect that predates this CodeQL fix. Agreeing with the commit's choice to leave it alone: the class is confirmed dead code (see verification above), the bug is orthogonal to the `js/xss-through-dom` sink being closed, and "fixing" `this.Type` here would (a) expand this security PR's blast radius into unrelated, currently-unexercised legacy code with its own unknown risk, and (b) require its own design decision (what should `this.Type` actually be?) that has nothing to do with the CodeQL alert. Testing around it by extracting and directly exercising `percTemplateCodeHasClass()` — while still asserting no jQuery/HTML-parsing sink is present in that function body — is the right scope boundary for a security-fix commit; correctly not conflated with a "dead code cleanup" or "fix pre-existing bug" change.

## Cross-platform path review

N/A — no file I/O, path construction, or path assertions in this diff (pure in-memory string/regex logic in browser JS and Vitest tests using existing `readFileSync`/`resolve`/`fileURLToPath` patterns already used elsewhere in this test directory).
