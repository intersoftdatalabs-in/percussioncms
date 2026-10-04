<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5191

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5191
- Base: origin/main
- Head: b7aa1adacaa9ff11aa9fd67f3243d1c03cc7dcf0
- Files analyzed: 11
- Reviewer: independent Erlang (did not author the PR)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/DetailList.tsx:383
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: Consider adding a test to verify that the folder display format is shown correctly when a folder is refreshed.
- Suggestion: Add a test case in DetailList.test.tsx that simulates a folder refresh and checks if the folder display format is displayed.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/contentExplorer/setFolderDisplayFormat.test.ts:269
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: Consider adding tests for the new functions introduced in setFolderDisplayFormat.ts, such as planSetFolderDisplayFormatMulti, loadSetFolderDisplayFormatMultiCatalog, saveSetFolderDisplayFormatOnSelection, and describeSetFolderDisplayFormatMultiSave.
- Suggestion: Create new test cases in setFolderDisplayFormat.test.ts that cover various scenarios for these new functions.
- Status: open

## Erlang notes

The two LLM rows are suggestions, not bugs, and they are already covered. `DetailList.test.tsx` asserts the format name is shown only when that folder was refreshed. `setFolderDisplayFormat.test.ts` (`set display format on multi-selected folders (#5180)`) exercises plan, catalog load, per-folder save, refresh mismatch, HTTP 400/403/409 as not-full-success, and the status text. The format name is painted from `onFolderSaved` only after the properties reload matches the id and catalog name. Pages and assets are named and not posted. Cancel does not save. No blocking finding.
