<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4813 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of uncommitted 4813 files
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4813
- Branch: `fix/issue-4813-explorer-multi-schedule` (HEAD == `origin/main` `99cf8a47e5`; all work is uncommitted)
- Files reviewed (12 tracked, `514 insertions / 27 deletions`):
  - `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx`
  - `WebUI/src/main/ts/contentExplorer/ScheduleDatesDialog.tsx`
  - `WebUI/src/main/ts/contentExplorer/actionDispatch.ts`
  - `WebUI/src/main/ts/contentExplorer/itemScheduleDates.ts`
  - `WebUI/src/main/ts/contentExplorer/messages.ts`
  - `WebUI/src/main/ts/contentExplorer/schedulePickerSession.ts`
  - `WebUI/src/test/ts/contentExplorer/ScheduleDatesDialog.test.tsx`
  - `WebUI/src/test/ts/contentExplorer/actionDispatch.test.ts`
  - `WebUI/src/test/ts/contentExplorer/itemScheduleDates.test.ts`
  - `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js`
  - `product-docs/8.2/admin/content-explorer.md`
  - `product-docs/8.2/admin/publishing.md`
- `--git-base origin/main` is empty here (HEAD == main). Analysis used a unified diff with `a/` `b/` prefixes so the machine pass saw all 12 files.
- Memory: `~/.agents/skills/erlang/PATTERNS.md`

## This-diff behavior

Issue 4813 (parent #4530 slice 32): from Content Explorer, one Schedule dialog sets or clears the same publish start/end (and comments) on every selected page or asset. Folders and other non-publishable rows are skipped. Partial failure is not full success. Single-item Schedule stays the highlighted-row path when fewer than two checkboxes are checked.

`publishableScheduleTargets` keeps pages and assets, drops empty ids / folders / `resolvePublishKind === "none"`, and writes duplicate ids once. `scheduleSelectedItems` POSTs `{ ItemDates }` per target, continues after a per-item throw, and reports `{ saved, skipped, failures }`. `scheduleSelectedItem` is that batch with a boolean.

`dispatchAction` for Schedule: two or more checked rows → those publishable targets; otherwise the highlighted item. Empty targets: folders-only → `ACTION_NEEDS_ITEM`, other non-publishable → `ACTION_UNAVAILABLE`. Seed dates come from `GET getitemdates/{seed}`. One picker (`applyCount: targets.length`) then one confirm (`CONFIRM_SCHEDULE_MULTI` vs `CONFIRM_SCHEDULE`). Dialog cancel or confirm cancel returns `rest` with no write. `applyScheduleDates` uses `ctx.onSchedule` when tests inject it; production Explorer does not pass `onSchedule`, so it writes through `scheduleSelectedItems`. Any failure returns `SCHEDULE_PARTIAL` plus `formatScheduleBatchFailure` (item names are data). `refresh` is true only when `saved > 0`.

The dialog shows `SCHEDULE_MULTI_HINT` when `applyCount > 1`. Shell confirm still runs `message(body)` on the catalog key.

Change-class companions are present: Vitest for batch + dispatch + dialog hint, Playwright `explorer-action-dispatch.spec.js` (one dialog, cancel writes nothing, both pages POST, About `FORBIDDEN` is an error), product-docs Explorer Schedule plus a Publishing site-workspace cross-note. Cross-platform path I/O: none in this slice (CMS `/Sites` paths and REST `/` joins).

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:812 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=302 (max 15), cyclomatic=192 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang commentary

`dispatchAction` complexity is preexisting and out of this slice. No in-diff machine bugs. Residual `--models` (`dev-coder:latest`, `require_llm = true`) returned exit 0 with no `llm.error` and no extra in-diff rows.

Behavioral coverage matches the new logic: same dates on each page/asset with folders skipped, one picker, cancel writes nothing, partial `FORBIDDEN` is not full success, dialog states the multi-selection, Playwright hits the live toolbar/dialog/POST/error region. Single-item Schedule tests remain.

Suggestions (not blocking): a dispatch case for two folders only (`ACTION_NEEDS_ITEM`); `applyScheduleDates` with `onSchedule` always reports `skipped: 0` (production does not pass `onSchedule`). Parent #4530 Agent progress is issue-tracker work, not this diff.

Recommendation: **approve**.

## Gate

- Blocking bugs: 0
- Missing behavioral tests: no
- Non-portable paths: no
- Recommendation: approve
- May commit/push: yes
