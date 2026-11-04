# Erlang review — issue #5557 EditorHost number field NUL

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1401 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=146 (max 15), cyclomatic=180 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/numericField.ts:151 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `numericFieldProblem` cognitive=23 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent notes

- Change class: EditorHost whole-number field NUL gate (peer of date NUL #5527 / datetime NUL #5526).
- `collectNumberNulFieldErrors` flags only `kind === "number"` with `numericInteger !== false`. Float/decimal NUL stays on slice 101. Required-blank number stays first. Invalid/range stay in `numericFieldProblem`.
- Companions present: Vitest (`numericField.test.ts`, `EditorHost.test.tsx`), Playwright (`editor-host-number-nul.spec.js`), `product-docs/8.2/admin/content-explorer.md`.
- No filesystem path joins. No agent-rule files. No public Java API shape change.
- In-diff complexity on `numericFieldProblem` is preexisting body (file offset only); not a blocking bug.
