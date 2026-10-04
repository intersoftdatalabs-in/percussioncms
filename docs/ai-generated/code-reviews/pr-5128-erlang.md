<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5128

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 9f2dd7324c2cd5e0a8b5e6cce43183feaeaf74f3
- Branch: fix/issue-5109-copy-location-scheme
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1003 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1003)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1006 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1006)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1058 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1058)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Agent note

Independent Erlang review of PR #5128 (not the author). Machine gate: 0 in-diff bugs. The three `paths.hardcoded_sep` rows are preexisting and outside the new query. Recommendation: approve. May merge: yes.

Copy uses the existing create-scheme API with a `locationScheme` root, `copy: true`, and `schemeParameter` values. A blank or 51-character name does not POST. Cancel does not create a row. HTTP 400/403/409 stay on the form. `UIX_RXLOCSCHEME` is left intact: when the source context/template/content-type triple is taken, the copy is stored with a different template id and the source row is not rewritten. `findSchemesByContextId` is a class override so the Spring transaction starts. Vitest, Java tests, Playwright, and product-docs are present. No rule-file diff.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
