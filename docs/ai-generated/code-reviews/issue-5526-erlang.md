# Erlang review — issue #5526 editor datetime NUL

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: worktree `fix/issue-5526-editor-datetime-nul`.
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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1389 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=144 (max 15), cyclomatic=178 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This slice adds `collectDatetimeNulFieldErrors` next to the existing invalid-date / keyword NUL gates and returns before PUT. Complexity is not introduced as a new defect.

## Gate notes

- In-diff bugs: none.
- Behavioral tests: Vitest `datetimeContainsNul` / `collectDatetimeNulFieldErrors`; DateFieldWidget NUL pass-through and `data-editor-has-nul`; EditorHost refuse-before-PUT, cancel, valid datetime save, required-blank unchanged, optional clear, HTTP 400/403/409 not success; Playwright `tests/editor-host-datetime-nul.spec.js` (1 passed per Work).
- Gate order: datetime NUL runs after required-blank and before `collectInvalidDateFieldErrors`, so a NUL is not shown as `Enter a valid date.` Date-only NUL stays out of this collector (slice 99).
- Change-class companions: WebUI Vitest, Playwright, `product-docs/8.2/admin/content-explorer.md` datetime cell (not an index).
- Non-portable filesystem paths: none. Playwright uses URL helpers.

Gate: **PASS**. Recommendation: approve.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
