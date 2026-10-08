# Erlang review — issue 5362

Persona: erlang 0.1.1
Persona source: /home/nate/.local/share/mkd/agents/erlang
Author-reviewer: same night-issue-prs session (disclosed). The LLM stage did not run; machine findings are the gate. This session does not approve or merge the PR.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: working tree on `fix/issue-5362-scheme-parameter-sequence` (feature files, before this report commit)
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2207 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterSequenceChange` cognitive=21 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: non-blocking. The method is the sequence-one validator. It mirrors `prepareSchemeParameterTypeChange` and stays under the bug line (cognitive 30). Behavioral tests cover a null sequence, a blank stored type, a blank stored value, a blank name, an overlong name, an empty list, two parameters, an unknown name (409), role 403, a duplicate scheme name (409), and sequence combined with add, remove, value, or type.

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2268 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: non-blocking. The method body is unchanged. The diff only extends the javadoc so a sequence-one update is documented as not using replace-all. Sequence-one calls `IPSLocationScheme.addParameter` and does not enter this method. The same cognitive 16 row was already present on the type slice.

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open
- Disposition: not a gate failure. Machine findings stand. Ollama CUDA allocation failed; the review was not failed for that warn.

## Intent notes

- Change class: one flag on the existing publishing-design scheme PUT (`updateParameterSequence`), sitemanage validation before any write, Publishing Design Contexts sequence form, Vitest, sitemanage behavioral tests, product-docs `product-docs/8.2/admin/publishing.md`, Playwright `designLocationSchemeParameterSequence.spec.js`.
- No edit to `api/paths.ts`, `DeveloperShell.tsx`, `sitemanage-beans.xml`, or WebUI / perc-qa-automation `package.json`.
- No `final` / `sealed` / signature change. No `extends PSLocationSchemeSummary` and no anonymous `new PSLocationSchemeSummary()`.
- No new filesystem path construction.
- Tests present for the success path (stored type and value kept, incoming type and value ignored, other parameter sequence and scheme identity kept) and for a null sequence, a blank stored type, a blank stored value, a blank name, a name too long, an empty list, two parameters, a missing name 409, role 403, a duplicate scheme name 409, and sequence combined with add, remove, value, or type.
- UI tests cover the save body, a blank sequence with no PUT, a non-integer sequence with no PUT, cancel, a name that is not on the loaded scheme, and HTTP 400/403/409 leaving the previous sequence.

## CLI

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --diff tmp/issue-5362-ab.diff \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

The unified diff rewrote `c/` and `w/` headers to `a/` and `b/` so the four new files were in scope. Files analyzed: 11. Exit 0. Blocking bugs: 0.
