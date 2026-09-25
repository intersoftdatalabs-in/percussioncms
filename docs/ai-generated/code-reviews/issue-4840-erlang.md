<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4840 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--machine-only`
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --diff tmp/issue-4840-working.diff --machine-only`
- `--git-base origin/main` is empty here (HEAD `68d86eeeff` == `origin/main`; all work uncommitted). Machine pass used a unified diff with `a/`/`b/` prefixes covering tracked edits **and** the untracked Playwright spec.
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4840 (parent #4532 slice 32)
- Branch: `fix/issue-4840-editor-community-save`
- Files reviewed (6 modified + 1 untracked):
  - `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java`
  - `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java`
  - `projects/sitemanage/src/test/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapperTest.java`
  - `WebUI/src/test/ts/editor/EditorHost.test.tsx`
  - `WebUI/src/test/ts/editor/widgets/CommunityFieldWidget.test.tsx`
  - `product-docs/8.2/admin/content-explorer.md`
  - `modules/perc-qa-automation/frontend/tests/editor-community-field.spec.js` (untracked)
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`
- Prior report: none for #4840
- Rule-file diffs: none
- Cross-platform path review: no new filesystem path I/O. Playwright `editorSpaUrl` joins URL path segments with `/` (correct for URLs). Java mapper uses `Integer.toString` and field names, not `Path`/`File`.

## This-diff behavior

`GET`/`PUT` editor fields already treat `sys_communityid` as editable (`applyUpdates` writes it). `fromContentItem` still omits the row when the content-item field map has no community key. `mergeEditorRows` then injects a schema community widget with value `""`, so reopen (and a save that rebuilds draft from the response) can show empty even when `PSComponentSummary.getCommunityId()` is set.

This slice adds `PSItemEditorFieldsMapper.fillCommunityWhenAbsent`: when the payload has no `sys_communityid` row and `communityId > 0`, copy the status id. A present row, including blank, is left alone. `PSItemService.getEditorFields` and `saveEditorFields` call it after `fromContentItem`. Save still uses `after != null ? after : sum` for revision (same source as before).

Change-class companions in this tree: mapper unit test for fill (absent / explicit blank / id `0`), Vitest after-save select value + read-only widget, Playwright surface spec (`@explorer-content-editor`), product-docs `content-explorer.md`. Internal CM1 `sitemanage` item REST, not a new `rest` adaptor.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **8** finding(s), **0** bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 8
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java:141 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 141)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1882 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1882)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2017 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2017)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2018 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2018)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2022 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2022)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2023 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2023)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2029 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2029)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:413 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveEditorFields` cognitive=18 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

Gate counts **in-diff** bugs only. CLI: 7 files, **0 in-diff findings**. Preexisting `paths.hardcoded_sep` hits are false positives (Javadoc `/`, log/message strings, CMS finder paths). `saveEditorFields` complexity is preexisting; this slice adds a null-guarded helper call.

Independent review of the uncommitted pack:

- `fillCommunityWhenAbsent` is the right layer for reopen: `fromContentItem` cannot invent a community id the item map omitted; summary `communityId` is the status value the picker should show. Call sites run only on `fromContentItem` output (mutable `ArrayList`), so `fields.add` is safe on this path.
- Blank-present short-circuit matches the stated contract (do not overwrite an explicit clear in the same payload). `communityId <= 0` is ignored. After persist, a later GET with the field omitted still backfills from status — that is the reopen path, not a silent restore inside one response.
- Mapper test exercises the three branches (copy when absent, keep blank, ignore `0`). That is behavioral coverage for the new helper. Service wiring is a two-line null check; `PSItemServiceSaveEditorFieldsTest` still passes because Mockito `getCommunityId()` defaults to `0` and fill no-ops.
- Vitest: after save, draft is rebuilt from the mock payload that includes `sys_communityid=20`; the new `waitFor` asserts the select stays at `20`. Widget `readOnly` disables the control. Playwright follows `editor-rich-controls.spec.js` (route-mocked fields API, `data-testid`, view mode hides Save). Product-docs row matches that contract.
- No secrets. New Playwright file uses Intersoft 2026 Apache header. No rule-file edits.

### Suggestions (non-blocking)

- `fillCommunityWhenAbsentCopiesStatusIdAndKeepsExplicitValue` asserts `get(0).getValue()` only — also assert `getName()` is `sys_communityid`.
- Class javadoc on `PSItemEditorFieldsMapper` still says system fields except `sys_title` are omitted; community is now an explicit exception on read and write.
- Optional: one `getEditorFields` mock test that item fields omit community and summary id `20` appears on the payload (locks the GET call site). Not required for this gate; the helper already has behavioral tests.

## Recommendation

approve

## Issues

_No in-diff bugs, missing behavioral tests, or non-portable path I/O._

Gate: PASS
May commit/push: yes

> Co-Authored by Grok Build using grok-4.6 with agent Erlang Shen.
