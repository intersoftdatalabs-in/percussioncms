# Erlang review — issue #5525 (EditorHost keyword field NUL)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main --models \
  /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1388 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=143 (max 15), cyclomatic=177 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Gate notes (this slice)

- In-diff bugs: none. The `handleSave` complexity row is preexisting; this slice only adds a keyword NUL collector call next to the existing link/table NUL gates.
- Behavioral tests: Vitest for `keywordContainsNul` / `collectKeywordNulFieldErrors`, EditorHost refuse/cancel/catalog/required-empty/HTTP 400-403-409, Playwright `tests/editor-host-keyword-nul.spec.js`.
- Cross-platform paths: no new filesystem path construction; Playwright uses URL helpers with `/`.

Gate: **PASS** (advisory; preexisting complexity does not BLOCK).
