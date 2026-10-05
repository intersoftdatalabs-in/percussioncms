<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Pre-push local code review — PR #5236

Independent Erlang confirmation at HEAD `5056b622` (code still `5cc28b80`; the tip commit is the prior verdict, not a fix). The machine report below is the full `mkd-code-review analyze --format markdown` stdout from this pass (`--pack percussion --gate advisory --git-base origin/main`). Ollama `dev-coder` returned CUDA out-of-memory; machine findings were kept. Preexisting rows do not block. In-diff machine bugs: 0. The return-to-restored-item bug below still blocks.

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 16 analyzed
- In-diff: 0 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2083 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2083)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2218 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2218)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2219 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2219)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2223 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2223)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2224 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2224)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2230 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2230)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Erlang verdict

**request-changes.** Do not merge. Reconfirmed on this pass: in-diff machine bugs: 0. The first content-item switch after restore is fixed. Returning to the restored item is not. No code changed after `5cc28b80`.

`sameItemReload` is `reloadItemId === itemId && reloadToken > 0` (`WebUI/src/main/ts/contentExplorer/RevisionsPanel.tsx:137`). A successful restore sets both and never clears them (`:215-216`). The shell keeps one `RevisionsPanel` mounted and only changes `itemId` between content items (`WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx:4318-4338`); there is no `key`. Folder and empty selection unmount the panel, so they do not hit this.

After restore on item A, open item B (session resets — covered), then select A again:

- `reloadItemId` is still A and `reloadToken` is still > 0, so the return is treated as a same-item reload.
- Load failure: `:156-160` already set `loading` because `prev.forItem` is B, then `:189-191` only `setRestoreError` and returns. The panel stays on Loading and the error state is never shown.
- Load success: `:173-180` does not assign compare from/to, so the selects keep B's revision ids (or stay null if B's load never finished). Compare then sends those ids for A. Pending confirm and compare error from B are also kept, because `resetSession` runs only when `!sameItemReload` (`:162-164`).

Vitest covers 42→99 and a failed 99 (`WebUI/src/test/ts/contentExplorer/RevisionsPanel.test.tsx:364` and `:431`) but never returns to 42. Add a behavioral test: restore 42, switch to 99, switch back to 42. Compare ids must be 42's, and a failed load of 42 must be `data-testid-state="error"`, not Loading.

Fix: a same-item reload must apply only to the effect run caused by that restore, not to a later visit. When `itemId` changes, drop the reload association so a later visit to the restored item loads fresh.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
