# Erlang review — issue #5558 (EditorHost refuse a decimal field NUL)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Independent of the implementer: the in-diff work adds `collectDecimalNulFieldErrors` for `numericInteger === false` rows, runs it after the whole-number NUL gate and before `collectInvalidNumericFieldErrors`, and marks float widgets with `data-editor-has-nul` when the draft contains U+0000. Required-blank still uses the required-field gate. Ordinary decimals still save. HTTP 400/403/409 stay failures. Integer NUL stays on `collectNumberNulFieldErrors`. Companions: Vitest (`numericField.test.ts`, `EditorHost.test.tsx`), Playwright (`tests/editor-host-decimal-nul.spec.js`), and the number-control row in `product-docs/8.2/admin/content-explorer.md`. No filesystem path joins. No rule files.

The in-diff `numericFieldProblem` complexity row is preexisting body (line numbers shifted by the new collector). `handleSave` complexity is preexisting.

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1398 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=147 (max 15), cyclomatic=181 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Gate: not blocking (preexisting; not introduced by this slice)

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/numericField.ts:175 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `numericFieldProblem` cognitive=23 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Gate: not blocking (preexisting function body; line shift only)

## Cross-platform

No new filesystem path joins. Playwright and Vitest inject U+0000 via code points / React `onChange`, not OS paths.
