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

# Erlang overnight review — PR #5578

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: PR head `d778c172e59ab26903a29e54ce83e71fb172405d` (`fix/issue-5559-editor-yesno-nul`).
CLI: mkd-code-review 0.1.18. Reviewer did not author this PR.

Overnight interpretation: **PASS**. In-diff bugs: 0. Preexisting `handleSave` / `classifyEditorControl` complexity does not BLOCK. Yes-no (`sys_SingleCheckBox`) NUL is refused before PUT; ordinary yes/no still saves. Companions: Vitest (`yesnoField.test.ts`, `EditorHost.test.tsx`, `controlKinds.test.ts`), Playwright (`tests/editor-host-yesno-nul.spec.js`), `product-docs/8.2/admin/content-explorer.md`. No filesystem path joins. No agent rule files.

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1440 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=158 (max 15), cyclomatic=201 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/editor/controlKinds.ts:70 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `classifyEditorControl` cognitive=25 (max 15), cyclomatic=40 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
