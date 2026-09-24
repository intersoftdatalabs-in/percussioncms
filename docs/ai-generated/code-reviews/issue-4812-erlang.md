<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4812 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--machine-only`, `--diff` of uncommitted 4812 files vs HEAD
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4812 (parent #4530, slice 31: Explorer multi-select Take Down)
- Branch: `fix/issue-4812-explorer-multi-takedown` (HEAD `66e76f80a1` == `origin/main`; all work is uncommitted)
- Files reviewed:
  - `WebUI/src/main/ts/contentExplorer/actionDispatch.ts` (modified)
  - `WebUI/src/main/ts/contentExplorer/itemPublish.ts` (modified)
  - `WebUI/src/main/ts/contentExplorer/messages.ts` (modified)
  - `WebUI/src/test/ts/contentExplorer/actionDispatch.test.ts` (modified)
  - `WebUI/src/test/ts/contentExplorer/itemPublish.test.ts` (modified)
  - `product-docs/8.2/admin/content-explorer.md` (modified)
  - `modules/perc-qa-automation/frontend/tests/explorer-multi-takedown.spec.js` (untracked)
- `--git-base origin/main` is empty here (no commits vs main) and would omit the untracked Playwright spec; analysis used `git diff HEAD` plus a read of the untracked spec
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`
- Hard-block this turn: in-diff bugs, missing behavioral tests, or non-portable path/file I/O in **this** diff only
- Out of scope: preexisting `dispatchAction` cognitive/cyclomatic size; parent #4530 issue-comment progress (not a code companion)

## This-diff behavior

Explorer Take Down today unpublishes only the single active row. Multi-select Stage and Remove from Staging already loop the checkbox selection. This slice adds the same batch for Take Down.

`dispatchAction` routes `selectedItems.length >= 2` into `takedownMultiSelection`: partition with `partitionStageSelection` (pages/assets eligible; folders and other types skipped; duplicate ids once), one confirm (`CONFIRM_TAKEDOWN_MULTI` with `{count}` = eligible length), cancel returns without work. `runTakedownBatch` calls `takedownSelectedItems` when `onTakedown` is absent.

`ContentExplorerShell.handleMenuInvoke` does not pass `onTakedown`, so the live shell uses `takedownSelectedItems`: per eligible item, `loadLinkedPagesForTakedown` then `takedownSelectedItem` (GET, or PUT when the linked list is non-empty). One item failure is recorded; the rest of the selection still runs. `refresh` is true only when `takenDownIds.length > 0`. `describeTakedownBatch` names skipped folders, skipped other types, and per-item HTTP / message failures.

New `EXPLORER_MSG` keys: `CONFIRM_TAKEDOWN_MULTI`, `TAKEDOWN_SKIPPED_FOLDERS`, `TAKEDOWN_SKIPPED_OTHER`, `TAKEDOWN_BATCH_INCOMPLETE`, `TAKEDOWN_NOTHING_ELIGIBLE`.

Change class: **WebUI Explorer product screen (multi-select Take Down)**. Companions in this tree: item-publish helper, action dispatch, i18n keys, Vitest (`actionDispatch` + `itemPublish`), Playwright `explorer-multi-takedown.spec.js` (peer of `explorer-multi-stage.spec.js`, tags `@explorer-multi-takedown`, a11y on the happy path), `product-docs/8.2/admin/content-explorer.md` Take Down row.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 6 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:780 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=292 (max 15), cyclomatic=183 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

Gate counts in-diff bugs only. Residual LLM skipped (`--machine-only`).

Issue 1 is preexisting size of `dispatchAction`. The new multi-select branch is ~10 lines at the Take Down arm plus extracted `takedownMultiSelection` / `runTakedownBatch` (same shape as Stage). **Out of scope** for this review.

Independent review of the uncommitted Take Down batch:

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/contentExplorer/itemPublish.test.ts:283
- Description: `takedownSelectedItems` is covered for skip-folder, skip-other, HTTP 403 isolation, and no `/staging/` URL. The fetch mock answers `findLinkedItems`, but the test never asserts that lookup ran, and never asserts PUT when the linked list is non-empty. That GET/PUT choice is the contract that differs from Stage and is already proven on `takedownSelectedItem` alone.
- Suggestion: Assert `findLinkedItems` URLs for each eligible id, and add one batch case where a non-empty linked list causes PUT on that item’s takedown URL.
- Status: open

### Issue 3 -- Severity: nit

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:237
- Description: `selectedItems` JSDoc still names only Stage (`When length is 2 or more, Stage uses one confirm…`). Take Down and Remove from Staging already share the field.
- Suggestion: Name Take Down (and Remove from Staging) in that comment.
- Status: open

## Behavioral tests

| Behaviour | Coverage |
|-----------|----------|
| One confirm; eligible pages + asset unpublished; folder named in message | `actionDispatch.test.ts` multi-select Take Down |
| Cancel takes down nothing | same file |
| One HTTP 403 is incomplete; other item still runs; `refresh` true | same file |
| Folder-only selection: no confirm work, names folders | same file |
| Batch helper: skip folder/other, 403, no staging URL | `itemPublish.test.ts` `takedownSelectedItems` |
| Screen: confirm copy, folder skip chrome, list epoch, a11y | `explorer-multi-takedown.spec.js` |
| Screen: cancel; 403 names About | same spec |

Single-item Take Down (linked-path confirm, GET/PUT, FORBIDDEN 200) is unchanged and remains covered by existing tests.

## Change-class closure

| Companion | Status |
|-----------|--------|
| `takedownSelectedItems` + `describeTakedownBatch` | present |
| `dispatchAction` multi-select arm | present |
| `EXPLORER_MSG` keys | present |
| Vitest dispatch + itemPublish | present |
| Playwright surface spec + `@explorer-multi-takedown` | present (untracked) |
| `product-docs/8.2/admin/content-explorer.md` | present |
| Explorer a11y (`expectNoSeriousA11yViolations` on happy path) | present |

No new React component; Vitest `renderA11yGate` does not apply. No REST/sitemanage adaptor change. No filesystem I/O.

## Cross-platform path / file I/O checklist

- No new filesystem joins (`"/"` / `"\\"`). CMS item paths (`/Sites/…`, `/Assets/…`) and sitemanage URLs correctly use `/`.
- Tests do not assert OS path shapes or Unix-only temp roots.
- Playwright `page.route` globs are URL paths.
- **Outcome:** clean.

Memory patterns hit: missing behavioral test (addressed for batch orchestration; linked PUT-in-batch is a suggestion); WebUI Playwright companion (present); product-docs companion (present); non-portable path I/O (not introduced).

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: yes

Gate: PASS

May commit/push: yes

> Co-Authored by Grok 4.6 using grok-4.6 with agent Erlang Shen.
