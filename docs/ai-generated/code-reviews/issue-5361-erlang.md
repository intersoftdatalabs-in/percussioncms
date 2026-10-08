# Erlang review — issue 5361

Persona: erlang 0.1.1
Persona source: /home/nate/.local/share/mkd/agents/erlang
Author-reviewer: same night-issue-prs session (disclosed). The LLM stage did not run; machine findings are the gate.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: working tree on `fix/issue-5361-location-scheme-parameter-type` (staged feature files, before this report commit)
- Files: 11 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate mode: advisory (`mkd-code-review analyze --gate advisory`)
- Block rule for this run: in-diff bugs, missing behavioral tests, non-portable paths. Suggestions do not block. Cognitive severity becomes a bug only above twice the pack threshold (30). Both complexity rows are under that line.

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2120 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterTypeChange` cognitive=20 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: non-blocking. The method is the type-one validator. It mirrors `prepareSchemeParameterValueChange` and stays under the bug line (cognitive 30). Behavioral tests cover blank type, overlong type, blank stored value, unknown name (409), and combined flags.

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2182 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: non-blocking. The method body is unchanged. The diff only extends the javadoc so a type-one update is documented as not using replace-all. Type-one calls `IPSLocationScheme.addParameter` and does not enter this method.

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open
- Disposition: not a gate failure. Machine findings stand. Ollama CUDA allocation failed; the review was not failed for that warn.

## Intent notes

- Change class: one flag on the existing publishing-design scheme PUT (`updateParameterType`), sitemanage validation before any write, Publishing Design Contexts type form, Vitest, sitemanage behavioral tests, product-docs `product-docs/8.2/admin/publishing.md`, Playwright `designLocationSchemeParameterType.spec.js`.
- No edit to `api/paths.ts`, `DeveloperShell.tsx`, `sitemanage-beans.xml`, or WebUI / perc-qa-automation `package.json`.
- No `final` / `sealed` / signature change. No `extends PSLocationSchemeSummary` and no anonymous `new PSLocationSchemeSummary()`.
- No new filesystem path construction.
- Tests present for the success path (stored value and sequence kept, incoming value and sequence ignored, other parameter and scheme identity kept) and for blank type, null type, type too long, blank stored value, blank name, name too long, empty list, two parameters, missing name 409, role 403, duplicate scheme name 409, and type combined with add, remove, or value.
- UI tests cover the save body, blank type with no PUT, cancel, and HTTP 400/403/409 leaving the previous type.

## CLI

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Second run after `git add` of the four new files. Files analyzed: 11. Exit 0.
