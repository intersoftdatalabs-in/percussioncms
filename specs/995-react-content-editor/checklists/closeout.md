# Closeout: Editor verification battery (T001–T015) — #6153

**Branch**: `fix/issue-6153-editor-verification-battery` · **Date**: 2026-11-12
**Method**: verify-first — execute the battery; implement only proven gaps.

## T001–T002 setup

- Owning modules + AGENTS hierarchy read: root `AGENTS.md`, `WebUI/AGENTS.md`
  (React-only, Playwright hard gate), `rest/AGENTS.md`, `projects/sitemanage/AGENTS.md`,
  `modules/perc-qa-automation/AGENTS.md`.
- Branch based on `night-issue-prs-main` (tip `70e8429b`, same as `origin/main` lineage).
- JDK 21 via `/usr/lib/jvm/java-21-openjdk` (`openjdk 21.0.13`).
- WebUI toolchain: `npm ci` in `WebUI/src/main/frontend` (canonical vitest cwd;
  root `WebUI/package.json` is a legacy mirror without devDeps — `npx vitest`
  from `WebUI/` root fails on `@testing-library/jest-dom` resolve).

## T003–T004 inventory

- `specs/995-react-content-editor/checklists/coverage.md`: FR-001–014 mapped to
  files + Vitest/Playwright tests. All FRs have covering tests; three helper
  modules lacked dedicated Vitest files (see T005).

## T005 Vitest (new tests for proven gap)

- Added `WebUI/src/test/ts/editor/itemFieldsApi.test.ts` (9 tests:
  `visibleEditorRevision` branches, fetch/save unwrap incl. PascalCase +
  zero-revision omission, checkout/checkin URL + comment query),
  `pageSiteImpactApi.test.ts` (2 tests: envelope parse, empty payload),
  `messages.test.ts` (5 tests: TMX prefix, non-blank defaults, no invisible/
  zero-width chars, UPPER_SNAKE keys).
- Full editor suite: **87 files, 946 tests, all pass** (baseline before: 84 files / 929).

## T006–T009 Playwright battery (H2 cell `perc-matrix-cms-h2`)

- Cell: `perc-devctl.py qa-up --skip-image-build` → RESULT:OK (95.5s, healthy).
- Installer jar reused from the `night-issue-prs` worktree build (same base tip;
  this slice adds tests + spec docs only — no production delta).
- Specs: `editor-host-checkout` (T006), `editor-host-text-save` (T006),
  `editor-host-create-item` + `editor-page-template` (T007),
  `editor-rich-controls` (T008), `editor-host-save-reload` (T009).
- Result: **13/13 green on H2** (2026-11-12, cell `perc-matrix-cms-h2`):
  checkout 5/5, text-save 1/1, create-item 2/2, page-template 1/1,
  rich-controls 2/2, save-reload 2/2. T010 fixed 5 stub-freshness failures
  (catch-all 404 stubs + console filter, mirroring save-reload spec).

## T010 gap fixes

- WP1 proved 5 stub-freshness failures (not product bugs: functional assertions
  passed; newer load-time calls siteimpact/page, allowedWorkflows, path/item were
  unstubbed in pre-#5739 specs and leaked 400/500 console noise). Fixed with
  catch-all 404 stubs + standard console filter in editor-host-checkout.spec.js
  and editor-page-template.spec.js (mirrors editor-host-save-reload.spec.js).
  Re-run 6/6 green; full battery 13/13.
  One more candidate investigated and cleared: `siteImpactSiteFromPath("//Sites/…")`
  returns `""` for double-slash paths, but the server contract
  (`PSItemService.buildSiteImpactJson` → `PSItemProperties.getPath()`,
  `PSItemServicePageSiteImpactTest`) sends canonical single-slash `/Sites/…`
  paths — existing test pins `/Sites/Demo/Home → Demo`. Not a real-world break;
  left unchanged per verify-first rule.

## T011 i18n

- N/A: no new editor strings added (test-only slice). `EDITOR_MSG` convention
  pinned by `messages.test.ts`; no `perc-i18n` TMX change required.

## T012–T015 closeout

- T012: tests-only change — no Maven module changed; no `clean install` required
  (WebUI Vitest battery is the module gate; 946/946 green).
- T013: Erlang CLI pre-push review (report in PR body + `docs/ai-generated/code-reviews/issue-6153-erlang.md`).
- T014: this note + coverage.md + ticked tasks.
- T015: PR opened; pause for review/merge.

## Disposition

- #4530 (editor lineage): battery evidence linked from PR.
- #6110: no disposition change from this slice.
