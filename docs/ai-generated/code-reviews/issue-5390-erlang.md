# Erlang review — issue 5390

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
Head: 69f04b4494725b2d577eb794bee6f469a9396b39

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

In-diff bugs, missing behavioral tests, and non-portable paths: none.
The preexisting `handleSave` cognitive-complexity row is not introduced by
the link NUL guard and does not block. Machine findings kept.

Independent read of the diff (not the author). EditorHost refuses a link
field that contains NUL before the fields PUT and before the existing
invalid-link shape gate. A content id, GUID, or folder path still saves.
Reload keeps the previous link. Cancel on close does not write. HTTP 400
is not success. `linkFieldProblem` is unchanged, so a `javascript:` target
still uses the shape message. Companions present: linkField and EditorHost
Vitest, surface Playwright, and product-docs/8.2/admin/content-explorer.md.
No new filesystem path joins. No agent rule files.

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1377 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=141 (max 15), cyclomatic=175 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: preexisting. Not introduced by the link NUL check. Does not block.
