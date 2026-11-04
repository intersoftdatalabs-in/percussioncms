<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.
-->

# Erlang review — PR #5566 EditorHost whole-number NUL (#5557)

Status: independent overnight review (this agent did not author the PR)

Persona: erlang 0.1.1

Persona source: ~/.local/share/mkd/agents/erlang

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS** (in-diff bugs: 0; preexisting `handleSave` complexity is not a functional defect; float NUL is an explicit later slice)

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1401 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=146 (max 15), cyclomatic=180 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Preexisting. This slice adds one peer gate (`collectNumberNulFieldErrors`) before `collectInvalidNumericFieldErrors`. Not a merge block.

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/numericField.ts:151 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `numericFieldProblem` cognitive=23 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent / change-class closure

Whole-number U+0000 refused before PUT; required-blank and ordinary integer unchanged; float NUL out of scope. Companions: `numericField.ts`, EditorHost, Vitest, Playwright, product-docs number control row. No REST adaptor change.

Recommendation: **approve**.
