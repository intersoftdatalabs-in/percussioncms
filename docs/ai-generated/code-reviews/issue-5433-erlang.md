# Erlang review — issue 5433

Implementer session (author and reviewer are the same session; host Erlang reviews the PR). CLI `mkd-code-review` 0.1.18, pack `percussion`, `--format markdown`, `--gate advisory`, `--git-base origin/main`, models `models.ollama-dev-coder.toml`. Persona erlang 0.1.1. Exit 0. No `llm.error`.

## CLI report

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1089 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=85 (max 15), cyclomatic=84 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

- In-diff bugs: 0. `handleSave` complexity is preexisting and outside the description-only save.
- Behavioral tests cover the write helper, panel success/cancel/unchanged/blank/400/403/409, and the adaptor blank clear that leaves label and enabled alone. Playwright covers the H2 surface.
- No new filesystem path construction.
- Gate: PASS. May commit/push: yes.
