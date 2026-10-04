<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5167

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 94613349ade2044ad11b38937ecb266253bcd4d2
- Branch: fix/issue-5161-editor-unapprove
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Agent note

Independent Erlang review of PR #5167 (not the author). Machine gate: 0 in-diff bugs. Recommendation: approve. May merge: yes.

EditorHost unapprove of the open page or asset reuses `POST /services/sitemanage/publish/incremental/explorer/{contentId}/unapprove`. Approved state clears and the unapproved notice appears only after the call returns true. Cancel does not POST. View mode does not render the control (`showIncrementalApprove` is edit-only). A template, folder, or other non-page/non-asset returns false and does not POST. HTTP 400/403/409 stay on the dialog and do not clear the approved mark. Vitest, Playwright, and `product-docs/8.2/admin/content-explorer.md` plus `publishing.md` match that behavior. No second queue API. No rule-file diff. No new filesystem path joins.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
