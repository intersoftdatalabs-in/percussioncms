## Summary

Machine analysis found **4** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 3 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1404 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=36 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1982 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterValueChange` cognitive=19 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2041 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Interpreter (night-issue-prs)

Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml` (no PATHS). Exit 0. 11 files in scope.

The advisory machine bug is `complexity.cognitive` on `updateScheme`, which already sat above the pack threshold before this slice. The new value path is `prepareSchemeParameterValueChange`. Behavioral tests cover success (stored type, sequence, and id stay; other parameters stay), blank and null value, blank and too-long name, empty and two-parameter bodies, a name that is not on the scheme, HTTP 403, duplicate scheme name, and combination with add or remove. No in-diff logic bug, missing behavioral test, or non-portable path. The LLM stage failed on CUDA out of memory (`llm.error`); machine findings were kept. Night gate does not block on the complexity metric. May open the PR.
