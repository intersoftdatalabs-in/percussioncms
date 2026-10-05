<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Pre-push local code review — PR #5236

Independent Erlang re-review at code HEAD `07bdd26bad` (fix: drop the revisions reload when leaving the item). The machine report below is the full `mkd-code-review analyze --format markdown` stdout from this pass (`--pack percussion --gate advisory --git-base origin/main --models models.ollama-dev-coder.toml`). Ollama `dev-coder` returned CUDA out-of-memory; machine findings were kept. Preexisting rows do not block. In-diff machine bugs: 0. The prior return-to-restored-item bug is fixed. Recommendation: approve.

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

**approve.** May merge. In-diff machine bugs: 0. The six `paths.hardcoded_sep` rows are preexisting in `PSItemService` and are not in this diff. The Ollama CUDA out-of-memory (`llm.error`) is not a product defect; the machine pass completed.

The prior bug is fixed in `07bdd26bad`. `sameItemReload` is true only when `reloadItemIdRef.current === itemId` and `reloadToken > 0` (`WebUI/src/main/ts/contentExplorer/RevisionsPanel.tsx:138-139`). A successful restore sets the ref to that item (`:220-221`). The load effect clears the ref when `itemId` is different (`:140-142`), so leaving the item drops the association. A later visit is not a same-item reload: compare from/to are assigned from that item's revision ids (`:178-185`), and a failed load sets `kind: "error"` (`:198-201`) instead of staying on Loading.

The shell still mounts one panel across content items (`ContentExplorerShell.tsx:4318-4338`) with no `key`. Folder and empty selection unmount it. `loadSummary` in production is the stable `defaultLoad`, so the effect does not re-fire on parent render.

Behavioral coverage: `RevisionsPanel.test.tsx` restores 42, switches to 99, and switches back to 42. Compare uses 42's ids (`"1"` / `"2"`, `compare` called with `("42", 1, 2)` and not `("42", 10, 12)`). A failed load of 42 on that return is `data-testid-state="error"`. Local vitest: `RevisionsPanel.test.tsx` 15 passed. Playwright `explorer-restore-revision.spec.js` covers the same return on success and on HTTP 500. No new filesystem path joins. No agent rule files in the diff.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
