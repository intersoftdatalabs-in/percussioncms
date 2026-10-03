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

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:258 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `copyEdition` cognitive=27 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Intent review (Erlang)

Independent read of the PublishingShell edition-copy slice (not the author). Recommendation: approve. No blocking bug.

- `PSEdition.setName` writes `displaytitle`, and `getName` returns that title. Copy now calls `setName` last and no longer calls `setDisplayTitle` with the source title, so the requested name is the visible name. `copyEdition_keepsRequestedNameWhenSourceHasDisplayTitle` asserts `setName("CopiedName")` and never `setDisplayTitle("SourceEd")`.
- `requireDesignWrite` runs before the copy. `requireUniqueEditionName` runs before `createEdition`, so HTTP 409 does not persist a row. Java tests cover 403 (no `createEdition`) and 409 (no `createEdition` or `saveEdition`).
- The client posts `{ copyEditionRequest: ... }` and unwraps an `edition` root. On success, Design closes the editor. A different target site updates `siteId`; `reloadEditions` is recreated with that id and the editions effect reloads the target list. Vitest covers same-site reload and other-site follow.
- HTTP 400 and 403 stay on the editor (`EditionEditor.copy.test.tsx` and the H2 spec). HTTP 409 uses the same catch (`mapEditionCopyError`, no `onCopied`). The 409 message branch is not asserted in the editor Vitest; the server 409 is. Non-blocking.
- `copyEdition` cognitive complexity is marked preexisting by the CLI (in-diff bugs: 0). Not a gate. Ollama `dev-coder` HTTP 500 (CUDA out of memory) is a suggestion, not a defect in the diff.
- Companions: sitemanage unit tests, Vitest API/editor/Design, Playwright `designEditionCopy.spec.js`, `product-docs/8.2/admin/publishing.md`. New sources use the Intersoft 2026 header. No agent-rule diff. No filesystem path joins.
