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

# Erlang review — PR #5427

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- Models: /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5427
- Base: origin/main
- Head reviewed: db5ee1a9be736a8fb27a53894971fe32a9ab3712
- Files analyzed: 7
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass (not the author). Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:627 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=46 (max 15), cyclomatic=43 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/assembly-longtext-nul-field.spec.js:76 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installAssemblyRoutes` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent reading

`nulLongTextFieldNames` names a `longtext` field whose current value contains U+0000. Line breaks are not a NUL. Single-line text stays on `nulSingleLineTextFieldNames`. `handleSaveFields` returns before the item PUT and uses **That long text contains a character that cannot be saved.** The host test asserts no `saveFields` call, the long-text message (not Fields saved), and a reload that still shows the previous text including its line break. Cancel restores that text and does not write. An ordinary multiline value still saves with the line break. HTTP 400 shows could-not-save, not Fields saved, and restores the previous line breaks. Product docs describe the same gate and say it does not change the single-line NUL gate and does not apply to HTML. No filesystem path joins. No agent-rule diff.

`handleSaveFields` complexity is preexisting and is not an in-diff bug. The Playwright route helper complexity is a suggestion. Neither blocks.

Recommendation: **approve**. May commit/push: yes. In-diff blocking bugs: 0.
