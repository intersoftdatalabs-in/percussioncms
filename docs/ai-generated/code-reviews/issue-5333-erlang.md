# Erlang review — issue 5333

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Models: /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
(ollama-dev-coder returned HTTP 500 CUDA OOM; machine findings kept)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Suggestions do not block. `updateScheme` cognitive complexity was already above the arborist cap from prior location-scheme slices; this change adds one guarded remove branch and does not introduce a bug, a missing behavioral test, or a non-portable path. `applySchemeParameters` was not logically changed beyond a comment. No new filesystem path joins. Companions present: `locationSchemeRemoveParameter.ts` plus Vitest, `ContextsPanel`, `PSPublishingDesignRestServiceTest`, Jackson bind test, Playwright `designLocationSchemeRemoveParameter.spec.js`, and `product-docs/8.2/admin/publishing.md`. No agent rule files.

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1391 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=25 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Gate: non-blocking suggestion

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1973 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Gate: non-blocking suggestion (comment-only touch on this method)

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open
- Gate: non-blocking; machine findings retained

## Pre-push local code review

Gate: PASS (0 blocking bugs). May commit/push: yes.
