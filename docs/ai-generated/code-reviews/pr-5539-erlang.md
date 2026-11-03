# Erlang review — PR #5539 (issue #5525 editor keyword NUL)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: PR head `1ccdd115a567d05bd47c47a2513c0c4f552080ab` (`fix/issue-5525-editor-keyword-nul`).
CLI: mkd-code-review 0.1.18. Reviewer did not author this PR.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- CLI: mkd-code-review 0.1.18
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- May merge: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1388 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=143 (max 15), cyclomatic=177 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This slice adds `collectKeywordNulFieldErrors` next to the existing link/table NUL gates and returns before PUT. Complexity is not introduced as a new defect.

## Gate notes

- In-diff bugs: none.
- Behavioral tests: Vitest `keywordContainsNul` / `collectKeywordNulFieldErrors`; EditorHost refuse-before-PUT, cancel, catalog save, required-empty unchanged, HTTP 400/403/409 not success; Playwright `tests/editor-host-keyword-nul.spec.js` (1 passed per Work).
- Gate order: keyword NUL runs before catalog-membership. Required-empty stays on the required-field gate.
- Change-class companions: WebUI Vitest, Playwright, `product-docs/8.2/admin/content-explorer.md` keyword cell (not an index).
- Non-portable filesystem paths: none. Playwright uses URL helpers.

Gate: **PASS**. Recommendation: approve.
