# Erlang review — issue #5527 editor date NUL

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1392 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=145 (max 15), cyclomatic=179 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent notes

- Change class: EditorHost calendar date NUL gate (peer of datetime NUL #5526 / PR #5543).
- `collectDateNulFieldErrors` flags only `kind === "date"`; datetime NUL stays on `collectDatetimeNulFieldErrors`. Required-blank date stays first.
- Companions present: Vitest (`dateField.test.ts`, `DateFieldWidget.test.tsx`, `EditorHost.test.tsx`), Playwright (`editor-host-date-nul.spec.js`), `product-docs/8.2/admin/content-explorer.md`.
- No filesystem path joins. No agent-rule files. No public Java API shape change.
