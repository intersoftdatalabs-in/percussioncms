/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# Erlang review PR #5376 (issue #5359)

Status: cli-ok (mkd-code-review 0.1.18)
Persona: erlang 0.1.1
Persona source: ~/.local/share/mkd/agents/erlang

Independent of the author. Gate counts in-diff bugs only. Preexisting complexity rows do not block. The Ollama CUDA out-of-memory warning is not a product defect. Manual read of the diff: a blank or whitespace required whole number is refused before item field PUT, reload keeps the previous number, a non-blank whole number still saves, Cancel does not write, and HTTP 400/403/409 do not claim success. Optional clear, decimals, and range stay on the existing number check. Behavioral Vitest and Playwright cover those paths. No new filesystem path construction. No rule-file edits.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:618 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=31 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:717 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=32 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
